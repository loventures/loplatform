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
} from '../../../utilities/attemptStates.js';

import {
  formatResponse,
  buildQuestionId,
  buildDisplayDetail,
} from '../../../selectors/quizSelectors.js';

import { buildScorableAttemptState } from '../../../assignmentGrade/models/scoreUtils.ts';

import { QuestionSubmissionAttemptGrade } from '../../../assignmentGrade/models/pure/questionSubmissionAttemptGrade.ts';
import { AutoGradedAttemptGrade } from '../../../assignmentGrade/models/pure/autoGradedAttemptGrade.ts';
import { nativeQ as $q } from '../../../utilities/nativeQ.ts';

import { Roles } from '../../../utilities/pure/roles.ts';

import { quizGraderLoader as QuizGraderLoader } from './quizGraderLoader.ts';
import { BaseGrader } from './baseGrader.ts';

/**
 * Pure-TS port of the Angular `lo.QuizGrader.QuizGrader` factory (graders/QuizGrader.js): the
 * instructor quiz grader — container for all users who have submitted anything. Prototype-chains
 * against the pure constructor-function `BaseGrader`.
 *
 * Lift-and-shift of the ES-class grader as a constructor-function + prototype so it extends the pure
 * `BaseGrader` the same way the pure grade models extend `CompositeGrade`. The GraderProvider's 150ms
 * poll reads mutable state on this object (`activeUser`, `activeAttempt`, `activeGrade`,
 * `gradableQuestionList`, `displayedQuestionList`, `unpostedCount`/`unpostedQuestionCount`, plus the
 * grade models' `isDirty()`/`outgoing.*`); every mutable field + the in-place mutation timing is
 * preserved verbatim. Do NOT make anything immutable, and do NOT reorder the mutations.
 *
 * `changeQuestion` defers its one-shot state mutation with `setTimeout(fn, 0)` — the EXPLICIT `0` is
 * required: the original `$timeout(fn)` (no delay) was digest-throttled, and an undefined-delay timer
 * can degrade into a renderer-hanging tight loop. It is a one-shot defer (not a reschedule loop), so an
 * explicit 0 is correct.
 *
 * The pure children/loader/base are imported directly: pure `QuestionSubmissionAttemptGrade` /
 * `AutoGradedAttemptGrade` models, the pure `quizGraderLoader`, the pure `BaseGrader`, pure `Roles`, and
 * `nativeQ` as `$q`. `QuizGrader.js` keeps a thin `.factory` adapter with the SAME service name so the
 * still-Angular grader module deps keep resolving.
 */
export const QuizGrader = function (this: any, assignmentId?: any) {
  BaseGrader.call(this, assignmentId);

  this.activeAttempt = null;
  this.gradableQuestionList = null;
  this.displayedQuestionList = null;
  this.gradeableUsers = null;
} as any;

QuizGrader.prototype = Object.create(BaseGrader.prototype);
QuizGrader.prototype.constructor = QuizGrader;

QuizGrader.prototype.loadInfo = function () {
  const substate = this.getSubstate('assignmentInfo');

  const loader = () => QuizGraderLoader.loadInfo(this.assignmentId);

  return this.resolveData(substate, loader);
};

QuizGrader.prototype.loadUsers = function (reload: any) {
  const substate = this.getSubstate('users');

  const loader = () => QuizGraderLoader.loadUsers(this.assignmentId);

  return this.resolveData(substate, loader, reload);
};

QuizGrader.prototype.loadAttemptsForUser = function (reload: any) {
  const userId = this.activeUser.id;

  const substate = this.getSubstate(`attempts-${userId}`);

  const loader = () => QuizGraderLoader.loadAttemptsForUser(this.assignmentId, userId);

  return this.resolveData(substate, loader, reload);
};

