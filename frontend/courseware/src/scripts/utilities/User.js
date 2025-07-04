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

import { keys, map, extend, pick, each } from 'lodash';

import Roles from './Roles.js';
/**
 * @ngdoc service
 * @name lo.utilities.UserProvider
 *
 * @description  Performs several User Operations
 */
export default angular
  .module('lo.utilities.User', [Roles.name])
  .provider(
    'User',
    /**
     * @ngdoc provider
     * @alias UserProvider
     * @memberof lo.utilities
     */
    function UserProvider() {
      /** @alias UserProvider **/
      var uP = this;

      var emptyUser = {
        _type: 'user',
        imageUrl: null,
        middleName: '',
        familyName: '',
        fullName: '',
        externalId: '',
        thumbnailId: '',
        emailAddress: '',
        userName: '',
        givenName: '',
        userState: '',
        title: null,
        id: 0,
        handle: '',
      };

      var userKeys = keys(emptyUser);

      /**
       * @ngdoc type
       * @memberof lo.utilities
       * @description UserClass is the internal constructor used by the UserProvider
       *   to represent an instance of a logged-in user.  It is currently not possible
       *   to create instances of this class directly, except indirectly via the
       *   UserProvider.init() and UserClass.switch() methods.
       *
       * @param {string} roles User Roles
       * @param {object} user User properties
       */
      var UserClass = function (roles, user) {
        if (!roles && user.roles) {
          roles = user.roles;
        }
        roles = angular.isArray(roles) ? roles : [roles];
        roles = map(roles, function (r) {
          if (angular.isObject(r)) {
            r = r.name;
          }
          return angular.isString(r) ? r.toLowerCase() : r;
        });
        this.roles = roles;
        extend(this, emptyUser, pick(user, userKeys));

        // Prevent changes.  To be safe.
        freeze(this);
      };

      uP.actualUser = null;
      uP.currentUser = null;
      uP.isActual = true; //TODO: make this a bit more robust...

      uP.onUserChange = () => {};

      /**
       * @description  DEPRECATED Initalize user service
       * Kept for backward-compatibility.  Use UserProvider.initUser instead.
       *
       * @param {string} role User Role
       * @param {string} userName User name
       * @param {string} givenName Real first name
       * @param {boolean} isPreview Is the user an instructor previewing the app as a student?
       * @param {number} id  userId
       */
      uP.init = function (role, userName, givenName, isPreview, id) {
        var userObj = {
          userName: userName,
          givenName: givenName,
          id: id,
        };

        return uP.initUser(role, userObj, isPreview);
      };

      var hasInitRunYet = false;
      var initListeners = [];
      uP.onInit = function (listener) {
        initListeners.push(listener);
        if (hasInitRunYet) {
          listener.call(uP, uP);
        }
      };

      var runInitListeners = function () {
        hasInitRunYet = true;
        each(initListeners, l => l.call(uP, uP));
      };

      /**
       * @description  Initalize user service
       *
       * @param {string} role User Role
       * @param {object} user User properties (aka a UserComponent)
       * @param {boolean} isPreview Is the user an instructor previewing the app as a student?
       */
      uP.initUser = function (role, user) {
        uP.actualUser = new UserClass(role, user);
        uP.currentUser = new UserClass(role, user);

        // Prevent changes from ever being made to "actual" user.
        if (Object.defineProperty) {
          defineProperty(this, 'actualUser', {
            configurable: false,
            writable: false,
          });
        }
        if (Object.seal) {
          Object.seal(uP.actualUser);
        }

        runInitListeners();

        return uP.actualUser;
      };

      UserClass.prototype.reInitUser = function () {
        uP.initUser.apply(uP, arguments);
      };

      /**
       * @description returns userId of the Object
       * @returns {Object} userData.user
       */
      UserClass.prototype.getId = function () {
        if (this.id) {
          return this.id; //So that people cannot tweak userId after runtime.
        }
        return this.user;
      };

      /**
       * @description returns handle of the Object
       * @returns {Object} userData.user
       */
      UserClass.prototype.getHandle = function () {
        if (this.handle) {
          return this.handle; //So that people cannot tweak userId after runtime.
        }
        return this.user;
      };

      UserClass.prototype.getExternalId = function () {
        return this.externalId;
      };

      /**
       * @description returns roles of the user
       * @returns {string} teh roles
       */
      UserClass.prototype.getRoles = function () {
        return this.roles;
      };

      /**
       * @description returns the user's full name
       * @returns {string} user's full name
       */
      UserClass.prototype.getName = function () {
        if (this.fullName) {
          return this.fullName;
        } else {
          return this.givenName + ' ' + this.familyName;
        }
      };

      /**
       * @description Checks whether the user is student or not
       * @returns {Boolean} true, false
       */
      UserClass.prototype.isStudent = function () {
        return this.roles.indexOf('student') !== -1;
      };

      /**
       * @description Checks whether the user is instructor or not
       * @returns {Boolean} true, false
       */
      UserClass.prototype.isInstructor = function () {
        return this.roles.indexOf('instructor') !== -1;
      };

      UserClass.prototype.showActivity = function () {
        return this.isStudent();
      };

      UserClass.prototype.recordActivity = function () {
        return this.isActual() && this.isStudent();
      };

      /**
       * @description Checks whether the user is in "student preview" mode or not
       * @returns {Boolean} true, false
       */
      UserClass.prototype.isPreview = function () {
        return this.current().id !== this.actual().id;
      };

      /**
       * @description Checks if role is granted
       * @returns {Boolean} true, false
       */
      UserClass.prototype.isUnknown = function () {
        return !this.isStudent() && !this.isInstructor();
      };

      /**
       * @description Returns a `User` object for the application's "effective"
       *   user.  This should always be the same thing as the DI-injectable
       *   `User` service, so it's not particularly useful.
       * @returns {Boolean} true, false
       */
      UserClass.prototype.current = function () {
        return uP.currentUser;
      };

      /**
       * @description Returns a `User` object for the user who is actually
       *   logged into the application.  This is useful when an instructor
       *   is viewing the application as a student, but we still need to
       *   provide a small set of functionality that is actually specific
       *   to the instructor (logout buttons; "Exit Preview" button, etc).
       *   When in doubt, don't use this.
       * @returns {Boolean} true, false
       */
      UserClass.prototype.actual = function () {
        return uP.actualUser;
      };

      UserClass.prototype.isActual = function () {
        return !!uP.isActual;
      };

      /**
       * @description Starts previewing the application as another user.
       *    Takes the same parameters as the UserClass constructor.
       *    You probably want to do a $state.reload() after running this.
       * @returns {object} User class
       */
      UserClass.prototype['switch'] = function () {
        // IE8
        unfreeze(uP.currentUser);
        UserClass.apply(uP.currentUser, arguments);
        freeze(uP.currentUser);
        uP.isActual = false;
        uP.onUserChange(uP.currentUser);
        return uP.currentUser;
      };

      UserClass.prototype.switchBack = function () {
        unfreeze(uP.currentUser);
        extend(uP.currentUser, emptyUser, uP.actualUser);
        freeze(uP.currentUser);
        uP.onUserChange(uP.currentUser);
        return uP.currentUser;
      };

      UserClass.prototype.setOnUserChange = function (fn) {
        uP.onUserChange = fn;
      };

      // Some fancy ES5 stuff to prevent us from modifying user objects
      // after they've been insantiated.
      // IE8 users continue to live dangerously.
      function freeze(obj, unfreeze) {
        if (!Object.defineProperty || !Object.getOwnPropertyNames) {
          return obj;
        }
        Object.getOwnPropertyNames(obj).forEach(function (prop) {
          defineProperty(obj, prop, {
            writable: !!unfreeze,
          });
        });
        return obj;
      }
      function unfreeze(obj) {
        freeze(obj, true);
      }

      // Wrapper for Object.defineProperty, because the method exists on IE8, but is
      // hopelessly broken.
      function defineProperty(obj, prop, desc) {
        try {
          Object.defineProperty(obj, prop, desc);
        } catch (e) {
          console.warn('Object.defineProperty unsupported or failed', e);
        }
      }

      uP.$get = [
        '$q',
        'Roles',
        function ($q, Roles) {
          /*
            Unfortunately, we don't have the information
            of a users specific roles other than the actual user
            fortunately, this works as we will only ever view as a student
        */
          UserClass.prototype.isStrictlyInstructor = function () {
            return !this.isPreview() && Roles.isStrictlyInstructor();
          };

          UserClass.prototype.setOnUserChange = function (fn) {
            uP.onUserChange = fn;
          };

          UserClass.prototype.setReady = function (readiness) {
            if (!uP.readinessDefer) {
              uP.readinessDefer = $q.defer();
            }

            if (readiness) {
              uP.readinessDefer.resolve();
            } else {
              uP.readinessDefer = $q.defer();
            }
          };

          UserClass.prototype.ready = function () {
            if (!uP.readinessDefer) {
              uP.readinessDefer = $q.defer();
              uP.readinessDefer.resolve();
            }
            return uP.readinessDefer.promise;
          };

          return uP.currentUser;
        },
      ];

      // Export the UserClass constructor
      uP.UserClass = UserClass;
    }
  )
  .provider('UserClass', [
    'UserProvider',
    function (UserProvider) {
      this.$get = function () {
        return UserProvider.UserClass;
      };
    },
  ]);
