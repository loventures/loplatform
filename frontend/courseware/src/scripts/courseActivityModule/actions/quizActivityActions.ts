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

import Course from '../../bootstrap/course.js';
import dayjs from 'dayjs';
import { compact, keyBy } from 'lodash';
import { viewParentFromContentActionCreator } from '../../courseContentModule/actions/contentPageActions.js';
import { createDataListUpdateMergeAction } from '../../utilities/apiDataActions.js';
import { loadingActionCreatorMaker } from '../../utilities/loadingStateUtils.js';
import { nativeRuntime } from '../../utilities/pure/callAggregator.ts';
import { competencyBreakdownService } from '../../services/competencyBreakdownService.ts';
import { quizAPI } from '../../services/quizAPI.ts';

// A4 Phase 4: native stand-ins for the digest-integrated $q used here. These quiz
// loaders feed redux (covered by the $ngRedux digest bridge) and wrap the axios
// QuizAPI / CompetencyBreakdownService (Request-boundary bridge), so native promises
// render correctly. Every `$q.all` here is the object-map form, matching
// pure/callAggregator's native `all`.
const $q = {
  when: (value: any) => Promise.resolve(value),
  all: nativeRuntime.all,
};

const loadQuiz = (contentId: any) => quizAPI.loadQuiz(contentId);

const loadQuizAttempts = (contentId: any, viewingAs: any, actualUserId: any) => {
  const start = dayjs();
  return viewingAs.isInstructor && viewingAs.id === actualUserId
    ? $q.when({})
    : quizAPI
        .loadAttempts(contentId, viewingAs.id)
        .then((attempts: any) => {
          attempts.forEach((attempt: any) => {
            if (attempt.remainingMillis)
              attempt.deadline = start.add(attempt.remainingMillis, 'ms');
          });
          return keyBy(attempts, 'id');
        });
};

const loadCompetencySummaryInfo = (assessment: any, viewingAs: any, actualUserId: any) => {
  if (viewingAs.isInstructor && viewingAs.id === actualUserId) {
    return $q.when({});
  } else {
    return competencyBreakdownService
      .getCompetencyStatus(assessment.contentId, viewingAs.id)
      .then((status: any) =>
        keyBy(
          status.map(({ attemptId, mastered }: any) => ({
            attemptId,
            mastered: new Set(mastered),
          })),
          'attemptId'
        )
      );
  }
};

const createQuizAttempt = (contentId: any, competencies: any) => {
  const start = dayjs();
  return quizAPI
    .createAttempt(contentId, Course.id, competencies)
    .then((attempt: any) => {
      if (attempt.remainingMillis) attempt.deadline = start.add(attempt.remainingMillis, 'ms');
      return attempt;
    });
};

export const quizActivityLoader = (content: any, viewingAs: any, actualUserId: any) =>
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

export const quizActivityUpdateLoader = (
  content: any,
  quiz: any,
  viewingAs: any,
  actualUserId: any
) =>
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
}: any) => {
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

export const quizActivityAttemptSubmittedAC = (data: any) => {
  return {
    type: 'QUIZ_ACTIVITY_ATTEMPT_SUBMITTED',
    id: data.contentId,
    userId: data.viewingAsId,
  };
};

export const quizActivityAttemptSavedAC = (data: any) => {
  return {
    type: 'QUIZ_ACTIVITY_ATTEMPT_SAVED',
    id: data.contentId,
    userId: data.viewingAsId,
  };
};

export const loadQuizActivityActionCreator = (content: any, viewingAs: any, actualUserId: any) => {
  const loadingActionCreator = loadingActionCreatorMaker(
    { sliceName: 'contentActivityLoadingState' },
    quizActivityLoader,
    [loadQuizActivitySuccessACs],
    (content: any) => ({ id: content.id })
  );

  return loadingActionCreator(content, viewingAs, actualUserId);
};

export const quizActivityAfterSubmitActionCreator = (
  content: any,
  quiz: any,
  viewingAs: any,
  actualUserId: any
) => {
  const loadingActionCreator = loadingActionCreatorMaker(
    { sliceName: 'contentActivityLoadingState' },
    quizActivityUpdateLoader,
    [loadQuizActivitySuccessACs, quizActivityAttemptSubmittedAC],
    (content: any) => ({ id: content.id })
  );

  return loadingActionCreator(content, quiz, viewingAs, actualUserId);
};

export const quizActivityAfterSaveActionCreator = (
  content: any,
  quiz: any,
  viewingAs: any,
  actualUserId: any
) => {
  const loadingActionCreator = loadingActionCreatorMaker(
    { sliceName: 'contentActivityLoadingState' },
    quizActivityUpdateLoader,
    [loadQuizActivitySuccessACs, quizActivityAttemptSavedAC],
    (content: any) => ({ id: content.id })
  );

  return (dispatch: any) => {
    dispatch(loadingActionCreator(content, quiz, viewingAs, actualUserId));
    dispatch(viewParentFromContentActionCreator(content));
  };
};

export const quizActivityAfterInvalidateActionCreator = (
  content: any,
  quiz: any,
  viewingAs: any,
  actualUserId: any
) => {
  const loadingActionCreator = loadingActionCreatorMaker(
    { sliceName: 'contentActivityLoadingState' },
    quizActivityUpdateLoader,
    [loadQuizActivitySuccessACs],
    (content: any) => ({ id: content.id })
  );

  return (dispatch: any) => {
    dispatch(loadingActionCreator(content, quiz, viewingAs, actualUserId));
  };
};

const quizAttemptCreatedAC = ({ content, attempt, userId }: any) => {
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

const createAttemptLoader = (content: any, quiz: any, viewingAs: any, competencies: any) => {
  return createQuizAttempt(content.contentId, competencies).then((attempt: any) => {
    return {
      content,
      attempt,
      userId: viewingAs.id,
    };
  });
};

export const createNewQuizAttemptActionCreator = (
  content: any,
  quiz: any,
  viewingAs: any,
  competencies: any
) => {
  const loadingActionCreator = loadingActionCreatorMaker(
    { sliceName: 'quizActivityOpenAttemptState' },
    createAttemptLoader,
    [quizAttemptCreatedAC],
    (content: any) => ({ id: content.contentId })
  );

  return loadingActionCreator(content, quiz, viewingAs, competencies);
};
