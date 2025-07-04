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

import { Course, UserInfo } from '../../loPlatform';
import { CourseState } from '../loRedux';
import { ContentPrinterPageLink } from '../utils/pageLinks.ts';
import dayjs from 'dayjs';
import { includes, last, map } from 'lodash';
import qs from 'qs';
import { createSelector } from 'reselect';

export type CourseWithDetails = Course & { isEnded: boolean; name: string; courseName: string };

export const selectCourse = createSelector(
  (state: CourseState) => state.course,
  course => {
    // @ts-ignore
    const name = course.name || course.courseName;
    const isEnded = course.endDate && dayjs().isAfter(course.endDate);
    return {
      ...course,
      isEnded,
      name,
      courseName: name,
    } as CourseWithDetails;
  }
);

export const selectRouter = createSelector(
  (state: CourseState) => state.router,
  router => {
    const location = router.location;
    const searchParams = location.search ? qs.parse(location.search.slice(1)) : {};
    return {
      path: location.pathname as string,
      searchParams,
    };
  }
);

export const selectPrintView = (state: CourseState) =>
  !!ContentPrinterPageLink.match(selectRouter(state).path);

export const selectSettings = (state: CourseState) => state.settings;

export const selectPreferences = (state: CourseState) => state.preferences;

export const selectActualUserRights = createSelector(
  [(state: CourseState) => state.actualUserRights],
  rights => map(rights, right => last(right.split('.'))) as string[]
);

export type UserWithRoleInfo = UserInfo & {
  isPreviewing: boolean;
  isInstructor: boolean;
  isStudent: boolean;
  isUnderTrialAccess: boolean;
};

const withRoleInfo = (
  user: UserInfo,
  actualUserRights: string[],
  actualUser: UserInfo
): UserWithRoleInfo => {
  const isInstructor =
    user.id === actualUser.id &&
    (includes(user.roles, 'instructor') || includes(actualUserRights, 'TeachCourseRight'));
  return {
    ...user,
    isPreviewing: user.id !== actualUser.id,
    isInstructor: isInstructor,
    isStudent: !isInstructor,
    //This does not catch when instructor is viewing as a trial student
    //but per our spec instructor shouldn't know if a student is trial
    isUnderTrialAccess:
      user.id === actualUser.id &&
      includes(actualUserRights, 'TrialContentRight') &&
      !includes(actualUserRights, 'FullContentRight'),
  };
};

export const selectActualUser = createSelector(
  [state => state.actualUser, selectActualUserRights],
  (actualUser, actualUserRights: string[]) => withRoleInfo(actualUser, actualUserRights, actualUser)
);

export const selectActualUserId = createSelector(selectActualUser, user => user && user.id);

const userIdIsString = (id: any): id is string => {
  return typeof id === 'string';
};

export const selectPreviewAsUserId = createSelector(
  selectRouter,
  router => router.searchParams.previewAsUserId
);

export const selectForLearnerId = createSelector(
  selectRouter,
  router => router.searchParams.forLearnerId
);

export const selectPreviewAsUser = createSelector(
  selectPreviewAsUserId,
  (state: CourseState) => state.api.users,
  selectActualUser,
  selectActualUserRights,
  (previewAsUserId, users, actualUser, actualUserRights) => {
    if (userIdIsString(previewAsUserId)) {
      const user = users[previewAsUserId];
      return user && withRoleInfo(user, actualUserRights, actualUser);
    }
  }
);

export const selectForLearner = createSelector(
  selectForLearnerId,
  (state: CourseState) => state.api.users,
  selectActualUser,
  selectActualUserRights,
  (forLearnerId, users, actualUser, actualUserRights) => {
    if (typeof forLearnerId === 'string') {
      const user = users[forLearnerId];
      return user && withRoleInfo(user, actualUserRights, actualUser);
    }
  }
);

export const selectCurrentUser = createSelector(
  selectActualUser,
  selectPreviewAsUser,
  (actualUser, previewAsUser) => {
    return previewAsUser || actualUser;
  }
);

export const selectCurrentUserId = createSelector(
  selectPreviewAsUserId,
  selectActualUserId,
  (previewAsUserId, actualUserId) => {
    return `${previewAsUserId || actualUserId}`;
  }
);
