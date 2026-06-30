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

import { each, mapValues, keyBy, orderBy, debounce } from 'lodash';

import { currentUser } from '../utilities/currentUserData.ts';
import { presenceService } from './presenceServiceImpl.ts';
import { profileAPI } from '../services/profileAPI.ts';

/**
 * Pure-TS singleton port of the AngularJS `PresentUsers`. Monitors the users
 * actively present in the current course; registers for `ScenePresence` events.
 *
 * The current user's `handle` comes from the pure `currentUser` singleton
 * (utilities/currentUserData.ts), and `ProfileAPI` is the native `profileAPI` singleton (direct
 * import) — both off lojector now. `$q.when()` becomes `Promise.resolve()`.
 */

const ACTIVE_TIMEOUT = 60000;
const IDLE_TIMEOUT = 300000;

class PresentUsersImpl {
  // If we expose presence on more granular contexts than the course (e.g. on a particular
  // activity) then this will need to turn into a factory for presence by scene
  presentUsers: Record<string, any> = {};
  orderedPresentUsers: any[] = [];

  createDummyProfile = (handle: string): any => {
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
   * Internal: Handle receipt of scene presence information.
   */
  onScenePresence = (data: any): void => {
    const handleToTime: Record<string, any> = mapValues(keyBy(data.users, '0'), '1');

    each(this.presentUsers, existingUser => {
      if (!handleToTime[existingUser.handle]) {
        existingUser.presence = 'Offline';
      }
    });

    each(handleToTime, (millisSinceActive, handle) => {
      // exclude self
      if (handle === currentUser.handle) {
        return;
      }

      this.ensureProfile(handle);

      this.presentUsers[handle].presence =
        millisSinceActive < ACTIVE_TIMEOUT
          ? 'Active'
          : millisSinceActive < IDLE_TIMEOUT
            ? 'Idle'
            : 'Away';
    });

    this.reorder();
  };

  ensureProfile = (handle: string): PromiseLike<any> | undefined => {
    if (handle === currentUser.handle) {
      return Promise.resolve();
    }

    if (!this.presentUsers[handle]) {
      this.presentUsers[handle] = this.createDummyProfile(handle);
    }

    if (!this.presentUsers[handle].loaded) {
      return profileAPI
        .getProfile(handle)
        .then((profile: any) => {
          this.presentUsers[profile.handle] = {
            ...this.presentUsers[profile.handle],
            ...profile,
            loaded: true,
          };
          this.reorder();
        });
    }
  };

  reorder = debounce(() => {
    const ordered = orderBy(this.presentUsers, 'givenName');
    this.orderedPresentUsers.length = 0;
    this.orderedPresentUsers.push(...ordered);
  });

  constructor() {
    // Register to hear scene presence messages
    presenceService.on('ScenePresence', this.onScenePresence);
  }
}

export type PresentUsers = PresentUsersImpl;

export const presentUsers = new PresentUsersImpl();
