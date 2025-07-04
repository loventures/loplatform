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
import { UserInfo } from '../../loPlatform';
import Course from '../bootstrap/course';
import { DEFAULT_PAGE_SIZE } from '../components/PaginateWithMax';
import { isEmpty, map } from 'lodash';

import { ApiQueryResults } from './commonTypes';

const buildPageFilter = (pageIndex: number, limit: number) => {
  return `;offset=${pageIndex * limit};limit=${limit}`;
};

export type OrderDirection = 'asc' | 'desc';

const buildOrderFilter = (field: string, direction: OrderDirection = 'asc') => {
  return isEmpty(field) ? '' : `;order=${field}:${direction}`;
};

export type SearchCondition = 'in' | 'co';

export type SearchOp = 'or' | 'and';

const buildSearchFilter = (
  searchString: string,
  fields: string[],
  cond: SearchCondition = 'co',
  op: SearchOp = 'or'
) => {
  const encodedName = encodeURIComponent(searchString);
  return isEmpty(searchString)
    ? ''
    : fields.includes('user.all')
      ? `;filter=fullName:ts(${encodedName}),emailAddress:sw(${encodedName}),externalId:eq(${encodedName}),userName:eq(${encodedName});filterOp=${op}`
      : `;filter=` + map(fields, f => `${f}:${cond}(${encodedName})`).join(',') + `;filterOp=${op}`;
};

export function fetchStudents(
  searchString = '',
  searchFields = ['givenName', 'familyName'],
  searchCondition: SearchCondition = 'co',
  sortField = '',
  sortDirection: OrderDirection = 'asc',
  pageIndex = 0,
  pageSize = DEFAULT_PAGE_SIZE
): Promise<ApiQueryResults<UserInfo>> {
  const nameFilter = buildSearchFilter(searchString, searchFields, searchCondition);
  const pageFilter = buildPageFilter(pageIndex, pageSize);
  const orderFilter = buildOrderFilter(sortField, sortDirection);

  // loConfig.enrollment.users
  return axios
    .get<
      ApiQueryResults<UserInfo>
    >(`/api/v2/contexts/${Course.id}/roster;prefilter=role.roleId:in(student%2CtrialLearner)${nameFilter}${orderFilter}${pageFilter}`)
    .then(({ data }) => data);
}

export function fetchStudentCount(): Promise<number> {
  return fetchStudents(void 0, void 0, void 0, void 0, void 0, void 0, 0).then(
    result => result.totalCount
  );
}

export function searchStudentByName(name: string, limit = 25): Promise<ApiQueryResults<UserInfo>> {
  return fetchStudents(name, ['givenName', 'familyName'], 'co', void 0, void 0, void 0, limit);
}
