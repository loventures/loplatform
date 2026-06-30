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

import {
  each,
  filter,
  find,
  findIndex,
  first,
  isEmpty,
  isFunction,
  last,
  map,
  orderBy,
  reduce,
  assign,
} from 'lodash';

import { SubmissionAttemptGrade } from '../../../assignmentGrade/models/pure/submissionAttemptGrade.ts';

import { submissionGraderLoader as SubmissionGraderLoader } from './submissionGraderLoader.ts';

import { BaseGrader } from './baseGrader.ts';

import { Roles } from '../../../utilities/pure/roles.ts';

import { DRIVER_LEARNER } from '../../../utilities/assessmentSettings.js';

import {
  ATTEMPT_OPEN,
  ATTEMPT_SUBMITTED,
  ATTEMPT_FINALIZED,
} from '../../../utilities/attemptStates.js';

import { buildScorableAttemptState } from '../../../assignmentGrade/models/scoreUtils.ts';
import { nativeQ as $q } from '../../../utilities/nativeQ.ts';

/**
 * Pure-TS port of the Angular `lo.SubmissionGrader.SubmissionGrader` factory (graders/SubmissionGrader.js):
 * the instructor submission/observation grader — container for all users who have submitted anything.
 * Prototype-chains against the pure constructor-function `BaseGrader`.
 *
 * Lift-and-shift of the ES-class grader as a constructor-function + prototype so it extends the pure
 * `BaseGrader` the same way the pure grade models extend `CompositeGrade`. The GraderProvider's 150ms
 * poll reads mutable state on this object (`activeUser`, `activeAttempt`, `activeGrade`,
 * `inProgressAttempt`, `unpostedCount`, plus the grade model's `isDirty()`/`outgoing.*`); every mutable
 * field + the in-place mutation timing is preserved verbatim. Do NOT make anything immutable.
 *
 * The original class-field arrow predicates (`isInProgressAttempt` / `isGradableAttempt`) are assigned as
 * `this`-bound instance fields in the constructor so they keep capturing `this` and stay passable by
 * reference to `filter` (`loadUserOrderedAttempts` / `loadUserOrderedEffectiveAttempts` / `changeAttempt`).
 * The base `saveChanges` (the original `super.saveChanges`) is reached via `BaseGrader.prototype.saveChanges.call`.
 *
 * The pure children/loader/base are imported directly: pure `SubmissionAttemptGrade` model, the pure
 * `submissionGraderLoader`, the pure `BaseGrader`, pure `Roles`, and `nativeQ` as `$q`.
 * `SubmissionGrader.js` keeps a thin `.factory` adapter with the SAME service name so the still-Angular
 * grader module deps keep resolving.
 */
export const SubmissionGrader = function (this: any, assignment: any) {
  BaseGrader.call(this, assignment.contentId);

  this.rubric = assignment.rubric;
  this.isStudentDriven = DRIVER_LEARNER === assignment.settings.driver;
  this.instructions = assignment.instructions;
  this.activeAttempt = null;
  this.onChangeCallbacks = [];
  this.inProgressAttempt = null;

  this.isInProgressAttempt = (attempt: any) => {
    if (!attempt.valid) {
      return false;
    }
    return this.isStudentDriven
      ? attempt.state === ATTEMPT_SUBMITTED || !attempt.valid
      : //See CBLPROD-16231 for this oddity
        attempt.state === ATTEMPT_SUBMITTED || attempt.state === ATTEMPT_OPEN;
  };

  this.isGradableAttempt = (attempt: any) => {
    return (
      this.isInProgressAttempt(attempt) ||
      attempt.state === ATTEMPT_FINALIZED ||
      (attempt.state !== ATTEMPT_OPEN && !attempt.valid) ||
      (attempt.state === ATTEMPT_OPEN && !this.isStudentDriven && !attempt.valid)
    );
  };
} as any;

SubmissionGrader.prototype = Object.create(BaseGrader.prototype);
SubmissionGrader.prototype.constructor = SubmissionGrader;

SubmissionGrader.prototype.loadInfo = function () {
  const substate = this.getSubstate('assignmentInfo');

  const loader = () => SubmissionGraderLoader.loadInfo(this.assignmentId);

  return this.resolveData(substate, loader);
};

