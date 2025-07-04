/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
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

import SubmissionAttemptGrade from '../../assignmentGrade/models/SubmissionAttemptGrade.js';

import SubmissionGraderLoader from './SubmissionGraderLoader.js';

import BaseGrader from './BaseGrader.js';

import Roles from '../../utilities/Roles.js';

import { DRIVER_LEARNER } from '../../utilities/assessmentSettings.js';

import {
  ATTEMPT_OPEN,
  ATTEMPT_SUBMITTED,
  ATTEMPT_FINALIZED,
} from '../../utilities/attemptStates.js';

import { buildScorableAttemptState } from '../../assignmentGrade/models/scoreUtils.js';

export default angular
  .module('lo.SubmissionGrader.SubmissionGrader', [
    Roles.name,
    SubmissionGraderLoader.name,
    BaseGrader.name,
    SubmissionAttemptGrade.name,
  ])
  .factory('SubmissionGrader', [
    'SubmissionGraderLoader',
    'BaseGrader',
    'SubmissionAttemptGrade',
    'Roles',
    '$q',
    function (SubmissionGraderLoader, BaseGrader, SubmissionAttemptGrade, Roles, $q) {
      /**
       * @ngdoc object
       * @memberof lo.SubmissionGrader
       * @description
       *   Container for all users who have submitted anything
       */
      class SubmissionGrader extends BaseGrader {
        constructor(assignment) {
          super(assignment.contentId);

          this.rubric = assignment.rubric;
          this.isStudentDriven = DRIVER_LEARNER === assignment.settings.driver;
          this.instructions = assignment.instructions;
          this.activeAttempt = null;
          this.onChangeCallbacks = [];
          this.inProgressAttempt = null;
        }

        isInProgressAttempt = attempt => {
          if (!attempt.valid) {
            return false;
          }
          return this.isStudentDriven
            ? attempt.state === ATTEMPT_SUBMITTED || !attempt.valid
            : //See CBLPROD-16231 for this oddity
              attempt.state === ATTEMPT_SUBMITTED || attempt.state === ATTEMPT_OPEN;
        };

        isGradableAttempt = attempt => {
          return (
            this.isInProgressAttempt(attempt) ||
            attempt.state === ATTEMPT_FINALIZED ||
            (attempt.state !== ATTEMPT_OPEN && !attempt.valid) ||
            (attempt.state === ATTEMPT_OPEN && !this.isStudentDriven && !attempt.valid)
          );
        };

        loadInfo() {
          const substate = this.getSubstate('assignmentInfo');

          const loader = () => SubmissionGraderLoader.loadInfo(this.assignmentId);

          return this.resolveData(substate, loader);
        }

        loadUsers(reload) {
          const substate = this.getSubstate('users');

          const loader = () => SubmissionGraderLoader.loadUsers(this.assignmentId);

          return this.resolveData(substate, loader, reload);
        }

        loadAttemptsForUser(reload) {
          const userId = this.activeUser.id;

          const substate = this.getSubstate(`attempts-${userId}`);

          const loader = () =>
            SubmissionGraderLoader.loadAttemptsForUser(this.assignmentId, userId);

          return this.resolveData(substate, loader, reload);
        }

        getFormattedAttempts() {
          return $q
            .all({
              userInfos: this.loadUsers(),
              attempts: this.loadAttemptsForUser(),
            })
            .then(({ userInfos, attempts }) => {
              const userId = this.activeUser.id;
              const userInfo = find(userInfos, { id: userId });
              return map(attempts, attempt =>
                SubmissionGraderLoader.formatAttempt(attempt, userInfo)
              );
            });
        }

        userAttemptsUpdated(updatedAttempt, userId) {
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
        }

        loadGradableUsers() {
          return this.loadUsers().then(users => {
            let gradeableUsers = orderBy(
              users,
              [u => u.gradeableAttempts.length, 'fullName'],
              ['desc', 'asc']
            );
            if (this.isStudentDriven) {
              gradeableUsers = filter(users, 'hasViewableAttempts');
            }

            return gradeableUsers;
          });
        }

        loadUserOrderedAttempts() {
          return this.getFormattedAttempts().then(attempts =>
            orderBy(filter(attempts, this.isGradableAttempt), 'createDate')
          );
        }

        loadUserOrderedEffectiveAttempts(userId = this.activeUser.id) {
          return this.loadUserOrderedAttempts(userId).then(attempts =>
            filter(attempts, this.isInProgressAttempt)
          );
        }

        changeUser(userId, attemptId) {
          this.activeUser = null;
          this.activeAttempt = null;
          this.activeGrade = null;
          this.inProgressAttempt = null;

          return this.loadGradableUsers().then(users => {
            if (!users.length) {
              return $q.reject();
            }

            if (this.isStudentDriven) {
              this.activeUser =
                find(users, { id: +userId }) ||
                find(users, u => u.gradeableAttempts.length > 0) ||
                first(users);
            } else {
              this.activeUser =
                find(users, { id: +userId }) ||
                find(users, u => u.gradeableAttempts.length > 0) ||
                find(users, u => u.attemptCount === 0) ||
                first(users);
            }

            attemptId = attemptId || this.activeUser.gradeableAttempts[0];

            this.changeAttempt(attemptId);
          });
        }

        changeAttempt(attemptId) {
          this.activeAttempt = null;
          this.activeGrade = null;

          return this.loadUserOrderedAttempts().then(attempts => {
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
              this.loadInfo().then(info => {
                this.activeGrade = new SubmissionAttemptGrade(
                  this.activeAttempt,
                  this.rubric,
                  info.gradebookPointsPossible
                );
              });
            }
          });
        }

        startAttempt() {
          return SubmissionGraderLoader.startAttempt(this.assignmentId, this.activeUser.id).then(
            newAttempt => {
              const substate = this.getSubstate(`attempts-${this.activeUser.id}`);
              substate.data[newAttempt.id] = newAttempt;
              this.userAttemptsUpdated(newAttempt, this.activeUser.id);
              this.calculateUnpostedCount(true);
              this.inProgressAttempt = newAttempt;
              return newAttempt;
            }
          );
        }

        static curryNext(item, offset) {
          if (!item) {
            return () => null;
          }

          return list => {
            const index = findIndex(list, { id: item.id });
            return list[index + offset];
          };
        }

        nextUser(dir) {
          const formatUser = user => ({
            key: 'STUDENT',
            item: user,
            changeToThis: () => this.changeUser(user.id),
          });

          return this.loadGradableUsers()
            .then(users => {
              const index = findIndex(users, { id: this.activeUser.id });
              return users[index + dir];
            })
            .then(user => user && formatUser(user));
        }

        nextAttempt(dir) {
          const formatAttempt = attempt => ({
            key: 'SUBMISSION',
            item: attempt,
            changeToThis: () => this.changeAttempt(attempt.id),
          });

          return this.loadUserOrderedEffectiveAttempts()
            .then(SubmissionGrader.curryNext(this.activeAttempt, dir))
            .then(attempt => attempt && formatAttempt(attempt));
        }

        findNext(dir) {
          return this.nextAttempt(dir).then(found => found || this.nextUser(dir));
        }

        canInvalidateAttempt() {
          if (!Roles.hasRole('EditCourseGradeRight')) {
            return false;
          }

          return this.activeAttempt.valid;
        }

        invalidateAttempt() {
          if (!this.activeAttempt.valid) {
            return $q.when();
          }

          return SubmissionGraderLoader.invalidateAttempt(this.activeAttempt.id).then(
            updatedAttempt => {
              this.activeGrade.resetGrade();
              this.canUserEditGrade = false;
              if (this.inProgressAttempt && this.inProgressAttempt.id === this.activeAttempt.id) {
                this.inProgressAttempt = null;
              }
              this.userAttemptsUpdated(updatedAttempt, this.activeUser.id);
            }
          );
        }

        calculateUnpostedCount(forceReload) {
          return this.loadUsers(forceReload).then(users => {
            if (this.activeAttempt) {
              const userInfo = find(users, { id: this.activeUser.id });
              const updatedScorableAttemptState = buildScorableAttemptState(
                this.activeAttempt,
                userInfo
              );
              this.activeAttempt.scorableAttemptState = updatedScorableAttemptState;
            }

            if (this.isStudentDriven) {
              this.unpostedCount = reduce(
                users,
                (sum, userSummary) => {
                  sum += userSummary.gradeableAttempts.length;
                  return sum;
                },
                0
              );
            } else {
              this.unpostedCount = reduce(
                users,
                (sum, userSummary) => {
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
        }

        saveChanges(isReleasing) {
          return super
            .saveChanges(isReleasing)
            .then(updatedAttempt => {
              if (isReleasing) {
                this.inProgressAttempt = null;
              }
              return this.userAttemptsUpdated(updatedAttempt, this.activeUser.id);
            })
            .then(() => this.runOnChangeCallbacks());
        }

        registerOnChangeCallback(cb) {
          if (isFunction(cb)) {
            this.onChangeCallbacks.push(cb);
          }
        }

        runOnChangeCallbacks() {
          each(this.onChangeCallbacks, callback => callback());
        }
      }

      return SubmissionGrader;
    },
  ]);
