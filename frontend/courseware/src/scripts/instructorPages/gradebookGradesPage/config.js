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

export const moduleConfig = {
  sliceName: 'gradebookGradesTableListState',
};

// Complete garbage because Course LW store is complete garbage. No other
// way to thread options into the actual API call.
export const searchByProps = inactive => ({
  SEARCH_BY_USER_ALL: {
    properties: ['user.all'],
    operator: 'or',
    options: { inactive },
  },
});

export const sortByProps = {
  SORT_GIVEN_NAME_ASC: {
    property: 'givenName',
    order: 'asc',
  },
  SORT_GIVEN_NAME_DESC: {
    property: 'givenName',
    order: 'desc',
  },
  SORT_FAMILY_NAME_ASC: {
    property: 'familyName',
    order: 'asc',
  },
  SORT_FAMILY_NAME_DESC: {
    property: 'familyName',
    order: 'desc',
  },
};
