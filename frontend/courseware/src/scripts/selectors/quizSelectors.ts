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

import { QuizAssessment } from '../api/quizApi.ts';
import { CourseState } from '../loRedux';
import { filter, find, groupBy, isEmpty, keyBy, map, pickBy } from 'lodash';
//@TODO fix this
import { selectContent } from '../courseContentModule/selectors/contentEntrySelectors.ts';
import { selectCurrentUserId } from '../utilities/rootSelectors.ts';
import { idMapSelectorCreator } from '../utilities/selectorUtils.ts';
import { createSelector } from 'reselect';

export const selectQuizzes = (state: CourseState) => state.api.quizzes;

export const selectQuizAttemptsByUser = (state: CourseState) => state.api.quizAttemptsByUser;

export const selectQuiz = createSelector([selectContent, selectQuizzes], (contentItem, quizzes) =>
  find<Record<string, QuizAssessment>>(quizzes, q => q.contentId === contentItem.contentId)
);

export const selectQuizAttempts = createSelector(
  [selectCurrentUserId, selectQuiz, selectQuizAttemptsByUser],
  (userId, quiz, quizAttemptsByUser = {}) =>
    pickBy(quizAttemptsByUser[userId], attempt => attempt.contentId === quiz?.contentId)
);

export const quizSelectorCreator = (assessmentId: string) =>
  idMapSelectorCreator(assessmentId, (state: CourseState) => state.api.quizzes);

export const quizStateSelectorCreator = (assessmentId: string) =>
  idMapSelectorCreator(assessmentId, (state: CourseState) => state.ui.quizzesState);

export const buildQuestionId = (question: any) => {
  const reference = question.reference || {
    commit: 'NO_COMMIT',
    nodeName: 'NO_NODENAME',
  };
  return reference.commit + '-' + reference.nodeName;
};

export const buildDisplayDetail = (question: any, noAnswers?: boolean) => {
  return {
    correctAnswer: question.includesCorrect && !noAnswers,
  };
};

const isResponseCorrect = (response: any) => {
  const score = response?.score;
  return !isEmpty(score) && score.pointsPossible === score.pointsAwarded;
};

const isResponseIncorrect = (response: any) => {
  const score = response?.score;
  return !isEmpty(score) && score.pointsPossible != score.pointsAwarded;
};

const isBlank = reason =>
  isEmpty(reason) || reason.match(/^\s*<p[^>]*>(?:\s*<br[^>]*>)?\s*<\/p>\s*$/);

const formatQuestion = (question: any, response?: any, noAnswers?: boolean) => {
  const remediationsByType = groupBy(question.rationales, '_type');
  const isInstructor = !response;
  return {
    ...question,
    id: buildQuestionId(question),
    displayDetail: buildDisplayDetail(question, noAnswers),
    remediationResources: remediationsByType.assetRemediation,
    remediationText: {
      correct:
        remediationsByType.rationale && (isInstructor || isResponseCorrect(response))
          ? filter(map(remediationsByType.rationale, 'reason'), r => !isBlank(r))
          : null,
      incorrect:
        remediationsByType.textRemediation && (isInstructor || isResponseIncorrect(response))
          ? filter(map(remediationsByType.textRemediation, 'reason'), r => !isBlank(r))
          : null,
    },
  };
};

export const formatResponse = (response: any, attempt: any) => {
  return {
    ...response,
    instructorFeedback: map(response.instructorFeedback, feedback => {
      return {
        ...feedback,
        attachments: map(feedback.attachments, attachmentId => attempt.attachments[attachmentId]),
      };
    }),
    attachments: map(response.attachments, attachmentId => attempt.attachments[attachmentId]),
  };
};

const printWithoutAnswersSelector = (state: CourseState) =>
  state.router.location?.query?.answers === 'false';
const printWithoutQuestionsSelector = (state: CourseState) =>
  state.router.location.query.questions === 'false';