SubmissionGrader.prototype.loadUsers = function (reload: any) {
  const substate = this.getSubstate('users');

  const loader = () => SubmissionGraderLoader.loadUsers(this.assignmentId);

  return this.resolveData(substate, loader, reload);
};

SubmissionGrader.prototype.loadAttemptsForUser = function (reload: any) {
  const userId = this.activeUser.id;

  const substate = this.getSubstate(`attempts-${userId}`);

  const loader = () => SubmissionGraderLoader.loadAttemptsForUser(this.assignmentId, userId);

  return this.resolveData(substate, loader, reload);
};

SubmissionGrader.prototype.getFormattedAttempts = function () {
  return $q
    .all({
      userInfos: this.loadUsers(),
      attempts: this.loadAttemptsForUser(),
    })
    .then(({ userInfos, attempts }: any) => {
      const userId = this.activeUser.id;
      const userInfo = find(userInfos, { id: userId });
      return map(attempts, (attempt: any) => SubmissionGraderLoader.formatAttempt(attempt, userInfo));
    });
};

SubmissionGrader.prototype.userAttemptsUpdated = function (updatedAttempt: any, userId: any) {
  const substate = this.getSubstate(`attempts-${userId}`);
  assign(substate.data[updatedAttempt.id], updatedAttempt);
  if (this.activeAttempt && updatedAttempt.id === this.activeAttempt.id) {
    this.activeAttempt = {
      ...this.activeAttempt,
      ...updatedAttempt,
    };
  }
  this.canUserEditGrade =
    this.activeAttempt && this.activeAttempt.valid && this.isUserInstructor;
  return this.calculateUnpostedCount(true);
};

SubmissionGrader.prototype.loadGradableUsers = function () {
  return this.loadUsers().then((users: any) => {
    let gradeableUsers = orderBy(
      users,
      [(u: any) => u.gradeableAttempts.length, 'fullName'],
      ['desc', 'asc']
    );
    if (this.isStudentDriven) {
      gradeableUsers = filter(users, 'hasViewableAttempts');
    }

    return gradeableUsers;
  });
};

SubmissionGrader.prototype.loadUserOrderedAttempts = function () {
  return this.getFormattedAttempts().then((attempts: any) =>
    orderBy(filter(attempts, this.isGradableAttempt), 'createDate')
  );
};

SubmissionGrader.prototype.loadUserOrderedEffectiveAttempts = function (
  this: any,
  userId = this.activeUser.id
) {
  return this.loadUserOrderedAttempts(userId).then((attempts: any) =>
    filter(attempts, this.isInProgressAttempt)
  );
};

SubmissionGrader.prototype.changeUser = function (userId: any, attemptId: any) {
  this.activeUser = null;
  this.activeAttempt = null;
  this.activeGrade = null;
  this.inProgressAttempt = null;

  return this.loadGradableUsers().then((users: any) => {
    if (!users.length) {
      return $q.reject();
    }

    if (this.isStudentDriven) {
      this.activeUser =
        find(users, { id: +userId }) ||
        find(users, (u: any) => u.gradeableAttempts.length > 0) ||
        first(users);
    } else {
      this.activeUser =
        find(users, { id: +userId }) ||
        find(users, (u: any) => u.gradeableAttempts.length > 0) ||
        find(users, (u: any) => u.attemptCount === 0) ||
        first(users);
    }

    attemptId = attemptId || this.activeUser.gradeableAttempts[0];

    this.changeAttempt(attemptId);
  });
};

SubmissionGrader.prototype.changeAttempt = function (attemptId: any) {
  this.activeAttempt = null;
  this.activeGrade = null;

  return this.loadUserOrderedAttempts().then((attempts: any) => {
    if (!this.isStudentDriven && !this.inProgressAttempt) {
      this.inProgressAttempt = find(attempts, this.isInProgressAttempt);
    }

    this.detailedGradeExists = !isEmpty(attempts);
    this.activeAttempt =
      find(attempts, { id: attemptId }) || this.inProgressAttempt || last(attempts);

    this.canUserEditGrade =
      this.activeAttempt && this.activeAttempt.valid && this.isUserInstructor;
    this.calcItemsToGrade();

    if (this.activeAttempt) {
      this.loadInfo().then((info: any) => {
        this.activeGrade = new (SubmissionAttemptGrade as any)(
          this.activeAttempt,
          this.rubric,
          info.gradebookPointsPossible
        );
      });
    }
  });
};

