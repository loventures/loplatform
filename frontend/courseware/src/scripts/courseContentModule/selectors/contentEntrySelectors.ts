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

import { UserInfo } from '../../../loPlatform';
import Course from '../../bootstrap/course.ts';
import { CourseState } from '../../loRedux';
import {
  ContentPlayerPageLink,
  ContentPrinterPageLink,
  InstructorAssignmentOverviewPageLink,
  LearnerCompetencyDetailPageLink,
  LearnerCompetencyPlayerPageLink,
} from '../../utils/pageLinks.ts';
import {
  ContentWithRelationships,
  ModuleWithRelationships,
  assembleContentView,
} from './assembleContentView.ts';
import { ViewingAs } from './contentEntry';
import { selectCompetencies } from '../../selectors/competencySelectors.ts';
import { selectCurrentUserOverallProgress } from '../../selectors/progressSelectors.ts';
import { selectContentItemsWithNebulousDetails } from '../../selectors/selectContentItemsWithNebulousDetails.ts';
import {
  CONTENT_TYPE_LESSON,
  CONTENT_TYPE_MODULE,
  CONTENT_TYPE_UNIT,
} from '../../utilities/contentTypes.ts';
import { createInstanceSelector } from '../../utilities/reduxify.ts';
import {
  selectActualUser,
  selectCourse,
  selectCurrentUser,
  selectRouter,
} from '../../utilities/rootSelectors.ts';
import { createSelector, createStructuredSelector } from 'reselect';

import { CourseWithRelationships, assembleCourseRelations } from './assembleCourseRelations.ts';

const contentIdSelectorFromProps = (_: CourseState, ownProps: any) => {
  if (ownProps?.content) {
    return ownProps.content.id;
  }
};

export const selectPageContentId = createSelector(
  selectRouter,
  selectCourse,
  contentIdSelectorFromProps,
  (router, course, contentId) => {
    const match =
      ContentPlayerPageLink.match(router.path) ||
      ContentPrinterPageLink.match(router.path) ||
      LearnerCompetencyPlayerPageLink.match(router.path) ||
      InstructorAssignmentOverviewPageLink.match(router.path);
    return contentId ?? match?.params.contentId ?? course.id;
  }
);

export const selectPageCompetencyId = createSelector(selectRouter, router => {
  const match = LearnerCompetencyDetailPageLink.match(router.path);
  return match && match.params.competencyId;
});

export const selectPageCompetency = createInstanceSelector(
  selectCompetencies,
  selectPageCompetencyId,
  null // NOTE: null is needed because ContentLink.tsx and potentially other places don't handle {}
);

export const selectCourseAsContent = createSelector(
  [selectCourse, selectCurrentUserOverallProgress],
  (course, overallProgress) => {
    return {
      ...course,
      id: course.id,
      displayIcon: overallProgress.isFullyCompleted ? 'icon-checkmark' : 'icon-books',
      progress: {
        title: course.name,
        ...overallProgress,
      },
      name: course.name,
    };
  }
);

export function isCourseWithRelationships(
  pageContent: ActiveContent
): pageContent is CourseWithRelationships {
  return pageContent.id === Course.id;
}

export function isUnitWithRelationships(
  pageContent: ActiveContent
): pageContent is ModuleWithRelationships /*ah fuckit*/ {
  return 'typeId' in pageContent && pageContent.typeId === CONTENT_TYPE_UNIT;
}

export function isModuleWithRelationships(
  pageContent: ActiveContent
): pageContent is ModuleWithRelationships {
  return 'typeId' in pageContent && pageContent.typeId === CONTENT_TYPE_MODULE;
}

export function isLessonWithRelationships(
  pageContent: ActiveContent
): pageContent is ModuleWithRelationships /*ah fuckit*/ {
  return 'typeId' in pageContent && pageContent.typeId === CONTENT_TYPE_LESSON;
}

export function isContentWithRelationships(
  pageContent: ActiveContent
): pageContent is ContentWithRelationships {
  return (
    !isCourseWithRelationships(pageContent) &&
    !isModuleWithRelationships(pageContent) &&
    !isUnitWithRelationships(pageContent)
  );
}

export const selectContentItemRelations = (state: CourseState) => state.api.contentRelations;

export type ActiveContent =
  | CourseWithRelationships
  | ModuleWithRelationships
  | ContentWithRelationships;

/**
 *
 * TODO: split this up appropriately. Producing one of three different shapes based on the
 *  type of asset is awful. This should be handled via routing.
 *
 *  */
export const selectPageContent = createSelector(
  [
    selectCourseAsContent,
    selectPageContentId,
    selectContentItemRelations,
    selectContentItemsWithNebulousDetails,
  ],
  (course, contentId, contentRelations, contentItems): ActiveContent => {
    const isCoursePage = contentId == course.id;
    if (isCoursePage) {
      return {
        ...course,
        ...assembleCourseRelations(contentItems, contentRelations),
      };
    } else {
      const rootItem = contentItems[contentId];
      if (!rootItem) {
        if (Object.keys(contentItems).length)
          console.error(`Content Item ${contentId} was not found in the store.`);
        return { id: contentId } as ContentWithRelationships;
      } else {
        return assembleContentView(rootItem, contentRelations, contentItems);
      }
    }
  }
);

/** Should the sidebar navigate to the page content. */
export const selectNavToPageContent = createSelector(
  [selectRouter],
  ({ path, searchParams: { nav } }): boolean =>
    !!ContentPlayerPageLink.match(path) && nav !== 'none'
);

export const selectContentPlayerComponent = createStructuredSelector<
  CourseState,
  {
    viewingAs: ViewingAs;
    actualUser: UserInfo;
    content: ActiveContent;
  }
>({
  content: selectPageContent,
  viewingAs: selectCurrentUser,
  actualUser: selectActualUser,
});

export const selectContent = createSelector(
  [selectPageContentId, selectContentItemRelations, selectContentItemsWithNebulousDetails],
  (
    contentId,
    contentRelations,
    contentItems
  ): ModuleWithRelationships | ContentWithRelationships => {
    const rootItem = contentItems[contentId];
    if (!rootItem) {
      if (Object.keys(contentItems).length)
        console.error(`Content Item ${contentId} was not found in the store.`);
      return { id: contentId } as ContentWithRelationships;
    } else {
      return assembleContentView(rootItem, contentRelations, contentItems);
    }
  }
);