export const quizQuestionsSelectorCreator = (assessmentId: string) => {
  const quizQuestionsSelector = idMapSelectorCreator(
    assessmentId,
    (state: CourseState) => state.api.quizQuestions
  );

  return createSelector(
    [quizQuestionsSelector, printWithoutAnswersSelector, printWithoutQuestionsSelector],
    (quizQuestions, noAnswers, noQuestions) => {
      if (noQuestions || !quizQuestions) {
        return null;
      }

      return {
        questionTuples: map(quizQuestions.questionInfo.questions, (question, index) => {
          return {
            index,
            question: formatQuestion(question, undefined, noAnswers),
            response: null,
          };
        }),
      };
    }
  );
};

export const quizQuestionsStateSelectorCreator = (assessmentId: string) =>
  idMapSelectorCreator(assessmentId, (state: CourseState) => state.ui.quizQuestionsState);

export const userAttemptsSelectorCreator = (userId: number) => {
  const userIdSelector = userId ? () => ({ id: userId }) : selectCurrentUserId;
  return idMapSelectorCreator(userIdSelector, (state: CourseState) => state.api.quizAttemptsByUser);
};

export const userAttemptStatesSelectorCreator = (userId: number) => {
  const userIdSelector = userId ? () => ({ id: userId }) : selectCurrentUserId;
  return idMapSelectorCreator(
    userIdSelector,
    (state: CourseState) => state.ui.quizAttemptsByUserState
  );
};

export const quizSettingsSelectorCreator = (quizId: string) => {
  const selectQuiz = quizSelectorCreator(quizId);
  return createSelector([selectQuiz], (quiz = {}) => {
    return quiz.settings || {};
  });
};

export const quizAttemptsSelectorCreator = (assessmentId: string, userId: number) =>
  createSelector(
    [quizSelectorCreator(assessmentId), userAttemptsSelectorCreator(userId)],
    (assessment, userAttempts) => {
      const attempts =
        assessment && assessment.attempts && userAttempts ? keyBy(assessment.attempts) : {};
      return attempts;
    }
  );

export const attemptDetailsSelectorCreator = (attemptId: number, userId: number) => {
  const userAttemptsSelector = userAttemptsSelectorCreator(userId);

  const attemptSelector = idMapSelectorCreator(attemptId, userAttemptsSelector);

  return createSelector([attemptSelector, printWithoutAnswersSelector], (attempt, noAnswers) => {
    if (!attempt) {
      return {};
    }

    return {
      ...attempt,
      questionTuples: map(attempt.questions, (question, index) => {
        const response = attempt.responses[index];
        return {
          index,
          question: formatQuestion(question, response, noAnswers),
          response: formatResponse(response, attempt),
        };
      }),
    };
  });
};

export const quizPlayerStateSelectorCreator = (attemptId: number) =>
  idMapSelectorCreator(attemptId, (state: CourseState) => state.ui.quizPlayerState);

export const quizQuestionSubmissionStateSelectorCreator = (attemptId: number) =>
  idMapSelectorCreator(attemptId, (state: CourseState) => state.ui.quizQuestionSubmissionState);

export const quizAttemptSubmissionStateSelectorCreator = (attemptId: number) =>
  idMapSelectorCreator(attemptId, (state: CourseState) => state.ui.quizAttemptSubmissionState);

export const quizAttemptSaveStateSelectorCreator = (attemptId: number) =>
  idMapSelectorCreator(attemptId, (state: CourseState) => state.ui.quizAttemptSaveState);

export const quizAttemptSaveOrSubmitStateSelectorCreator = (attemptId: number) => {
  const saveStateSelector = quizAttemptSaveStateSelectorCreator(attemptId);
  const submitStateSelector = quizAttemptSubmissionStateSelectorCreator(attemptId);

  return createSelector(
    [saveStateSelector, submitStateSelector],
    (saveState = {}, submitState = {}) => {
      //since submit is final, it trumps the save state
      //if it has anything going on
      if (submitState.loaded || submitState.loading || submitState.error) {
        return { ...submitState };
      } else {
        return { ...saveState };
      }
    }
  );
};
