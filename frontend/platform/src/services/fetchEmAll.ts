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

import { AxiosResponse } from 'axios';

import { SrsCollection } from '../srs';

/* Given a fetcher that accepts an offset and returns a standard SRS AQR,
 * will fetch all the pages of the data and return the result. */
export default <A = any>(
  fetcher: (offset: number) => Promise<AxiosResponse<SrsCollection<A>>>
): Promise<AxiosResponse<SrsCollection<A>>> => {
  return new Promise((resolve, reject) => {
    const fetch = (offset: number, previous: A[]) => {
      fetcher(offset)
        .then(res => {
          const data = res.data;
          const objects = previous.concat(data.objects);
          const end = (data.offset ?? 0) + data.count;
          if (end >= (data.filterCount ?? 0)) {
            resolve({ ...res, data: { ...data, offset: 0, count: objects.length, objects } });
          } else {
            fetch(end, objects);
          }
        })
        .catch(e => reject(e));
    };
    return fetch(0, []);
  });
};
