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

import Course from '../../bootstrap/course.js';
import dayjs from 'dayjs';
import { compact, keyBy } from 'lodash';
import { viewParentFromContentActionCreator } from '../../courseContentModule/actions/contentPageActions.js';
import { createDataListUpdateMergeAction } from '../../utilities/apiDataActions.js';
import { loadingActionCreatorMaker } from '../../utilities/loadingStateUtils.js';
import { $q, lojector } from '../../loject.js';

const loadQuiz = contentId => lojector.get('QuizAPI').loadQuiz(contentId);

const loadQuizAttempts = (contentId, viewingAs, actualUserId) => {
  const start = dayjs();
  return viewingAs.isInstructor && viewingAs.id === actualUserId
    ? $q.when({})
    : lojector
        .get('QuizAPI')
        .loadAttempts(contentId, viewingAs.id)
        .then(attempts => {
          attempts.forEach(attempt => {
            if (attempt.remainingMillis)
              attempt.deadline = start.add(attempt.remainingMillis, 'ms');
          });
          return keyBy(attempts, 'id');
        });
};

const loadCompetencySummaryInfo = (assessment, viewingAs, actualUserId) => {
  if (viewingAs.isInstructor && viewingAs.id === actualUserId) {
    return $q.when({});
  } else {
    return lojector
      .get('CompetencyBreakdownService')
      .getCompetencyStatus(assessment.contentId, viewingAs.id)
      .then(status =>
        keyBy(
          status.map(({ attemptId, mastered }) => ({
            attemptId,
            mastered: new Set(mastered),
          })),
          'attemptId'
        )
      );
  }
};

const createQuizAttempt = (contentId, competencies) => {
  const start = dayjs();
  return lojector
    .get('QuizAPI')
    .createAttempt(contentId, Course.id, competencies)
    .then(attempt => {
      if (attempt.remainingMillis) attempt.deadline = start.add(attempt.remainingMillis, 'ms');
      return attempt;
    });
};

export const quizActivityLoader = (content, viewingAs, actualUserId) =>
  $q
    .all({
      assessment: loadQuiz(content.contentId),
      attempts: loadQuizAttempts(content.contentId, viewingAs, actualUserId),
    })
    .then(({ assessment, attempts }) => {
      return $q.all({
        contentId: content.id,
        viewingAsId: viewingAs.id,
        assessment,
        attempts,
        competencyBreakdown: loadCompetencySummaryInfo(assessment, viewingAs, actualUserId),
      });
    });

export const quizActivityUpdateLoader = (content, quiz, viewingAs, actualUserId) =>
  $q
    .all({
      attempts: loadQuizAttempts(content.contentId, viewingAs, actualUserId),
    })
    .then(({ attempts }) => {
      return $q.all({
        contentId: content.id,
        viewingAsId: viewingAs.id,
        attempts,
        competencyBreakdown: loadCompetencySummaryInfo(quiz.assessment, viewingAs, actualUserId),
      });
    });

export const loadQuizActivitySuccessACs = ({
  contentId,
  viewingAsId,
  assessment,
  competencyBreakdown,
  attempts,
}) => {
  return compact([
    assessment &&
      createDataListUpdateMergeAction('quizzes', {
        [assessment.contentId]: assessment,
      }),
    attempts &&
      createDataListUpdateMergeAction('quizAttemptsByUser', {
        [viewingAsId]: attempts,
      }),
    competencyBreakdown &&
      createDataListUpdateMergeAction('competencyBreakdownByContent', {
        [contentId]: competencyBreakdown,
      }),
  ]);
};

export const quizActivityAttemptSubmittedAC = data => {
  return {
    type: 'QUIZ_ACTIVITY_ATTEMPT_SUBMITTED',
    id: data.contentId,
    userId: data.viewingAsId,
  };
};

export const quizActivityAttemptSavedAC = data => {
  return {
    type: 'QUIZ_ACTIVITY_ATTEMPT_SAVED',
    id: data.contentId,
    userId: data.viewingAsId,
  };
};

export const loadQuizActivityActionCreator = (content, viewingAs, actualUserId) => {
  const loadingActionCreator = loadingActionCreatorMaker(
    { sliceName: 'contentActivityLoadingState' },
    quizActivityLoader,
    [loadQuizActivitySuccessACs],
    content => ({ id: content.id })
  );

  return loadingActionCreator(content, viewingAs, actualUserId);
};

export const quizActivityAfterSubmitActionCreator = (content, quiz, viewingAs, actualUserId) => {
  const loadingActionCreator = loadingActionCreatorMaker(
    { sliceName: 'contentActivityLoadingState' },
    quizActivityUpdateLoader,
    [loadQuizActivitySuccessACs, quizActivityAttemptSubmittedAC],
    content => ({ id: content.id })
  );

  return loadingActionCreator(content, quiz, viewingAs, actualUserId);
};

export const quizActivityAfterSaveActionCreator = (content, quiz, viewingAs, actualUserId) => {
  const loadingActionCreator = loadingActionCreatorMaker(
    { sliceName: 'contentActivityLoadingState' },
    quizActivityUpdateLoader,
    [loadQuizActivitySuccessACs, quizActivityAttemptSavedAC],
    content => ({ id: content.id })
  );

  return dispatch => {
    dispatch(loadingActionCreator(content, quiz, viewingAs, actualUserId));
    dispatch(viewParentFromContentActionCreator(content));
  };
};

export const quizActivityAfterInvalidateActionCreator = (
  content,
  quiz,
  viewingAs,
  actualUserId
) => {
  const loadingActionCreator = loadingActionCreatorMaker(
    { sliceName: 'contentActivityLoadingState' },
    quizActivityUpdateLoader,
    [loadQuizActivitySuccessACs],
    content => ({ id: content.id })
  );

  return dispatch => {
    dispatch(loadingActionCreator(content, quiz, viewingAs, actualUserId));
  };
};

const quizAttemptCreatedAC = ({ content, attempt, userId }) => {
  return [
    createDataListUpdateMergeAction('quizAttemptsByUser', {
      [userId]: {
        [attempt.id]: attempt,
      },
    }),
    {
      type: 'QUIZ_ACTIVITY_ATTEMPT_CREATED',
      id: content.id,
      userId: userId,
    },
  ];
};

const createAttemptLoader = (content, quiz, viewingAs, competencies) => {
  return createQuizAttempt(content.contentId, competencies).then(attempt => {
    return {
      content,
      attempt,
      userId: viewingAs.id,
    };
  });
};

export const createNewQuizAttemptActionCreator = (content, quiz, viewingAs, competencies) => {
  const loadingActionCreator = loadingActionCreatorMaker(
    { sliceName: 'quizActivityOpenAttemptState' },
    createAttemptLoader,
    [quizAttemptCreatedAC],
    content => ({ id: content.contentId })
  );

  return loadingActionCreator(content, quiz, viewingAs, competencies);
};
