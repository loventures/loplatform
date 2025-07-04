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
  find,
  assign,
  orderBy,
  filter,
  first,
  last,
  reduce,
  map,
  includes,
  findIndex,
} from 'lodash';

import {
  RESPONSE_SUBMITTED,
  RESPONSE_SCORED,
  RESPONSE_SCORED_RELEASED,
} from '../../utilities/attemptStates.js';

import {
  formatResponse,
  buildQuestionId,
  buildDisplayDetail,
} from '../../selectors/quizSelectors.js';

import { buildScorableAttemptState } from '../../assignmentGrade/models/scoreUtils.js';

import QuestionSubmissionAttemptGrade from '../../assignmentGrade/models/QuestionSubmissionAttemptGrade.js';
import AutoGradedAttemptGrade from '../../assignmentGrade/models/AutoGradedAttemptGrade.js';

import QuizGraderLoader from './QuizGraderLoader.js';
import BaseGrader from './BaseGrader.js';

import Roles from '../../utilities/Roles.js';

export default angular
  .module('lo.QuizGrader.QuizGrader', [
    Roles.name,
    QuizGraderLoader.name,
    QuestionSubmissionAttemptGrade.name,
    AutoGradedAttemptGrade.name,
    BaseGrader.name,
  ])
  .factory('QuizGrader', [
    'QuizGraderLoader',
    'QuestionSubmissionAttemptGrade',
    'AutoGradedAttemptGrade',
    'Roles',
    '$q',
    '$timeout',
    'BaseGrader',
    function (
      QuizGraderLoader,
      QuestionSubmissionAttemptGrade,
      AutoGradedAttemptGrade,
      Roles,
      $q,
      $timeout,
      BaseGrader
    ) {
      /**
       * @ngdoc object
       * @memberof lo.QuizGrader
       * @description
       *   Container for all users who have submitted anything
       */
      class QuizGrader extends BaseGrader {
        constructor(assignmentId) {
          super(assignmentId);

          this.activeAttempt = null;
          this.gradableQuestionList = null;
          this.displayedQuestionList = null;
          this.gradeableUsers = null;
        }

        loadInfo() {
          const substate = this.getSubstate('assignmentInfo');

          const loader = () => QuizGraderLoader.loadInfo(this.assignmentId);

          return this.resolveData(substate, loader);
        }

        loadUsers(reload) {
          const substate = this.getSubstate('users');

          const loader = () => QuizGraderLoader.loadUsers(this.assignmentId);

          return this.resolveData(substate, loader, reload);
        }

        loadAttemptsForUser(reload) {
          const userId = this.activeUser.id;

          const substate = this.getSubstate(`attempts-${userId}`);

          const loader = () => QuizGraderLoader.loadAttemptsForUser(this.assignmentId, userId);

          return this.resolveData(substate, loader, reload);
        }

        loadAttemptQuestions() {
          const questionTuples = map(this.activeAttempt.questions, (question, index) => {
            const response = this.activeAttempt.responses[index];
            return {
              index,
              id: buildQuestionId(question),
              question: {
                ...question,
                id: buildQuestionId(question),
                displayDetail: buildDisplayDetail(question),
              },
              response: formatResponse(response, this.activeAttempt),
            };
          });

          return $q.when(questionTuples);
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
              return map(attempts, attempt => QuizGraderLoader.formatAttempt(attempt, userInfo));
            });
        }

        userAttemptsUpdated(updatedAttempt, userId) {
          const substate = this.getSubstate(`attempts-${userId}`);
          assign(substate.data[updatedAttempt.id], updatedAttempt);
          if (updatedAttempt.id === this.activeAttempt.id) {
            this.activeAttempt = {
              ...this.activeAttempt,
              ...updatedAttempt,
            };
          }
          return this.calculateUnpostedCount(true);
        }

        loadGradableUsers() {
          if (this.gradeableUsers) {
            return $q.when(this.gradeableUsers);
          }
          return this.loadUsers()
            .then(users => filter(users, 'hasViewableAttempts'))
            .then(users =>
              orderBy(users, [u => u.gradeableAttempts.length, 'fullName'], ['desc', 'asc'])
            )
            .then(users => {
              this.gradeableUsers = users;
              return users;
            });
        }

        loadUserOrderedAttempts() {
          return this.getFormattedAttempts().then(attempts =>
            orderBy(attempts, ['submitTimestamp'])
          );
        }

        loadUserOrderedEffectiveAttempts(userId = this.activeUser.id) {
          return this.loadUserOrderedAttempts(userId).then(attempts =>
            filter(attempts, a => {
              return a.valid && a.scorableAttemptState.awaitsInstructorInput;
            })
          );
        }

        changeUser(userId, attemptId, qIndex) {
          this.activeUser = null;
          this.activeAttempt = null;

          this.gradableQuestionList = null;
          this.displayedQuestionList = null;
          this.activeGrade = null;

          return this.loadGradableUsers().then(users => {
            if (!users.length) {
              return $q.reject();
            }

            this.activeUser =
              find(users, { id: +userId }) ||
              find(users, u => u.gradeableAttempts.length > 0) ||
              first(users);
            attemptId = attemptId || this.activeUser.gradeableAttempts[0];
            this.changeAttempt(attemptId, qIndex);
          });
        }

        changeAttempt(attemptId, qIndex) {
          this.activeAttempt = null;

          this.gradableQuestionList = null;
          this.displayedQuestionList = null;
          this.activeGrade = null;
          return this.loadUserOrderedAttempts().then(attempts => {
            if (!attempts.length) {
              return $q.reject();
            }

            attemptId = attemptId || this.activeUser.gradeableAttempts[0];
            this.activeAttempt = find(attempts, { id: attemptId }) || last(attempts);

            this.canUserEditGrade = this.isUserInstructor && this.activeAttempt.valid;

            this.changeQuestion(qIndex);
          });
        }

        changeQuestion(qIndex) {
          this.gradableQuestionList = null;
          this.displayedQuestionList = null;
          this.activeGrade = null;
          return this.loadAttemptGradableQuestions().then(questions => {
            $timeout(() => {
              if (questions.length) {
                return this.changeToOneQuestion(questions, qIndex);
              } else {
                return this.changeToViewAllQuestions();
              }
            });
          });
        }

        changeToViewAllQuestions() {
          this.detailedGradeExists = false;

          return $q
            .all({
              questions: this.loadAttemptQuestions(),
              info: this.loadInfo(),
            })
            .then(({ questions, info }) => {
              this.gradableQuestionList = [];
              this.displayedQuestionList = questions;
              this.activeGrade = new AutoGradedAttemptGrade(
                this.activeAttempt,
                info.gradebookPointsPossible
              );
              this.calcItemsToGrade();
            });
        }

        loadAttemptGradableQuestions() {
          return this.loadAttemptQuestions().then(questionTuples =>
            filter(questionTuples, tuple => tuple.question.manuallyGraded)
          );
        }

        changeToOneQuestion(questionTuples, qIndex) {
          this.detailedGradeExists = true;
          this.gradableQuestionList = [
            find(questionTuples, { index: qIndex }) ||
              find(questionTuples, tuple => tuple.response.state === RESPONSE_SUBMITTED) ||
              first(questionTuples),
          ];
          this.displayedQuestionList = this.gradableQuestionList;

          return this.recreateGradedActiveGrade().then(() => {
            this.calculateUnpostedCount();
            this.calcItemsToGrade();
          });
        }

        recreateGradedActiveGrade() {
          return this.loadInfo().then(info => {
            const question = this.gradableQuestionList[0];
            this.activeGrade = new QuestionSubmissionAttemptGrade(
              this.activeAttempt,
              question,
              info.gradebookPointsPossible
            );
          });
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
            .then(QuizGrader.curryNext(this.activeAttempt, dir))
            .then(attempt => attempt && formatAttempt(attempt));
        }

        nextQuestion(dir) {
          const formatQuestion = question => ({
            key: 'QUESTION',
            item: question,
            changeToThis: () => this.changeQuestion(question.index),
          });

          return this.loadAttemptGradableQuestions()
            .then(
              QuizGrader.curryNext(this.gradableQuestionList && this.gradableQuestionList[0], dir)
            )
            .then(question => question && formatQuestion(question));
        }

        findNext(dir) {
          return this.nextQuestion(dir)
            .then(found => found || this.nextAttempt(dir))
            .then(found => found || this.nextUser(dir));
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

          return QuizGraderLoader.invalidateAttempt(this.activeAttempt.id).then(updatedAttempt => {
            this.canUserEditGrade = false;
            this.userAttemptsUpdated(updatedAttempt, this.activeUser.id);
          });
        }

        calculateUnpostedCount(forceReload) {
          return this.loadUsers(forceReload).then(userSummaries => {
            if (this.activeAttempt) {
              const userInfo = find(userSummaries, { id: this.activeUser.id });
              const updatedScorableAttemptState = buildScorableAttemptState(
                this.activeAttempt,
                userInfo
              );
              this.activeAttempt.scorableAttemptState = updatedScorableAttemptState;

              const qTuples = map(this.activeAttempt.questions, (question, qIndex) => {
                return {
                  question,
                  response: this.activeAttempt.responses[qIndex],
                };
              });

              if (includes(userInfo.gradeableAttempts, this.activeAttempt.id)) {
                this.unpostedQuestionCount = reduce(
                  qTuples,
                  (sum, { question, response }) => {
                    if (
                      question.manuallyGraded &&
                      response.state != RESPONSE_SCORED &&
                      response.state != RESPONSE_SCORED_RELEASED
                    ) {
                      sum += 1;
                    }

                    return sum;
                  },
                  0
                );
              } else {
                this.unpostedQuestionCount = 0;
              }
            }

            this.unpostedCount = reduce(
              userSummaries,
              (sum, userSummary) => {
                sum += userSummary.gradeableAttempts.length;
                return sum;
              },
              0
            );
          });
        }

        saveChanges(isReleasing) {
          return this.activeGrade
            .saveChanges(isReleasing)
            .then(updatedAttempt => this.userAttemptsUpdated(updatedAttempt, this.activeUser.id))
            .then(() => {
              if (isReleasing) {
                this.changeQuestion(void 0);
              }
            });
        }
      }

      return QuizGrader;
    },
  ]);
