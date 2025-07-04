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
import { createUrl, loConfig } from '../bootstrap/loConfig';
import { axiosJsonConfig } from '../utils/utils';

export type TutorialInfo = {
  status: TutorialStatus;
};

export type TutorialStatus = 'Complete';

export function setTutorialStatus(
  tutName: string,
  status: TutorialStatus
): Promise<Record<string, TutorialInfo>> {
  return axios
    .put(
      createUrl(loConfig.tutorial.status, { name: tutName }),
      JSON.stringify(status),
      axiosJsonConfig()
    )
    .then(resp => resp.data);
}
