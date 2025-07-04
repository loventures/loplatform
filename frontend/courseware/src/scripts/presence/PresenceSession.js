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
import dayjs from 'dayjs';
import { isEqual } from 'lodash';

export default angular.module('lo.presence.PresenceSession', []).service('PresenceSession', [
  'Settings',
  function PresenceService(Settings) {
    const VisibilityCacheKey = 'presence-visibility';

    const service = {
      visibility: Settings.getUserGlobal(
        VisibilityCacheKey,
        !Settings.isFeatureEnabled('AdminRight')
      ),
      inScenes: [{ context: Course.id }], // currently in these scenes
      inSceneChange: true, // have in scenes changed
      followScenes: [], // currently following these scenes
      followSceneChange: false, // have follow scenes changed
    };

    /**
     * Set the scenes of the session.
     * @param scenes an array of scene identifiers.
     * @return boolean whether a scene change occurred.
     */
    service.setScenes = scenes => {
      if (!isEqual(scenes, service.inScenes)) {
        service.inScenes = scenes;
        service.inSceneChange = true;
        return true;
      }
    };

    service.followScene = scene => {
      if (!service.followScenes.find(s => isEqual(s, scene))) {
        console.log('SVC FOLLOW: ', service.followScenes, scene);
        service.followScenes.push(scene);
        service.followSceneChange = true;
        return true;
      }
    };

    service.updateLastActive = () => {
      service.lastActive = dayjs().valueOf();
    };

    service.updateLastEventId = lastEventId => {
      service.lastEventId = lastEventId;
    };

    /**
     * Set whether your presence is visible to other users.
     * @param(boolean) visibility whether the user wishes their presence to be visible to others.  If no value is specified, toggles your visibility.
     * @return the resulting visibility
     */
    service.setVisibleToOthers = (visibility = !service.visibility) => {
      service.visibility = visibility;
      Settings.setUserGlobal(VisibilityCacheKey, service.visibility);
    };

    service.getSummary = () => {
      const millisSinceActive = dayjs().valueOf() - service.lastActive;

      const inScenes = service.inSceneChange ? service.inScenes : null;

      const followScenes = service.followSceneChange ? service.followScenes : null;

      return {
        visible: service.visibility,
        millisSinceActive,
        inScenes,
        followScenes,
        lastEventId: service.lastEventId,
      };
    };

    service.scenesUpdated = inScenes => {
      if (isEqual(inScenes, service.inScenes)) {
        service.inSceneChange = false;
        service.followSceneChange = false;
      }
      // else, the inScenes changed while the heartbeat was inflight and another heartbeat
      // is already chained on the inFlight promise, and we must heave inSceneChange true.
    };

    return service;
  },
]);
