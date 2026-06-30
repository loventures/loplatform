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

import { each } from 'lodash';
import Course from '../../bootstrap/course.ts';
import { loConfig } from '../../bootstrap/loConfig.ts';
import UrlBuilder from '../../utilities/UrlBuilder.js';

/** The Request object this service needs (only promiseRequest). */
export interface RequestLike {
  promiseRequest(url: any, method?: string, ...rest: any[]): PromiseLike<any>;
}

/** The lscache surface this service uses (the custom userLoad extension). */
export interface LsCacheLike {
  userLoad(url: any, params: any, method: any, lifespan: any, force?: any): PromiseLike<any>;
}

/** StepLoader (paged loader) surface. */
export interface StepLoaderLike {
  stepLoad(url: any): PromiseLike<any>;
}

/** UserModel surface (fromProfile maps an enrolled user record). */
export interface UserModelLike {
  fromProfile(enrolledUser: any): any;
}

const CACHE_LIFESPAN = 5; // minutes per CBLPROD-14575

/**
 * Enrolled-user lookups by role, migrated verbatim from the AngularJS
 * `enrolledUserService` to plain TS taking the injected `StepLoader`, `UserModel`,
 * `Request`, and the (Settings-extended) `lscache`.
 */
export const makeEnrolledUserService = (
  StepLoader: StepLoaderLike,
  UserModel: UserModelLike,
  Request: RequestLike,
  lscache: LsCacheLike
) => {
  function mapToUser(enrolledUser: any) {
    return UserModel.fromProfile(enrolledUser);
  }

  function mapToUsers(enrolledUsers: any) {
    //enrolledUsers contains server info
    //like offest, limit, and various counts
    each(enrolledUsers, function (user: any, index: any, users: any) {
      users[index] = mapToUser(user);
    });

    return enrolledUsers;
  }

  const enrolledUserService: any = {
    getUsersByRole: function (roles: any, filters: any, courseId: any, force: any) {
      if (!Array.isArray(roles)) {
        roles = [roles];
      }

      if (!roles.length || !roles[0]) {
        throw new Error('role id required!');
      }

      const url = new (UrlBuilder as any)(
        loConfig.enrollment.users,
        {
          contextId: courseId || Course.id,
        },
        filters
      );

      url.query.setPrefilter('role.roleId', 'in', roles.join(','));

      return lscache.userLoad(url, {}, null, CACHE_LIFESPAN, force).then(mapToUsers);
    },

    getStudents: function (filters: any, courseId: any, force: any) {
      return this.getUsersByRole(['student', 'trialLearner'], filters, courseId, force);
    },

    getAllStudents: function (courseId: any) {
      const url = new (UrlBuilder as any)(loConfig.enrollment.users, {
        contextId: courseId || Course.id,
      });
      url.query.setPrefilter('role.roleId', 'in', ['student', 'trialLearner']);

      return StepLoader.stepLoad(url);
    },

    getUser: function (userId: any, courseId: any, filters: any) {
      // You can add a 'roles' embed to this if you need it.
      const url = new (UrlBuilder as any)(
        loConfig.enrollment.user,
        {
          contextId: courseId || Course.id,
          userId: userId,
        },
        filters
      );
      return lscache.userLoad(url, {}, null, CACHE_LIFESPAN).then(mapToUser);
    },

    getUsers: function (courseId: any, filters: any) {
      // You can add a 'roles' embed to this if you need it.
      const url = new (UrlBuilder as any)(
        loConfig.enrollment.users,
        {
          contextId: courseId || Course.id,
        },
        filters
      );
      return lscache.userLoad(url, {}, null, CACHE_LIFESPAN).then(mapToUsers);
    },

    dropUsers: function (userIds: any, courseId: any) {
      const url = new (UrlBuilder as any)(loConfig.enrollment.drop, {
        contextId: courseId || Course.id,
        userId: userIds,
      });
      return Request.promiseRequest(url, 'delete');
    },
  };

  return enrolledUserService;
};

export type EnrolledUserService = ReturnType<typeof makeEnrolledUserService>;
