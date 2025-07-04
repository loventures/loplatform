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

import { useCourseSelector } from '../loRedux';
import { mapValues } from 'lodash';
import { assembleActivityView } from '../courseContentModule/selectors/assembleContentView.ts';
import { ContentWithNebulousDetails } from '../courseContentModule/selectors/contentEntry';
import { selectContentItemRelations } from '../courseContentModule/selectors/contentEntrySelectors.ts';
import { selectCurrentUserActivity, selectCurrentUserDueDateExempt } from './activitySelectors.ts';
import { selectContentItems } from './contentItemSelectors.ts';
import { discussionsSelector } from './discussionSelectors.ts';
import { selectCurrentUserDiscussionSummary } from './discussionSummarySelectors.ts';
import { selectCurrentUserGatingInformation } from './gatingInformationSelector.ts';
import { selectCurrentUserGrades } from './gradeSelectors.ts';
import { selectCurrentUserProgress } from './progressSelectors.ts';
import { getContentDisplayInfo } from '../utilities/contentDisplayInfo.ts';
import { createSelector } from 'reselect';

export const selectContentItemsWithNebulousDetails = createSelector(
  [
    selectContentItems,
    selectCurrentUserGrades,
    selectCurrentUserProgress,
    selectCurrentUserActivity,
    selectCurrentUserDueDateExempt,
    discussionsSelector,
    selectCurrentUserDiscussionSummary,
    selectCurrentUserGatingInformation,
  ],
  (
    contentItems,
    gradesByContent,
    progressByPath,
    activityByContent,
    dueDateExemptByContent,
    discussions,
    discussionSummaryByContent,
    gatingInformationByContent
  ) => {
    return mapValues(contentItems, content => {
      const activity = activityByContent[content.id] ?? ({} as any); // usage or creation of content.activity
      const availability = gatingInformationByContent[content.id] ?? ({} as any);
      const displayInfo = getContentDisplayInfo(content);

      const progress = {
        title: content.name,
        ...progressByPath[content.id],
      };
      /* I'm still skeptical this information really belongs up here. Might it not be correct to compute it further down like my original implementation */
      const displayIcon = progress.isFullyCompleted
        ? 'icon-checkmark'
        : content.typeId === 'lesson.1' &&
            progress.progressTypes &&
            progress.progressTypes.includes('TESTEDOUT')
          ? 'icon-trophy'
          : content.iconClass || displayInfo.displayIcon;

      const discussion = discussions[content.id];

      return {
        ...content,
        displayIcon,
        displayKey: displayInfo.displayKey,
        availability,
        grade: gradesByContent[content.id],
        progress,
        activity: {
          ...activity,
          discussion,
          discussionSummary: discussionSummaryByContent[content.id],
        },
        hasCompetencies: content.competencies.length > 0,
        dueDateExempt: dueDateExemptByContent[content.id],
        isClosedAsActivity:
          availability?.isReadOnly &&
          // currently only discussions can be closed
          discussion &&
          discussion.closed,
      } as ContentWithNebulousDetails;
    });
  }
);

export const useNebulousContentItem = (contentId: string | undefined) => {
  const contentItems = useCourseSelector(selectContentItemsWithNebulousDetails);
  return contentItems[contentId ?? '_none_'];
};

export const useContentItemWithRelations = (contentId: string | undefined) => {
  const relations = useCourseSelector(selectContentItemRelations);
  const contentItems = useCourseSelector(selectContentItemsWithNebulousDetails);
  const content = contentItems[contentId ?? '_none_'];
  return content ? assembleActivityView(content, relations, contentItems) : null;
};
