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

import dayjs from 'dayjs';
import { filter, last, map, orderBy, some } from 'lodash';
import {
  ActiveContent,
  selectPageContent,
  selectPageContentId,
} from '../../courseContentModule/selectors/contentEntrySelectors.ts';
import { selectQuiz, selectQuizAttempts } from '../../selectors/quizSelectors.ts';
import {
  LEGACY_ASSESSMENT_SETTING_MULTI_PAGE,
  LEGACY_ASSESSMENT_SETTING_SINGLE_PAGE,
  QUIZ_SETTING_SINGLE_PAGE,
} from '../../utilities/assessmentSettings';
import { ATTEMPT_FINALIZED, ATTEMPT_OPEN, ATTEMPT_SUBMITTED } from '../../utilities/attemptStates';
import { createInstanceSelector } from '../../utilities/reduxify.ts';
import {
  selectCourse,
  selectCurrentUser,
  UserWithRoleInfo,
} from '../../utilities/rootSelectors.ts';
import { createSelector, createStructuredSelector, Selector } from 'reselect';

import { selectContentActivityLoaderComponent } from './activitySelectors';
import { CourseState } from '../../loRedux';
import { Course } from '../../../loPlatform';

export { selectContentActivityLoaderComponent };

const selectQuizOpenAttemptUI = createInstanceSelector(
  (state: CourseState) => state.ui.quizActivityOpenAttemptState,
  (state, ownProps) => selectQuiz(state, ownProps).contentId
);

const selectCompetencyBreakdown = createInstanceSelector(
  (state: CourseState) => state.api.competencyBreakdownByContent,
  selectPageContentId
);

export const selectQuizActivityData = createSelector(
  [selectCourse, selectQuiz, selectQuizAttempts, selectCompetencyBreakdown],
  (course, assessment, attempts, competencyBreakdownByAttempt) => {
    const validAttempts = map(
      filter(attempts, att => att.valid),
      attempt => ({
        ...attempt,
        responses: map(attempt.responses, response => {
          return {
            ...response,
            attachments: map(
              response.attachments,
              attachmentId => attempt.attachments[attachmentId]
            ),
          };
        }),
      })
    );

    const allOrderedAttempts = orderBy(validAttempts, 'createDate');

    const latestAttempt = last(allOrderedAttempts);

    const isLatestAttemptOpen = latestAttempt && latestAttempt.state === ATTEMPT_OPEN;

    const latestAttemptCompetencyBreakdown = !isLatestAttemptOpen
      ? competencyBreakdownByAttempt[latestAttempt?.id]?.mastered
      : null;

    const orderedAttempts = isLatestAttemptOpen
      ? allOrderedAttempts.slice(0, -1)
      : allOrderedAttempts;

    const latestSubmittedAttempt = last(orderedAttempts);

    const isLatestSubmittedAttemptFinalized =
      latestSubmittedAttempt && latestSubmittedAttempt.state === ATTEMPT_FINALIZED;

    const hasSubmittedAttempts = some(
      validAttempts,
      attempt => attempt.state === ATTEMPT_FINALIZED || attempt.state === ATTEMPT_SUBMITTED
    );

    const settings = assessment?.settings ?? ({} as any);
    const unlimitedAttempts = settings?.maxAttempts === null;
    const numSubmittedAttempts = filter(validAttempts, a => a.state !== ATTEMPT_OPEN).length;
    const attemptsRemaining = settings?.maxAttempts - numSubmittedAttempts;
    const openAttempt = isLatestAttemptOpen ? latestAttempt : null;

    // in LW, `settings` always has a `navigationPolicy`, but this selector is also used before we have
    // loaded the quiz so everything has to be conditional :rage:
    const singlePage = settings?.navigationPolicy
      ? settings.navigationPolicy.policyType === QUIZ_SETTING_SINGLE_PAGE
      : false;

    const now = dayjs();
    const isCourseEnded = course.endDate && now.isAfter(course.endDate);
    const canPlayAttempt =
      !assessment?.pastDeadline &&
      !isCourseEnded &&
      (openAttempt || unlimitedAttempts || attemptsRemaining > 0);

    //fugly...fix in CBLPROD-15074
    const legacyFriendlySettings = settings?.navigationPolicy
      ? {
          ...settings,
          pagingPolicy: singlePage
            ? LEGACY_ASSESSMENT_SETTING_SINGLE_PAGE
            : LEGACY_ASSESSMENT_SETTING_MULTI_PAGE,
          singlePage,
        }
      : settings;

    return {
      assessment: {
        ...(assessment ?? {}),
        settings: legacyFriendlySettings,
      },
      orderedAttempts,
      latestAttempt,
      isLatestAttemptOpen,
      latestAttemptCompetencyBreakdown,
      openAttempt,
      hasSubmittedAttempts,
      attemptsRemaining,
      attemptNumber: 1 + numSubmittedAttempts,
      unlimitedAttempts,
      canPlayAttempt,
      latestSubmittedAttempt,
      isLatestSubmittedAttemptFinalized,
    };
  }
);

export const selectQuizActivityOpenAttemptLoaderComponent = createSelector(
  [selectQuizOpenAttemptUI, selectQuizActivityData],
  (loadingState, quiz) => {
    return {
      loadingState: {
        ...loadingState,
        //TODO: make this less magical( logic is split between too many places )
        //fix in TECH-1335
        //the OpenAttemptComponent is considered "loaded" if:
        //we can select an existing open attempt from previously loaded quiz data
        //or if we created a new attempt via createLoaderComponent which this is
        //exclusively binded to.
        //
        //During re-selection updates, we don't want to unload this component if the
        //open attempt it previously found became closed due to submission since
        //there are post-submission operations (like exit after save, or
        //view quiz results after submit) that may not have run yet.
        //see contentQuizPlayerLoader#$init
        //see QuizAPI#submitQuestions
        loaded: !loadingState.error && !!quiz.openAttempt,
      },
    };
  }
);

export const selectQuizActivityComponent = createStructuredSelector<
  CourseState,
  {
    quiz: ReturnType<typeof selectQuizActivityData>;
    course: Course;
    content: ActiveContent;
    viewingAs: UserWithRoleInfo;
  }
>({
  quiz: selectQuizActivityData,
  course: selectCourse,
  content: selectPageContent,
  viewingAs: selectCurrentUser,
});
