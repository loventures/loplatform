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

import naturalCompare from 'natural-compare';
import { get } from 'lodash';

/**
 * Order a collection using natural sorting on a (possibly nested) property.
 *
 * Behaviour is preserved verbatim from the original `naturallyOrderBy` Angular
 * filter, including that it sorts the array in place and passes non-arrays
 * through unchanged.
 *
 * @example naturallyOrderBy(competencies, 'title')
 */
export const naturallyOrderBy = <T>(array: T[], prop: string): T[] =>
  Array.isArray(array) ? array.sort((a0, a1) => naturalCompare(get(a0, prop), get(a1, prop))) : array;
