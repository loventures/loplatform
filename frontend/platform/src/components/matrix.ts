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

type FilterItem = { property: string; operator?: string; value: string | number };
type OrderItem = { property: string; direction: string };

const encodeValue = (value: string | number): string => {
  return encodeURIComponent(value).replace(/\)/g, '%29');
};

const encodeFilter = (v: string | FilterItem | Array<string | FilterItem>): string =>
  (Array.isArray(v) ? v : [v])
    .map(o =>
      typeof o === 'string'
        ? o
        : o.property + (o.operator ? ':' + o.operator : '') + '(' + encodeValue(o.value) + ')'
    )
    .join(',');

const matrixParamHandlers: Record<string, (v: any) => string> = {
  order: (v: string | OrderItem | Array<string | OrderItem>) =>
    (Array.isArray(v) ? v : [v])
      .map(o => (typeof o === 'string' ? o : o.property + ':' + o.direction))
      .join(','),

  filter: encodeFilter,
  prefilter: encodeFilter,

  embed: (v: string | string[]) => (Array.isArray(v) ? v.join(',') : v),
};

const encodeQuery = (
  offset?: any,
  limit?: any,
  order?: any,
  filter?: any,
  prefilter?: any
): string => {
  const matrix: Record<string, any> =
    typeof offset === 'object'
      ? offset
      : { offset: offset, limit: limit, order: order, filter: filter, prefilter: prefilter };

  return Object.keys(matrix)
    .map(k => {
      const v = matrix[k];
      const h = matrixParamHandlers[k];

      const encoded = v !== null && h ? h(v) : v;

      return encoded !== null && encoded !== '' ? k + '=' + encoded : null;
    })
    .filter(m => m !== null)
    .join(';');
};

export default encodeQuery;
