/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import { Rubric } from './rubric.ts';
import { FeedbackManager } from '../../../assignmentFeedback/FeedbackManager.js';
import { attachmentService as AttachmentService } from '../../../services/pure/attachmentService.ts';
import { nativeQ as $q } from '../../../utilities/nativeQ.ts';

/**
 * Pure-TS port of the Angular `lo.assignmentGrade.CompositeGrade` module (models/CompositeGrade.js):
 * a wrapper for an assignment grade that might or might not have a rubric.
 *
 * The grade-card UI re-renders via the GraderProvider's 150ms poll, which reads MUTABLE state on
 * these objects (`outgoing.pointsAwarded / rubric / feedback / feedbackManager / releaseStatus`,
 * `isSubmitting`, `isBlankGrade`) and the `isDirty()` / `isComplete()` / `isReleased()` /
 * `resetGrade()` surface. This is a lift-and-shift: the `outgoing` mutation chains and the
 * `rubricChanged` onChange wiring are preserved verbatim. Do NOT make anything immutable.
 *
 * `CompositeGrade.js` keeps thin `.factory` adapters over these classes with the SAME service names
 * (`CompositeGrade`, `ViewCompositeGrade`) so the child grade models (still `.js`, which `extends`
 * `CompositeGrade`) and the graders keep resolving.
 */
export const CompositeGrade = function (this: any, config: any) {
  this.pointsPossible = config.pointsPossible;
  this.scaledPointsPossible = config.scaledPointsPossible;
  this.rubric = config.rubric;
  this.attachmentUrl = config.attachmentUrl;
  this.isBlankGrade = config.isBlankGrade;
  this.displayStyle = config.displayStyle;

  this.initial = {
    pointsAwarded: config.pointsAwarded !== void 0 ? config.pointsAwarded : null,
    rubricResponse: config.rubricResponse,
    feedback: config.feedback || '',
    attachments: config.attachments || [],
    releaseStatus: config.releaseStatus,
  };

  this.resetGrade();
} as any;

CompositeGrade.prototype.setReleaseStatus = function (releaseStatus: any) {
  this.outgoing.releaseStatus = releaseStatus;
};

CompositeGrade.prototype.resetGrade = function () {
  //this is intentionally not blocking
  if (this.outgoing) {
    this.outgoing.feedbackManager.removeStagedFiles();
  }

  this.outgoing = {
    pointsAwarded: this.initial.pointsAwarded,
    rubric:
      this.rubric &&
      new (Rubric as any)(this.rubric, this.initial.rubricResponse, this.rubricChanged.bind(this)),
    feedback: this.initial.feedback || '',
    feedbackManager: new (FeedbackManager as any)(this.initial.attachments),
    releaseStatus: this.initial.releaseStatus,
  };
};

CompositeGrade.prototype.rubricChanged = function () {
  var score = this.outgoing.rubric.getScore();
  if (score.pointsPossible === 0 || this.pointsPossible === 0) {
    this.outgoing.pointsAwarded = score.pointsAwarded;
  } else {
    this.outgoing.pointsAwarded =
      (score.pointsAwarded / score.pointsPossible) * this.pointsPossible;
  }
};

CompositeGrade.prototype.isComplete = function () {
  if (this.outgoing.rubric) {
    return this.outgoing.rubric.isValid();
  } else {
    return !isNaN(parseFloat(this.outgoing.pointsAwarded));
  }
};

CompositeGrade.prototype.isDirty = function () {
  return (
    this.initial.pointsAwarded !== this.outgoing.pointsAwarded ||
    this.initial.feedback !== this.outgoing.feedback ||
    this.outgoing.feedbackManager.hasStagedOrOngoing() ||
    (this.rubric && this.outgoing.rubric.isDirty()) ||
    this.initial.releaseStatus !== this.outgoing.releaseStatus
  );
};

CompositeGrade.prototype.isReleased = function () {
  return this.outgoing.releaseStatus;
};

CompositeGrade.prototype.beforeSaveGrade = function () {
  return this.outgoing.feedbackManager
    .confirmResetByModal()
    .then(() => this.outgoing.feedbackManager.commitRemovingFiles());
};

CompositeGrade.prototype.saveGrade = function () {
  console.error('You must implement saveGrade');
  return $q.reject('You must implement saveGrade');
};

CompositeGrade.prototype.syncAttachments = function (savedGrade: any) {
  return AttachmentService.getAttachments(this.attachmentUrl).then(
    function (this: any, uploads: any) {
      this.initial.attachments = uploads;
      this.outgoing.feedbackManager = new (FeedbackManager as any)(uploads);
      return savedGrade;
    }.bind(this),
    function (this: any) {
      // server returns 404 if there are no attachments (which is totally valid)
      this.initial.attachments = [];
      this.outgoing.feedbackManager = new (FeedbackManager as any)([]);
      return savedGrade;
    }.bind(this)
  );
};

CompositeGrade.prototype.syncSavedGrade = function (savedGrade: any) {
  this.initial = {
    pointsAwarded: this.outgoing.pointsAwarded,
    rubricResponse: this.outgoing.rubric ? this.outgoing.rubric.getResponse() : {},
    feedback: this.outgoing.feedback || '',
    releaseStatus: this.outgoing.releaseStatus,
  };

  if (this.outgoing.rubric) {
    this.outgoing.rubric.setClean();
  }

  return this.syncAttachments(savedGrade);
};

CompositeGrade.prototype.afterSaveGrade = function (savedGrade: any) {
  this.isBlankGrade = false;
  return $q.when(savedGrade);
};

CompositeGrade.prototype.saveChanges = function (isReleasing: any) {
  this.isSubmitting = true;
  return this.beforeSaveGrade()
    .then(
      function (this: any) {
        return this.saveGrade(isReleasing);
      }.bind(this)
    )
    .then(
      function (this: any, savedGrade: any) {
        this.outgoing.releaseStatus = isReleasing;
        return this.syncSavedGrade(savedGrade);
      }.bind(this)
    )
    .then(this.afterSaveGrade.bind(this))
    .finally(() => (this.isSubmitting = false));
};

export const ViewCompositeGrade = function (this: any, config: any) {
  this.isBlankGrade = config.isBlankGrade;
  this.pointsAwarded = config.pointsAwarded;
  this.pointsPossible = config.pointsPossible;
  this.scaledPointsPossible = config.scaledPointsPossible;

  this.scorePercent = this.pointsAwarded / this.pointsPossible;

  this.feedback = config.feedback;
  this.attachmentUrl = config.attachmentUrl;
  this.feedbackManager = new (FeedbackManager as any)(config.attachments);

  if (config.rubric) {
    this.rubric = new (Rubric as any)(config.rubric, config.rubricResponse);
  }
} as any;
