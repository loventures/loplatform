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

import { isNumber, each } from 'lodash';
import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';

export default angular.module('lo.services.ProfileAPI', []).service('ProfileAPI', [
  '$http',
  '$timeout',
  '$q',
  function ProfileAPI($http, $timeout, $q) {
    const MAX_PROFILES = 32;

    //spam prevention
    const MAX_CONSECUTIVE_ERRORS = 5;

    //also spam prevention.
    //It is this low because we count on the fact that
    //when we need more than one profile
    //it is most likely to be added within a single event loop
    const FETCH_DEBOUNCE_DELAY = 1;

    const service = {
      pending: [],
      defers: {},
      fetchDebounce: null,
      consecutiveErrors: 0,
    };

    service.loadProfiles = handles => {
      const url = new UrlBuilder(
        loConfig.presence.profiles,
        {},
        {
          prefilter: ['handle', 'in', handles],
        }
      );

      return $http({ method: 'GET', url: url.toString() }).then(response => response.data.objects);
    };

    service.fetchProfiles = () => {
      if (service.consecutiveErrors > MAX_CONSECUTIVE_ERRORS) {
        console.error('error loading profiles with consecutive fails', service.consecutiveErrors);
        return;
      }

      if (!service.pending.length) {
        return;
      }

      const chunk = service.pending.splice(0, MAX_PROFILES);
      service
        .loadProfiles(chunk)
        .then(profiles => {
          each(profiles, profile => {
            if (service.defers[profile.handle]) {
              service.defers[profile.handle].resolve(service.formatProfile(profile));
            }
          });
        })
        .catch(() => (service.consecutiveErrors += 1))
        .finally(() => service.fetchProfiles());
    };

    service.formatProfile = data => {
      const imageUrl = isNumber(data.thumbnailId)
        ? new UrlBuilder(loConfig.presence.thumbnail, data).toString()
        : null;

      return {
        handle: data.handle,
        givenName: data.givenName,
        fullName: data.fullName,
        imageUrl,
        presenceLetter: (data.givenName || data.fullName || '?').charAt(0),
        presenceColour: 'hsl(' + ((data.id * 47) % 360) + ', 50%, 40%)',
        loaded: true,
      };
    };

    /**
     * @description get the profile of a for a given user.
     * @params {string} handle the user handle
     * @returns the user's profile. This is returned immediately, even if not yet available. Background requests will take care
     * of populating and updating the profile data and presence state.
     */
    service.getProfile = handle => {
      if (service.defers[handle]) {
        return service.defers[handle].promise;
      }

      service.defers[handle] = $q.defer();
      service.pending.push(handle);

      if (service.fetchDebounce) {
        $timeout.cancel(service.fetchDebounce);
      }

      service.fetchDebounce = $timeout(() => service.fetchProfiles(), FETCH_DEBOUNCE_DELAY);

      return service.defers[handle].promise;
    };

    return service;
  },
]);
