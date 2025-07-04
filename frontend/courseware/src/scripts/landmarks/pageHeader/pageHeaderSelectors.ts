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

import Course from '../../bootstrap/course';
import * as tutorialSlice from '../../tutorial/tutorialSlice';
import { find, includes, isEmpty } from 'lodash';
import {
  isCourseWithRelationships,
  selectPageContent,
} from '../../courseContentModule/selectors/contentEntrySelectors';
import { selectContentItems } from '../../selectors/contentItemSelectors';
import { selectMeekMode } from '../../selectors/meekModeSelectors';
import { isAssignment, isDiscussion } from '../../utilities/contentTypes';
import { appIsFramed } from '../../utilities/deviceType';
import { selectCourse, selectCurrentUser, selectPreferences } from '../../utilities/rootSelectors';
import { createSelector } from 'reselect';

const CourseAuthoring = '/api/v2/courseAuthoring';

export const headerPageSelector = createSelector(
  [
    selectPreferences,
    selectCurrentUser,
    selectCourse,
    selectPageContent,
    selectMeekMode,
    tutorialSlice.selectShowManuallyPlay,
    tutorialSlice.selectShowManuallyPlayGlow,
  ],
  (
    settings,
    currentUser,
    course,
    contentPlayerContent,
    { showAppLevelFeatures },
    showManuallyPlayTutorialButton,
    showManuallyPlayGlow
  ) => {
    const { allowDirectMessaging, groupChatFeature, presenceChatFeature } = settings;

    const isAuthor = includes(
      currentUser.rights,
      'loi.authoring.security.right$AccessAuthoringAppRight'
    );
    const authoringUrl = !contentPlayerContent
      ? null
      : contentPlayerContent.id === Course.id
        ? `${CourseAuthoring}/section/${course.id}`
        : `${CourseAuthoring}/section/${course.id}/content/${contentPlayerContent.id}`;

    // Launching into a module or lesson will, in many ways, not work optimally. Limited nav should be possible.
    const isMeek = contentPlayerContent && contentPlayerContent.id === course.contentItemRoot;

    return {
      isStudent: currentUser.isStudent,
      isInstructor: currentUser.isInstructor,
      isAuthor: isAuthor,
      authoringUrl: authoringUrl,
      show: {
        pageHeader: !course.noHeader,
        breadCrumbs: true,
        search: !showAppLevelFeatures && appIsFramed,
        groupChat: groupChatFeature && !course.isEnded,
        presenceChat: presenceChatFeature && !course.isEnded,
        messageLink: allowDirectMessaging && !isMeek,
        studentNav: !isMeek,
        playlistNav:
          !isCourseWithRelationships(contentPlayerContent) &&
          contentPlayerContent.depth > 1 &&
          !isEmpty(contentPlayerContent.ancestors) &&
          !isMeek,
        tutorial: showManuallyPlayTutorialButton,
        tutorialGlow: showManuallyPlayGlow,
      },
    };
  }
);

export const selectCourseStatus = createSelector(
  [selectContentItems, selectPreferences],
  (contentItems, preferences) => {
    return {
      hasAssignments: find(contentItems, isAssignment),
      hasCompetencies: find(contentItems, c => Boolean(c.competencies?.length)),
      hasDiscussions: find(contentItems, isDiscussion),
      instructorResources: preferences.CBLPROD16934InstructorResources,
      instructorControlsV2: preferences.instructorControlsV2,
      enableInstructorLinkChecker: preferences.enableInstructorLinkChecker,
      progressReportPageEnabled: preferences.progressReportPageEnabled,
      instructorDashboardPageEnabled: preferences.instructorDashboardPageEnabled,
      enableContentSearch: preferences.enableContentSearch,
      enableInstructorFeedback: preferences.enableInstructorFeedback,
    };
  }
);
