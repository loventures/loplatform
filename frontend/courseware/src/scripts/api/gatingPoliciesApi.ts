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

import axios, { AxiosError } from 'axios';
import Course from '../bootstrap/course';

import { ContentId } from './contentsApi';

const contextId = Course.id;

export type GateOverrides = {
  perUser?: Record<string, ContentId[]>;
  overall?: ContentId[];
  assignment?: Record<ContentId, ContentId[]>;
};

export const getGatingOverrides = (): Promise<GateOverrides> =>
  axios.get<GateOverrides>(`/api/v2/lwc/${contextId}/gateOverrides`).then(({ data }) => data);

type UpdateStudentOverridesRequest = {
  content: ContentId[];
  enabled: boolean;
  userIds: number[];
};

export const updateStudentOverrides = (
  request: UpdateStudentOverridesRequest
): Promise<GateOverrides> =>
  axios
    .put<GateOverrides>(`/api/v2/lwc/${contextId}/gateOverrides`, request)
    .catch((e: AxiosError) => {
      throw e.message;
    })
    .then(({ data }) => data);

type UpdateActivityGateOverridesRequest = {
  content: ContentId;
  enabled: boolean;
  assignments: ContentId[];
};

export const updateActivityGateOverrides = (
  request: UpdateActivityGateOverridesRequest
): Promise<GateOverrides> =>
  axios
    .put<GateOverrides>(`/api/v2/lwc/${contextId}/gateOverrides`, request)
    .catch((e: AxiosError) => {
      throw e.message;
    })
    .then(({ data }) => data);
