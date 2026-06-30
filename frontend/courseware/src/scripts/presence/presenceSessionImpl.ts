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

import dayjs from 'dayjs';
import { isEqual } from 'lodash';

import Course from '../bootstrap/course.ts';
import settings from '../utilities/settingsService';

/**
 * Pure-TS singleton port of the AngularJS `PresenceSession` service. Tracks the
 * scenes the user is in / following and visibility state, and produces the
 * heartbeat summary.
 *
 * `Settings` is still an AngularJS service, so it is reached lazily via
 * `lojector` (a single contained reach-in). Crucially, the original read its
 * visibility default at construction time; this singleton is imported eagerly,
 * so the default is read lazily on first access of `visibility` to ensure
 * `Settings` (and the lo bootstrap) are ready.
 */

const VisibilityCacheKey = 'presence-visibility';

const Settings = {
  get: () => settings,
};

export interface PresenceSessionSummary {
  visible: boolean;
  millisSinceActive: number;
  inScenes: any[] | null;
  followScenes: any[] | null;
  lastEventId: any;
}

class PresenceSessionImpl {
  inScenes: any[] = [{ context: Course.id }]; // currently in these scenes
  inSceneChange = true; // have in scenes changed
  followScenes: any[] = []; // currently following these scenes
  followSceneChange = false; // have follow scenes changed
  lastActive?: number;
  lastEventId?: any;

  private _visibility?: boolean;

  // The original read this at construction time. Defer to first access so that
  // `Settings` (an Angular service reached via lojector) is wired up first.
  get visibility(): boolean {
    if (this._visibility === undefined) {
      this._visibility = Settings.get().getUserGlobal(
        VisibilityCacheKey,
        !Settings.get().isFeatureEnabled('AdminRight')
      );
    }
    return this._visibility;
  }

  set visibility(v: boolean) {
    this._visibility = v;
  }

  /**
   * Set the scenes of the session.
   * @param scenes an array of scene identifiers.
   * @return boolean whether a scene change occurred.
   */
  setScenes = (scenes: any): boolean | undefined => {
    if (!isEqual(scenes, this.inScenes)) {
      this.inScenes = scenes;
      this.inSceneChange = true;
      return true;
    }
  };

  followScene = (scene: any): boolean | undefined => {
    if (!this.followScenes.find(s => isEqual(s, scene))) {
      console.log('SVC FOLLOW: ', this.followScenes, scene);
      this.followScenes.push(scene);
      this.followSceneChange = true;
      return true;
    }
  };

  updateLastActive = (): void => {
    this.lastActive = dayjs().valueOf();
  };

  updateLastEventId = (lastEventId: any): void => {
    this.lastEventId = lastEventId;
  };

  /**
   * Set whether your presence is visible to other users.
   * @param visibility whether the user wishes their presence to be visible to others.  If no value is specified, toggles your visibility.
   * @return the resulting visibility
   */
  setVisibleToOthers = (visibility: boolean = !this.visibility): void => {
    this.visibility = visibility;
    Settings.get().setUserGlobal(VisibilityCacheKey, this.visibility);
  };

  getSummary = (): PresenceSessionSummary => {
    const millisSinceActive = dayjs().valueOf() - (this.lastActive as number);

    const inScenes = this.inSceneChange ? this.inScenes : null;

    const followScenes = this.followSceneChange ? this.followScenes : null;

    return {
      visible: this.visibility,
      millisSinceActive,
      inScenes,
      followScenes,
      lastEventId: this.lastEventId,
    };
  };

  scenesUpdated = (inScenes: any): void => {
    if (isEqual(inScenes, this.inScenes)) {
      this.inSceneChange = false;
      this.followSceneChange = false;
    }
    // else, the inScenes changed while the heartbeat was inflight and another heartbeat
    // is already chained on the inFlight promise, and we must heave inSceneChange true.
  };
}

export type PresenceSession = PresenceSessionImpl;

export const presenceSession = new PresenceSessionImpl();
