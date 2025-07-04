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
import Course from '../bootstrap/course';

export type UnknownData = Record<string, unknown>;

export type ScormData = {
  contentHeight: number | null;
  contentWidth: number | null;
  launchNewWindow: boolean;
  resourcePath: string;
  allRefs: Record<string, string>;
  apiData: UnknownData;
  sharedData: UnknownData;
};

export function buildScormUrl(commit: number, nodeName: string, resourcePath: string): string {
  return `/api/v2/authoring/${commit}/scorm.1/${nodeName}/serve/${resourcePath}`;
}

export function loadScormData(contentId: string): Promise<ScormData> {
  const contextId = Course.id;
  return axios
    .get<ScormData>(`/api/v2/lwc/${contextId}/scorm/${contentId}/apidata`)
    .then(({ data }) => {
      return data;
    });
}

export function setScormData(
  contentId: string,
  apiDataJson: UnknownData,
  sharedDataJson: UnknownData
): Promise<string> {
  const contextId = Course.id;
  return axios
    .post(`/api/v2/lwc/${contextId}/scorm/${contentId}/apidata`, {
      apiData: apiDataJson,
      sharedData: sharedDataJson,
    })
    .then(({ data }) => data);
}

export function submitScorm(
  raw: number,
  min: number,
  max: number,
  contentId: string
): Promise<string> {
  const contextId = Course.id;
  // loConfig.scorm.submit
  return axios
    .post(`/api/v2/lwc/${contextId}/scorm/${contentId}/submit`, {
      raw,
      min,
      max,
    })
    .then(({ data }) => data.lastManualSync);
}
