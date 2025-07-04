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

import { each, capitalize, extend, isNil } from 'lodash';

export default angular.module('lo.utilities.Roles', []).provider(
  'Roles',
  /**
   * @ngdoc provider
   * @memberof lo.utilities
   */
  function RolesProvider() {
    /**
     * @ngdoc service
     * @memberof lo.utilities
     *
     * @description  Performs several Roles Operations like telling you if you have a role
     * or making it so that we can determine the most likely role if you have many (student & teacher)
     */
    var Roles = {
      roles: {},
      indexPages: {},
    };

    /**
     * @description  Performs Constructor Role for Object
     *
     * @param {object} rolesArr The roles.
     */
    this.init = function (rolesArr, indexPages, features) {
      var name = '';
      each(rolesArr, function (r) {
        name = Roles.roleToFeature(r);

        Roles.roles[name] = true;
        if (features) {
          if (isNil(features[name])) {
            features[name] = { isEnabled: true, type: 'UserRole' };
          } else {
            console.warn('Not overriding feature for Role for: ', name, features[name]);
          }
        }
      });

      Roles.indexPages = indexPages;

      return Roles;
    };

    Roles.roleToFeature = function (role) {
      var path = role.split('.');
      var name = path[path.length - 1];
      return name;
    };

    Roles.roleTypeToFeature = function (roleType) {
      if (roleType === 'Student') {
        return 'isLearner';
      } else if (roleType === 'Faculty') {
        return 'isInstructor';
      } else {
        return 'is' + capitalize(roleType);
      }
    };

    Roles.init = this.init;

    /**
     * @param {String} roleName the name of the role to check.
     * @description Check if somebody had a role
     * @returns {boolean} true/false
     */
    Roles.hasRole = function (roleName) {
      return Roles.roles[roleName];
    };

    /**
     * @description Check if somebody had a role
     * @returns {boolean} true/false
     */
    Roles.isStudent = function () {
      return Roles.hasRole('LearnCourseRight'); //Note admins can have both
    };

    Roles.isReadOnly = function () {
      return Roles.hasRole('ReadCourseRight') && !Roles.hasRole('InteractCourseRight');
    };

    Roles.isUnderTrialAccess = function () {
      //FullContentRight supercedes Trial
      return Roles.hasRole('TrialContentRight') && !Roles.hasRole('FullContentRight');
    };

    /**
     * @description Check if somebody has the Instructor role.
     * @returns {boolean} true/false
     */
    Roles.isInstructor = function () {
      if (Roles.hasRole('TeachCourseRight')) return true;

      //TODO: Advisors and Instructors have ContentCourseRight right...
      //fix this up and don't break everything via TECH-712
      if (Roles.hasRole('ContentCourseRight') && !Roles.hasRole('LearnCourseRight')) return true;
    };

    //Temp fix till we refactor in TECH-712
    Roles.isStrictlyInstructor = function () {
      if (Roles.hasRole('TeachCourseRight')) return true;
      if (Roles.hasRole('EditCourseGradeRight') && !Roles.hasRole('LearnCourseRight')) return true;
    };

    /**
     * @description Check if somebody has the Advisor role.
     * @returns {boolean} true/false
     */
    Roles.isAdvisor = function () {
      if (Roles.hasRole('ViewCourseGradeRight') && !Roles.hasRole('EditCourseGradeRight'))
        return true;
    };

    /**
     * @description Check if you have the authoring role
     * @returns {boolean} true/false
     */
    Roles.isAdmin = function () {
      return false && Roles.hasRole('CourseRight'); //TODO: Make this supported
    };

    /**
     * @description What is the primary access right for this user?  Legacy suppoer
     */
    Roles.getPrimaryRole = function () {
      if (Roles.isAdmin()) {
        return 'administrator';
      } else if (Roles.isInstructor()) {
        return 'instructor';
      } else if (Roles.isStudent()) {
        return 'student';
      } else {
        return 'unknown';
      }
    };

    /**
     * @description what is the index page for the primary role for this user?
     * used for going back from previews
     */
    Roles.getPrimaryRoleIndex = function () {
      return Roles.indexPages[Roles.getPrimaryRole()];
    };

    this.$get = function () {
      /** @alias Roles **/
      var service = {};

      return extend(service, Roles);
    };

    Roles.$get = this.$get;

    return Roles;
  }
);