QuizGrader.prototype.loadAttemptQuestions = function () {
  const questionTuples = map(this.activeAttempt.questions, (question: any, index: number) => {
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
};

QuizGrader.prototype.getFormattedAttempts = function () {
  return $q
    .all({
      userInfos: this.loadUsers(),
      attempts: this.loadAttemptsForUser(),
    })
    .then(({ userInfos, attempts }: any) => {
      const userId = this.activeUser.id;
      const userInfo = find(userInfos, { id: userId });
      return map(attempts, (attempt: any) => QuizGraderLoader.formatAttempt(attempt, userInfo));
    });
};

QuizGrader.prototype.userAttemptsUpdated = function (updatedAttempt: any, userId: any) {
  const substate = this.getSubstate(`attempts-${userId}`);
  assign(substate.data[updatedAttempt.id], updatedAttempt);
  if (updatedAttempt.id === this.activeAttempt.id) {
    this.activeAttempt = {
      ...this.activeAttempt,
      ...updatedAttempt,
    };
  }
  return this.calculateUnpostedCount(true);
};

QuizGrader.prototype.loadGradableUsers = function () {
  if (this.gradeableUsers) {
    return $q.when(this.gradeableUsers);
  }
  return this.loadUsers()
    .then((users: any) => filter(users, 'hasViewableAttempts'))
    .then((users: any) =>
      orderBy(users, [(u: any) => u.gradeableAttempts.length, 'fullName'], ['desc', 'asc'])
    )
    .then((users: any) => {
      this.gradeableUsers = users;
      return users;
    });
};

QuizGrader.prototype.loadUserOrderedAttempts = function () {
  return this.getFormattedAttempts().then((attempts: any) => orderBy(attempts, ['submitTimestamp']));
};

QuizGrader.prototype.loadUserOrderedEffectiveAttempts = function (
  this: any,
  userId = this.activeUser.id
) {
  return this.loadUserOrderedAttempts(userId).then((attempts: any) =>
    filter(attempts, (a: any) => {
      return a.valid && a.scorableAttemptState.awaitsInstructorInput;
    })
  );
};

QuizGrader.prototype.changeUser = function (userId: any, attemptId: any, qIndex: any) {
  this.activeUser = null;
  this.activeAttempt = null;

  this.gradableQuestionList = null;
  this.displayedQuestionList = null;
  this.activeGrade = null;

  return this.loadGradableUsers().then((users: any) => {
    if (!users.length) {
      return $q.reject();
    }

    this.activeUser =
      find(users, { id: +userId }) ||
      find(users, (u: any) => u.gradeableAttempts.length > 0) ||
      first(users);
    attemptId = attemptId || this.activeUser.gradeableAttempts[0];
    this.changeAttempt(attemptId, qIndex);
  });
};

QuizGrader.prototype.changeAttempt = function (attemptId: any, qIndex: any) {
  this.activeAttempt = null;

  this.gradableQuestionList = null;
  this.displayedQuestionList = null;
  this.activeGrade = null;
  return this.loadUserOrderedAttempts().then((attempts: any) => {
    if (!attempts.length) {
      return $q.reject();
    }

    attemptId = attemptId || this.activeUser.gradeableAttempts[0];
    this.activeAttempt = find(attempts, { id: attemptId }) || last(attempts);

    this.canUserEditGrade = this.isUserInstructor && this.activeAttempt.valid;

    this.changeQuestion(qIndex);
  });
};

QuizGrader.prototype.changeQuestion = function (qIndex: any) {
  this.gradableQuestionList = null;
  this.displayedQuestionList = null;
  this.activeGrade = null;
  return this.loadAttemptGradableQuestions().then((questions: any) => {
    setTimeout(() => {
      if (questions.length) {
        return this.changeToOneQuestion(questions, qIndex);
      } else {
        return this.changeToViewAllQuestions();
      }
    }, 0);
  });
};

QuizGrader.prototype.changeToViewAllQuestions = function () {
  this.detailedGradeExists = false;

  return $q
    .all({
      questions: this.loadAttemptQuestions(),
      info: this.loadInfo(),
    })
    .then(({ questions, info }: any) => {
      this.gradableQuestionList = [];
      this.displayedQuestionList = questions;
      this.activeGrade = new (AutoGradedAttemptGrade as any)(
        this.activeAttempt,
        info.gradebookPointsPossible
      );
      this.calcItemsToGrade();
    });
};

QuizGrader.prototype.loadAttemptGradableQuestions = function () {
  return this.loadAttemptQuestions().then((questionTuples: any) =>
    filter(questionTuples, (tuple: any) => tuple.question.manuallyGraded)
  );
};

QuizGrader.prototype.changeToOneQuestion = function (questionTuples: any, qIndex: any) {
  this.detailedGradeExists = true;
  this.gradableQuestionList = [
    find(questionTuples, { index: qIndex }) ||
      find(questionTuples, (tuple: any) => tuple.response.state === RESPONSE_SUBMITTED) ||
      first(questionTuples),
  ];
  this.displayedQuestionList = this.gradableQuestionList;

  return this.recreateGradedActiveGrade().then(() => {
    this.calculateUnpostedCount();
    this.calcItemsToGrade();
  });
};

QuizGrader.prototype.recreateGradedActiveGrade = function () {
  return this.loadInfo().then((info: any) => {
    const question = this.gradableQuestionList[0];
    this.activeGrade = new (QuestionSubmissionAttemptGrade as any)(
      this.activeAttempt,
      question,
      info.gradebookPointsPossible
    );
  });
};

QuizGrader.curryNext = function (item: any, offset: any) {
  if (!item) {
    return () => null;
  }

  return (list: any) => {
    const index = findIndex(list, { id: item.id });
    return list[index + offset];
  };
};

QuizGrader.prototype.nextUser = function (dir: any) {
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

QuizGrader.prototype.nextAttempt = function (dir: any) {
  const formatAttempt = (attempt: any) => ({
    key: 'SUBMISSION',
    item: attempt,
    changeToThis: () => this.changeAttempt(attempt.id),
  });

  return this.loadUserOrderedEffectiveAttempts()
    .then(QuizGrader.curryNext(this.activeAttempt, dir))
    .then((attempt: any) => attempt && formatAttempt(attempt));
};

QuizGrader.prototype.nextQuestion = function (dir: any) {
  const formatQuestion = (question: any) => ({
    key: 'QUESTION',
    item: question,
    changeToThis: () => this.changeQuestion(question.index),
  });

  return this.loadAttemptGradableQuestions()
    .then(QuizGrader.curryNext(this.gradableQuestionList && this.gradableQuestionList[0], dir))
    .then((question: any) => question && formatQuestion(question));
};

QuizGrader.prototype.findNext = function (dir: any) {
  return this.nextQuestion(dir)
    .then((found: any) => found || this.nextAttempt(dir))
    .then((found: any) => found || this.nextUser(dir));
};

QuizGrader.prototype.canInvalidateAttempt = function () {
  if (!Roles.hasRole('EditCourseGradeRight')) {
    return false;
  }

  return this.activeAttempt.valid;
};

QuizGrader.prototype.invalidateAttempt = function () {
  if (!this.activeAttempt.valid) {
    return $q.when();
  }

  return QuizGraderLoader.invalidateAttempt(this.activeAttempt.id).then((updatedAttempt: any) => {
    this.canUserEditGrade = false;
    this.userAttemptsUpdated(updatedAttempt, this.activeUser.id);
  });
};

QuizGrader.prototype.calculateUnpostedCount = function (forceReload: any) {
  return this.loadUsers(forceReload).then((userSummaries: any) => {
    if (this.activeAttempt) {
      const userInfo = find(userSummaries, { id: this.activeUser.id });
      const updatedScorableAttemptState = buildScorableAttemptState(this.activeAttempt, userInfo);
      this.activeAttempt.scorableAttemptState = updatedScorableAttemptState;

      const qTuples = map(this.activeAttempt.questions, (question: any, qIndex: number) => {
        return {
          question,
          response: this.activeAttempt.responses[qIndex],
        };
      });

      if (includes(userInfo.gradeableAttempts, this.activeAttempt.id)) {
        this.unpostedQuestionCount = reduce(
          qTuples,
          (sum: number, { question, response }: any) => {
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
      (sum: number, userSummary: any) => {
        sum += userSummary.gradeableAttempts.length;
        return sum;
      },
      0
    );
  });
};

QuizGrader.prototype.saveChanges = function (isReleasing: any) {
  return this.activeGrade
    .saveChanges(isReleasing)
    .then((updatedAttempt: any) =>
      this.userAttemptsUpdated(updatedAttempt, this.activeUser.id)
    )
    .then(() => {
      if (isReleasing) {
        this.changeQuestion(void 0);
      }
    });
};

export default QuizGrader;
