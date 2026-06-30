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

import { get, set, isFunction } from 'lodash';

import { Roles } from '../../../utilities/pure/roles.ts';
import navBlockerService from '../../../services/navBlockerService.ts';
import { nativeQ as $q } from '../../../utilities/nativeQ.ts';

/**
 * Pure-TS port of the Angular `lo.assignmentGrader.BaseGrader` factory (graders/BaseGrader.js): the
 * base class for the quiz and submission instructor graders. The two graders prototype-chain against
 * this (`QuizGrader` / `SubmissionGrader`).
 *
 * Lift-and-shift as a constructor-function + prototype (matching the pure grade models) so the
 * subclass graders can extend it the same way the pure grade models extend `CompositeGrade`. The
 * GraderProvider's 150ms poll reads mutable state on these objects (`activeUser`, `activeGrade`,
 * `nextItemToGrade`/`prevItemToGrade`, `unpostedCount`, and the grade models' `isDirty()`) — every
 * mutable field + the in-place mutation timing is preserved verbatim. Do NOT make anything immutable.
 *
 * The `resolveData` promise-memoization/caching (`substate.data` / `substate.promise` / `substate.error`)
 * is preserved EXACTLY: a resolved `data` short-circuits, an in-flight `promise` is shared, and the
 * promise clears itself in `.finally`.
 *
 * The pure singletons are imported directly: pure `Roles` (utilities/pure/roles.ts), the pure
 * `navBlockerService` singleton (services/navBlockerService.ts, the same instance the Angular
 * `NavBlockerService` adapter exposes), and `nativeQ` as `$q`. `BaseGrader.js` keeps a thin `.factory`
 * adapter with the SAME service name so the still-Angular grader module deps keep resolving.
 */
export const BaseGrader = function (this: any, assignmentId?: any, contentItemId?: any) {
  this.assignmentId = assignmentId;
  this.contentItemId = contentItemId;

  this.state = {};

  this.status = {};

  this.activeUser = null;

  this.activeGrade = null;

  this.nextItemToGrade = null;
  this.prevItemToGrade = null;

  this.unpostedCount = null;

  this.isUserInstructor = Roles.isStrictlyInstructor();

  this.detailedGradeExists = true;
} as any;

BaseGrader.prototype.getSubstate = function (path: any) {
  return get(this.state, path) || (set(this.state, path, {}) && get(this.state, path));
};

BaseGrader.prototype.resolveData = function (substate: any, loader: any, reload?: any) {
  if (substate.data && !reload) {
    return $q.when(substate.data);
  }

  if (!substate.promise || reload) {
    substate.error = null;
    substate.data = null;
    substate.promise = loader()
      .then((data: any) => (substate.data = data), substate.data)
      .catch((error: any) => (substate.error = error), $q.reject(substate.error))
      .finally(() => (substate.promise = null));
  }

  return substate.promise;
};

BaseGrader.prototype.hasUnsavedChanges = function () {
  return this.activeGrade && this.activeGrade.isDirty();
};

BaseGrader.prototype.blockNavForUnsavedChanges = function () {
  let navBlockCondition = this.hasUnsavedChanges.bind(this);
  this.removeNavBlocker = navBlockerService.register(
    navBlockCondition,
    'GRADER_CONFIRM_MOVE_UNSAVED_CHANGES'
  );
};

BaseGrader.prototype.confirmDiscardChanges = function () {
  if (this.hasUnsavedChanges()) {
    return navBlockerService.confirmNavByModal(['GRADER_CONFIRM_MOVE_UNSAVED_CHANGES']);
  } else {
    return $q.when();
  }
};

BaseGrader.prototype.saveChanges = function (isReleasing: any) {
  return this.activeGrade.saveChanges(isReleasing);
};

BaseGrader.prototype.changeByInfo = function (info: any) {
  if (isFunction(info.changeToThis)) {
    return info.changeToThis();
  } else {
    return $q.reject();
  }
};

BaseGrader.prototype.findNext = function (dir: any) {
  console.error('You must implement findNext for direction', dir);
  return $q.reject('You must implement findNext');
};

BaseGrader.prototype.calcItemsToGrade = function () {
  this.findNext(-1).then((prev: any) => {
    this.prevItemToGrade = prev && {
      ...prev,
      text: 'GRADER_CONTROL_PREV_' + prev.key,
    };
  });
  this.findNext(1).then((next: any) => {
    this.nextItemToGrade = next && {
      ...next,
      text: 'GRADER_CONTROL_NEXT_' + next.key,
    };
  });
};

export default BaseGrader;