SubmissionGrader.prototype.startAttempt = function () {
  return SubmissionGraderLoader.startAttempt(this.assignmentId, this.activeUser.id).then(
    (newAttempt: any) => {
      const substate = this.getSubstate(`attempts-${this.activeUser.id}`);
      substate.data[newAttempt.id] = newAttempt;
      this.userAttemptsUpdated(newAttempt, this.activeUser.id);
      this.calculateUnpostedCount(true);
      this.inProgressAttempt = newAttempt;
      return newAttempt;
    }
  );
};

SubmissionGrader.curryNext = function (item: any, offset: any) {
  if (!item) {
    return () => null;
  }

  return (list: any) => {
    const index = findIndex(list, { id: item.id });
    return list[index + offset];
  };
};

SubmissionGrader.prototype.nextUser = function (dir: any) {
  const formatUser = (user: any) => ({
    key: 'STUDENT',
    item: user,
    changeToThis: () => this.changeUser(user.id),
  });

  return this.loadGradableUsers()
    .then((users: any) => {
      const index = findIndex(users, { id: this.activeUser.id });
      return users[index + dir];
    })
    .then((user: any) => user && formatUser(user));
};

SubmissionGrader.prototype.nextAttempt = function (dir: any) {
  const formatAttempt = (attempt: any) => ({
    key: 'SUBMISSION',
    item: attempt,
    changeToThis: () => this.changeAttempt(attempt.id),
  });

  return this.loadUserOrderedEffectiveAttempts()
    .then(SubmissionGrader.curryNext(this.activeAttempt, dir))
    .then((attempt: any) => attempt && formatAttempt(attempt));
};

SubmissionGrader.prototype.findNext = function (dir: any) {
  return this.nextAttempt(dir).then((found: any) => found || this.nextUser(dir));
};

SubmissionGrader.prototype.canInvalidateAttempt = function () {
  if (!Roles.hasRole('EditCourseGradeRight')) {
    return false;
  }

  return this.activeAttempt.valid;
};

SubmissionGrader.prototype.invalidateAttempt = function () {
  if (!this.activeAttempt.valid) {
    return $q.when();
  }

  return SubmissionGraderLoader.invalidateAttempt(this.activeAttempt.id).then(
    (updatedAttempt: any) => {
      this.activeGrade.resetGrade();
      this.canUserEditGrade = false;
      if (this.inProgressAttempt && this.inProgressAttempt.id === this.activeAttempt.id) {
        this.inProgressAttempt = null;
      }
      this.userAttemptsUpdated(updatedAttempt, this.activeUser.id);
    }
  );
};

SubmissionGrader.prototype.calculateUnpostedCount = function (forceReload: any) {
  return this.loadUsers(forceReload).then((users: any) => {
    if (this.activeAttempt) {
      const userInfo = find(users, { id: this.activeUser.id });
      const updatedScorableAttemptState = buildScorableAttemptState(this.activeAttempt, userInfo);
      this.activeAttempt.scorableAttemptState = updatedScorableAttemptState;
    }

    if (this.isStudentDriven) {
      this.unpostedCount = reduce(
        users,
        (sum: number, userSummary: any) => {
          sum += userSummary.gradeableAttempts.length;
          return sum;
        },
        0
      );
    } else {
      this.unpostedCount = reduce(
        users,
        (sum: number, userSummary: any) => {
          //for observation, any learner w/o a grade or gradeable attempts is included in the count
          const count = isEmpty(userSummary.grade)
            ? userSummary.gradeableAttempts.length || 1
            : 0;
          sum += count;
          return sum;
        },
        0
      );
    }
  });
};

SubmissionGrader.prototype.saveChanges = function (isReleasing: any) {
  return BaseGrader.prototype.saveChanges
    .call(this, isReleasing)
    .then((updatedAttempt: any) => {
      if (isReleasing) {
        this.inProgressAttempt = null;
      }
      return this.userAttemptsUpdated(updatedAttempt, this.activeUser.id);
    })
    .then(() => this.runOnChangeCallbacks());
};

SubmissionGrader.prototype.registerOnChangeCallback = function (cb: any) {
  if (isFunction(cb)) {
    this.onChangeCallbacks.push(cb);
  }
};

SubmissionGrader.prototype.runOnChangeCallbacks = function () {
  each(this.onChangeCallbacks, (callback: any) => callback());
};

export default SubmissionGrader;
