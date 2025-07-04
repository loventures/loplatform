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

import cookies from 'browser-cookies';
import { clone, get, setWith } from 'lodash';
/*
  Stronger than value! that this will throw at where
  the assumption is made
*/
export const ensureNotNull = <T>(value: T | null): T => {
  if (value === null) {
    throw 'failed: asserting value to not be null';
  } else {
    return value;
  }
};

export const setPath = <TSrc extends Record<string, unknown>, TVal>(
  src: TSrc,
  path: string[],
  value: TVal
): TSrc => {
  return setWith(clone(src), path, value, src => {
    if (src === null) {
      return {};
    } else {
      return clone(src);
    }
  });
};

export const mapPath = <TSrc extends Record<string, unknown>, TVal>(
  src: TSrc,
  path: string[],
  mapper: (v: TVal) => TVal
): TSrc => {
  const existing = get(src, path);
  const newValue = mapper(existing);
  return setPath(src, path, newValue);
};

export const numericKeyBy = <A>(as: readonly A[], fa: (a: A) => number): Record<number, A> => {
  const dict: Record<number, A> = {};
  for (const a of as) {
    dict[fa(a)] = a;
  }
  return dict;
};

export const axiosJsonConfig = () =>
  ({
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF': cookies.get('CSRF') || 'true',
    },
  }) as const;
