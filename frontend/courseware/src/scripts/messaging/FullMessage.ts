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

import { extend, find, map } from 'lodash';

import { FeedbackManager } from '../assignmentFeedback/FeedbackManager.js';
import Course from '../bootstrap/course.ts';
import messagingService from '../services/messagingService.ts';
import { nativeQ as $q } from '../utilities/nativeQ.ts';

/**
 * The message-compose model, ported verbatim from the AngularJS `FullMessage`
 * `.factory` to a plain TS constructor function + prototype. The injected
 * `MessagingService`/`$q`/`FeedbackManager` are now direct imports of the pure
 * singleton / native-$q shim / FeedbackManager class.
 */
export const FullMessage: any = function (this: any) {
  this.questionId = '';
  this.feedbackManager = new (FeedbackManager as any)();
  this.recipients = [];
};

FullMessage.prototype.prepareRecipients = function () {
  if (this.selectingEntireClass) {
    return $q.when([
      {
        _type: 'context',
        context: Course.id,
      },
    ]);
  } else {
    return $q.when(
      map(this.recipients, function (user: any) {
        return {
          _type: 'user',
          user: user.id,
        };
      })
    );
  }
};

FullMessage.prototype.serialize = function (overwrite: any) {
  return extend(
    {
      subject: this.title,
      body: this.content,
      recipients: this.recipients,
      uploads: this.feedbackManager.getFilesInStaging(),
    },
    overwrite
  );
};

FullMessage.prototype.isValid = function () {
  return true;
};

FullMessage.prototype.isSelected = function (user: any) {
  return find(this.recipients, function (selected: any) {
    return selected.id === user.id;
  });
};

/**
 * Returns true if it has selections or if it was set to send to
 * the entire class (assumes there is at least one person in the class)
 */
FullMessage.prototype.hasRecipients = function () {
  return !!this.selectingEntireClass || this.recipients.length > 0;
};

FullMessage.prototype.setSelection = function (selection: any) {
  this.recipients = selection || [];
};

FullMessage.prototype.addSelection = function (user: any) {
  var index = this.recipients.indexOf(user);
  if (index === -1) {
    this.recipients.push(user);
  }
};

FullMessage.prototype.removeSelection = function (user: any) {
  var index = this.recipients.indexOf(user);
  if (index !== -1) {
    this.recipients.splice(index, 1);
  }
};

FullMessage.prototype.selectEntireClass = function (isSelected: any) {
  this.selectingEntireClass = isSelected;
};

FullMessage.prototype.send = function () {
  return this.prepareRecipients().then(
    function (this: any, recipients: any) {
      return messagingService.sendMessage(
        this.serialize({
          recipients: recipients,
        })
      );
    }.bind(this)
  );
};
