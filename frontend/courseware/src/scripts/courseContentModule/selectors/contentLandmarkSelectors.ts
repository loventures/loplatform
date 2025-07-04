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

import { findIndex, get } from 'lodash';
import { selectSubmissionActivityComponent } from '../../contentPlayerComponents/activityViews/submission/redux/submissionActivitySelectors.ts';
import { selectQuizActivityComponent } from '../../courseActivityModule/selectors/quizActivitySelectors.ts';
import { selectViewableRelatedContentsByCompetency } from '../../selectors/competencySelectors.ts';
import { selectRootContent } from '../../selectors/contentItemSelectors.ts';
import { selectContentItemsWithNebulousDetails } from '../../selectors/selectContentItemsWithNebulousDetails.ts';
import {
  QUIZ_ACTIVITY_PLAY_ATTEMPT,
  QUIZ_ACTIVITY_PLAY_INSTRUCTIONS,
} from '../../utilities/activityStates';
import {
  CONTENT_TYPE_ASSESSMENT,
  CONTENT_TYPE_ASSIGNMENT,
  CONTENT_TYPE_CHECKPOINT,
  CONTENT_TYPE_DIAGNOSTIC,
  CONTENT_TYPE_DISCUSSION,
  CONTENT_TYPE_FILE_BUNDLE,
  CONTENT_TYPE_HTML,
  CONTENT_TYPE_LTI,
  CONTENT_TYPE_POOLED_ASSESSMENT,
  CONTENT_TYPE_SCORM,
} from '../../utilities/contentTypes.ts';
import {
  selectActualUser,
  selectCourse,
  selectCurrentUser,
} from '../../utilities/rootSelectors.ts';
import { createSelector, Selector } from 'reselect';

import {
  ActiveContent,
  selectPageCompetencyId,
  selectPageContent,
} from './contentEntrySelectors.ts';
import { ContentWithRelationships } from './assembleContentView.ts';
import { QuizSettings } from '../../api/quizApi.ts';

export const showHeaderGrade = (content, activityState) => {
  switch (content.typeId) {
    case CONTENT_TYPE_DIAGNOSTIC:
    case CONTENT_TYPE_ASSESSMENT:
    case CONTENT_TYPE_POOLED_ASSESSMENT:
      return (
        activityState.playerState !== QUIZ_ACTIVITY_PLAY_ATTEMPT &&
        activityState.playerState !== QUIZ_ACTIVITY_PLAY_INSTRUCTIONS
      );
    case CONTENT_TYPE_CHECKPOINT:
      return false;
    default:
      return true;
  }
};

export const selectQuizLikeActivityInfo = createSelector(
  [selectPageContent, selectQuizActivityComponent, selectSubmissionActivityComponent],
  (rawContent, quizActivityComponent, submissionActivityComponent) => {
    const content = rawContent as ContentWithRelationships;
    switch (content.typeId) {
      case CONTENT_TYPE_DIAGNOSTIC:
      case CONTENT_TYPE_ASSESSMENT:
      case CONTENT_TYPE_CHECKPOINT:
      case CONTENT_TYPE_POOLED_ASSESSMENT:
        return {
          settings: get(quizActivityComponent, 'quiz.assessment.settings', {}),
          activityState: get(quizActivityComponent, 'activityState', {}),
          hasSubmittedAttempts: get(quizActivityComponent, 'quiz.hasSubmittedAttempts', false),
        };
      case CONTENT_TYPE_ASSIGNMENT:
        return {
          settings: get(submissionActivityComponent, 'submissionActivity.assessment.settings', {}),
          activityState: get(submissionActivityComponent, 'activityState', {}),
          hasSubmittedAttempts: get(
            submissionActivityComponent,
            'submissionActivity.hasSubmittedAttempts',
            false
          ),
        };
      default:
        return { settings: {}, activityState: {}, hasSubmittedAttempts: false };
    }
  }
);

export const selectFullscreenState = state => state.ui.fullscreenState.fullscreen;

const canShowFullscreen = typeId => {
  switch (typeId) {
    case CONTENT_TYPE_HTML:
    case CONTENT_TYPE_LTI:
    case CONTENT_TYPE_FILE_BUNDLE:
    case CONTENT_TYPE_SCORM:
      return true;
    default:
      return false;
  }
};

