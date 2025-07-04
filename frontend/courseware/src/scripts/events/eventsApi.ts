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

import axios from 'axios';

export type ContextAlone = {
  context: number;
};

export type ContextWithEdgePath = {
  context: number;
  edgePath: string;
  assetId: number;
};

export type SceneId = ContextAlone | ContextWithEdgePath;

export type SceneInfo = SceneId[];

export type PresenceIn = {
  visible: boolean;
  millisSinceActive?: number;
  activeMillis?: number | null;
  inScenes?: SceneInfo;
  followScenes?: SceneInfo;
  lastEventId?: number | null;
};

export function fetchPresenceId(presenceIn: PresenceIn): Promise<string> {
  // loConfig.presence.sessions
  return axios.post('api/v2/presence/sessions', presenceIn).then(({ data }) => data.presenceId);
}
