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

import lscache from 'lscache';
import { each } from 'lodash';

import StepLoader from '../utilities/StepLoader.js';
import Request from '../utilities/Request.js';

import userModal from '../users/User.js';
import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';
import Course from '../bootstrap/course.js';

/**
 * @ngdoc service
 * @alias enrolledUserService
 * @memberof lo.services
 * @description
 *   Returns users that are enrolled in a course based on role.
 */
export default angular
  .module('lo.services.enrolledUserService', [userModal.name, StepLoader.name, Request.name])
  .factory('enrolledUserService', [
    'StepLoader',
    'UserModel',
    'Request',
    function enrolledUserService(StepLoader, UserModel, Request) {
      var CACHE_LIFESPAN = 5; // minutes per CBLPROD-14575

      /** @alias enrolledUserService **/
      var enrolledUserService = {
        /**
         * @description get list of enrolled users by role
         * @returns {Promise} resolves when complete
         */
        getUsersByRole: function (roles, filters, courseId, force) {
          if (!Array.isArray(roles)) {
            roles = [roles];
          }

          if (!roles.length || !roles[0]) {
            throw new Error('role id required!');
          }

          var url = new UrlBuilder(
            loConfig.enrollment.users,
            {
              contextId: courseId || Course.id,
            },
            filters
          );

          url.query.setPrefilter('role.roleId', 'in', roles.join(','));

          return lscache.userLoad(url, {}, null, CACHE_LIFESPAN, force).then(mapToUsers);
        },

        /**
         * @description convenience method to get list of enrolled students
         * @returns {Promise} resolves when complete
         */
        getStudents: function (filters, courseId, force) {
          return this.getUsersByRole(['student', 'trialLearner'], filters, courseId, force);
        },

        getAllStudents: function (courseId) {
          var url = new UrlBuilder(loConfig.enrollment.users, {
            contextId: courseId || Course.id,
          });
          url.query.setPrefilter('role.roleId', 'in', ['student', 'trialLearner']);

          return StepLoader.stepLoad(url);
        },

        getUser: function (userId, courseId, filters) {
          // You can add a 'roles' embed to this if you need it.
          var url = new UrlBuilder(
            loConfig.enrollment.user,
            {
              contextId: courseId || Course.id,
              userId: userId,
            },
            filters
          );
          return lscache.userLoad(url, {}, null, CACHE_LIFESPAN).then(mapToUser);
        },

        getUsers: function (courseId, filters) {
          // You can add a 'roles' embed to this if you need it.
          var url = new UrlBuilder(
            loConfig.enrollment.users,
            {
              contextId: courseId || Course.id,
            },
            filters
          );
          return lscache.userLoad(url, {}, null, CACHE_LIFESPAN).then(mapToUsers);
        },

        dropUsers: function (userIds, courseId) {
          var url = new UrlBuilder(loConfig.enrollment.drop, {
            contextId: courseId || Course.id,
            userId: userIds,
          });
          return Request.promiseRequest(url, 'delete');
        },
      };

      function mapToUser(enrolledUser) {
        return UserModel.fromProfile(enrolledUser);
      }

      function mapToUsers(enrolledUsers) {
        //enrolledUsers contains server info
        //like offest, limit, and various counts
        each(enrolledUsers, function (user, index, users) {
          users[index] = mapToUser(user);
        });

        return enrolledUsers;
      }

      return enrolledUserService;
    },
  ]);