export const selectContentHeaderComponent = createSelector(
  [
    selectPageContent,
    selectCurrentUser,
    selectActualUser,
    selectQuizLikeActivityInfo,
    selectFullscreenState,
  ],
  (
    rawContent,
    viewingAs,
    actualUser,
    { settings, activityState, hasSubmittedAttempts },
    fullscreen
  ) => {
    const content = rawContent as ContentWithRelationships;
    const showPrint = content.typeId !== CONTENT_TYPE_DISCUSSION;
    const showAnyGrade = viewingAs.isStudent && showHeaderGrade(content, activityState);
    const hasGrade = !!content.grade;
    /**
     * SCORM is an arbitrary standard, with no strict requirements. As such, it has the potential
     * for grades. It also has the potential to contain nothing but text (or, quizzes with only
     * visual feedback), and as such progress must be granted generously, like an HTML file. Due to
     * the special gradeable-and-fullyprogressed nature of SCORM, we must exclude it below.
     */
    const hasPendingGrade =
      content.hasGradebookEntry &&
      content.progress.isFullyCompleted &&
      content.typeId !== CONTENT_TYPE_SCORM;
    const showGrade = showAnyGrade && (hasGrade || hasPendingGrade || hasSubmittedAttempts);
    const { gradingPolicy, maxAttempts } = settings as QuizSettings;

    const showFullscreen = canShowFullscreen(content.typeId);

    return {
      viewingAs,
      actualUser,
      content,
      gradingPolicy,
      maxAttempts,
      showGrade,
      showPrint,
      showFullscreen,
      fullscreen,
    };
  }
);

const checkShowFooter = (rootContent, content, activityState) => {
  switch (content.typeId) {
    case CONTENT_TYPE_DIAGNOSTIC:
    case CONTENT_TYPE_ASSESSMENT:
    case CONTENT_TYPE_CHECKPOINT:
    case CONTENT_TYPE_POOLED_ASSESSMENT:
      return activityState && activityState.playerState !== QUIZ_ACTIVITY_PLAY_ATTEMPT;
    default: {
      if (rootContent) {
        return content.id !== rootContent.id;
      }
      return true;
    }
  }
};

const selectContentPlayerFooterComponent = createSelector(
  selectCourse,
  selectPageContent,
  selectRootContent,
  selectQuizLikeActivityInfo,
  selectCurrentUser,
  (course, content, rootContent, { activityState }, viewingAs) => {
    const courseContent = content as ContentWithRelationships;
    const isDiagnostic = courseContent.typeId === CONTENT_TYPE_DIAGNOSTIC;
    const showFooter = checkShowFooter(rootContent, content, activityState);
    return {
      course,
      content,
      showBackToModule:
        !viewingAs.isInstructor &&
        showFooter &&
        isDiagnostic &&
        content.id !== course.contentItemRoot,
      showPrevNext: viewingAs.isInstructor || (showFooter && !isDiagnostic),
      showCompetencyFooter: false,
      prev: courseContent.prev || rootContent,
      next: courseContent.next || rootContent,
    };
  }
);

const selectCompetencyPlayerFooterComponent = createSelector(
  selectCourse,
  selectPageContent,
  selectPageCompetencyId,
  selectContentItemsWithNebulousDetails,
  selectViewableRelatedContentsByCompetency,
  (course, rawContent, competencyId, contentItems, viewableRelatedContentsByCompetency) => {
    const relatedContents = viewableRelatedContentsByCompetency[competencyId] || [];
    const content = rawContent as ContentWithRelationships;
    const index = findIndex(relatedContents, { id: content.id });
    return {
      course,
      content,
      showBackToModule: false,
      showPrevNext: true,
      showCompetencyFooter: true,
      prev: relatedContents[index - 1],
      next: relatedContents[index + 1],
    };
  }
);

export const selectContentFooterComponent = state => {
  if (selectPageCompetencyId(state)) {
    return selectCompetencyPlayerFooterComponent(state);
  } else {
    return selectContentPlayerFooterComponent(state);
  }
};
