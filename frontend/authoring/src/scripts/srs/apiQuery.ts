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

export type ApiQueryResults<T> = {
  count: number;
  offset?: number;
  limit?: number;
  filterCount?: number;
  totalCount?: number;
  objects: Array<T>;
};

const encodeValue = value => {
  return encodeURIComponent(value).replace(/\)/g, '%29');
};

export type MatrixFilter =
  | null
  | undefined
  | false
  | string
  | { property: string; operator?: string; value?: string | number | boolean };

const isFiltery = (f: MatrixFilter): f is Exclude<MatrixFilter, null | undefined | false> => !!f;

const encodeFilter = (v: MatrixFilter | MatrixFilter[]) =>
  (Array.isArray(v) ? v : [v])
    .filter(isFiltery)
    .map(o =>
      typeof o === 'string'
        ? o
        : o.property + (o.operator ? ':' + o.operator : '') + '(' + encodeValue(o.value ?? '') + ')'
    )
    .join(',');

export type MatrixOrder =
  | null
  | undefined
  | false
  | string
  | { property: string; direction: 'asc' | 'desc' | 'ascNullsFirst' | 'descNullsLast' };

const isOrdery = (o: MatrixOrder): o is Exclude<MatrixOrder, null | undefined | false> => !!o;

const encodeOrder = (v: MatrixOrder | MatrixOrder[]) =>
  (Array.isArray(v) ? v : [v])
    .filter(isOrdery)
    .map(o => (typeof o === 'string' ? o : o.property + ':' + o.direction))
    .join(',');

const matrixParamHandlers = {
  order: encodeOrder,
  filter: encodeFilter,
  prefilter: encodeFilter,
  embed: v => (Array.isArray(v) ? v.join(',') : v),
} as const;

export const encodeQuery = (matrix: {
  offset?: number;
  limit?: number;
  order?: MatrixOrder | MatrixOrder[];
  filter?: MatrixFilter | MatrixFilter[];
  prefilter?: MatrixFilter | MatrixFilter[];
  filterOp?: 'or';
}) => {
  return (
    ';' +
    Object.keys(matrix)
      .map(k => {
        const v = matrix[k];
        const h = matrixParamHandlers[k];
        const encoded = v != null && h ? h(v) : v;
        return encoded != null && encoded !== '' ? k + '=' + encoded : null;
      })
      .filter(m => m != null)
      .join(';')
  );
};
