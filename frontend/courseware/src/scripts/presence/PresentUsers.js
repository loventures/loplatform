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

import { each, mapValues, keyBy, orderBy, debounce } from 'lodash';

import User from '../utilities/User.js';

import ProfileAPI from '../services/ProfileAPI.js';

import PresenceService from './PresenceService.js';

export default angular
  .module('lo.presence.PresentUsers', [PresenceService.name, User.name, ProfileAPI.name])
  /**
   * @ngdoc service
   * @alias PresentUsers
   * @memberof lo.presence.PresentUsers
   * @description Service for monitoring the users who are actively present in the current course.
   */
  .service('PresentUsers', [
    'PresenceService',
    'ProfileAPI',
    'User',
    '$q',
    function (PresenceService, ProfileAPI, User, $q) {
      // If we expose presence on more granular contexts than the course (e.g. on a particular
      // activity) then this will need to turn into a factory for presence by scene

      const ACTIVE_TIMEOUT = 60000;
      const IDLE_TIMEOUT = 300000;

      const service = {
        presentUsers: {},
        orderedPresentUsers: [],
      };

      service.createDummyProfile = handle => {
        return {
          _type: 'profile',
          id: null,
          handle: handle,
          loaded: false,
          givenName: '???',
          fullName: '???',
          presenceLetter: '?',
          presenceColour: 'gray',
        };
      };

      /**
       * @description Internal: Handle receipt of scene presence information.
       * @params {Object} data the scene presence data
       */
      service.onScenePresence = data => {
        const handleToTime = mapValues(keyBy(data.users, '0'), '1');

        each(service.presentUsers, existingUser => {
          if (!handleToTime[existingUser.handle]) {
            existingUser.presence = 'Offline';
          }
        });

        each(handleToTime, (millisSinceActive, handle) => {
          //exclude self
          if (handle === User.handle) {
            return;
          }

          service.ensureProfile(handle);

          service.presentUsers[handle].presence =
            millisSinceActive < ACTIVE_TIMEOUT
              ? 'Active'
              : millisSinceActive < IDLE_TIMEOUT
                ? 'Idle'
                : 'Away';
        });

        service.reorder();
      };

      service.ensureProfile = handle => {
        if (handle === User.handle) {
          return $q.when();
        }

        if (!service.presentUsers[handle]) {
          service.presentUsers[handle] = service.createDummyProfile(handle);
        }

        if (!service.presentUsers[handle].loaded) {
          return ProfileAPI.getProfile(handle).then(profile => {
            service.presentUsers[profile.handle] = {
              ...service.presentUsers[profile.handle],
              ...profile,
              loaded: true,
            };
            service.reorder();
          });
        }
      };

      service.reorder = debounce(() => {
        const ordered = orderBy(service.presentUsers, 'givenName');
        service.orderedPresentUsers.length = 0;
        service.orderedPresentUsers.push(...ordered);
      });

      // Register to hear scene presence messages
      PresenceService.on('ScenePresence', service.onScenePresence);

      return service;
    },
  ]);
