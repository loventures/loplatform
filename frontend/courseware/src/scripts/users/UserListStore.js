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

import Course from '../bootstrap/course.js';
import { loConfig } from '../bootstrap/loConfig.js';
import { isFunction } from 'lodash';

import ResourceStore from '../srs/ResourceStore.js';
import UserContainer from './UserContainer.js';

/**
 * @ngdoc service
 * @alias UserListStore
 * @memberof lo.users
 * @param {number} courseId
 *     Course context
 * @param {String} role
 *     Roles allowed for this store. Defaults to ['student']
 * @augments ResourceStore
 * @description
 *      A UserListStore is a storage that connects to a resource API.
 *      The resource API is possibly paginated, filterable and sortable
 */
export default angular
  .module('lo.users.UserListStore', [ResourceStore.name, UserContainer.name])
  .factory('UserListStore', [
    'Roles',
    'ResourceStore',
    'UserContainer',
    function (Roles, ResourceStore, UserContainer) {
      /** @alias UserListStore **/
      var UserListStore = function (courseId, roles) {
        if (Roles.isInstructor()) {
          this.url = loConfig.enrollment.users; //Full user needed by, and only available to, instructors/admins
        } else {
          this.url = loConfig.cohort.users; //Slimmed down User Profile object, FERPA compliant
        }
        this.courseId = courseId || Course.id;
        this.roles = roles || ['student', 'trialLearner']; //Should not bail if you do not have roles.

        this.data = [];

        ResourceStore.call(this, this.url, {
          contextId: this.courseId,
        });
      };

      UserListStore.prototype = Object.create(ResourceStore.prototype);
      UserListStore.prototype.constructor = UserListStore;

      UserListStore.prototype.searchByProps = {
        GIVEN_NAME: 'givenName',
        FAMILY_NAME: 'familyName',
        USER_NAME: 'userName',
        EXTERNAL_ID: 'externalId',
        EMAIL_ADDRESS: 'emailAddress',
      };

      UserListStore.prototype.sortByProps = {
        GIVEN_NAME_ASC: {
          property: 'givenName',
          order: 1,
        },
        GIVEN_NAME_DESC: {
          property: 'givenName',
          order: -1,
        },
        FAMILY_NAME_ASC: {
          property: 'familyName',
          order: 1,
        },
        FAMILY_NAME_DESC: {
          property: 'familyName',
          order: -1,
        },
      };

      /**
       * @description
       *     Does a load using the current filter options
       * @returns {Promise}
       *     Resolves the list of items loaded
       */
      UserListStore.prototype.load = function () {
        this.filters.setPrefilters([['role.roleId', 'in', this.roles.join(',')]]);

        return ResourceStore.prototype.load.apply(this);
      };

      /**
       * @memberof UserListStore
       * @this UserListStore
       * @param str
       * @returns {*}
       */
      UserListStore.prototype.searchByName = function (str) {
        return this.search(str, this.searchByProps);
      };

      UserListStore.prototype.deserialize = function (user) {
        return new UserContainer(user);
      };

      UserListStore.prototype.create = function () {
        throw new Error('Not allowed');
      };

      UserListStore.prototype.update = function () {
        throw new Error('Not allowed');
      };

      UserListStore.prototype.remove = function () {
        throw new Error('Not allowed');
      };

      UserListStore.prototype.getName = function (item) {
        return item.getName();
      };
      //the following are ported over for compatibility
      //really shouldn't use these

      UserListStore.prototype.rowAction = function (row) {
        console.log('click on row', row);
        //TODO: UI route or something to student preview?
      };

      UserListStore.prototype.setRowTextFunction = function (func) {
        if (isFunction(func)) {
          console.warn('Overriding normal row text function.');
          this.getRowText = func;
        } else {
          console.error('Failed to override the row text function with ', func);
        }
      };

      UserListStore.prototype.getRowText = function (row) {
        return row.givenName + ' ' + row.familyName;
      };

      return UserListStore;
    },
  ]);
