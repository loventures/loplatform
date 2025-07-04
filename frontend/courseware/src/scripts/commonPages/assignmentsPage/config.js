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
  sliceName: 'learnerAssignmentsPage',
};

export const configWithUserId = userId => ({
  sliceName: 'learnerAssignmentsPage',
  userId,
});

export const searchByProps = {
  SEARCH_ASSIGNMENT_NAME: {
    properties: ['content.name'],
    operator: 'or',
  },
};

export const sortByProps = {
  SORT_ASSIGNMENT_DUEDATE_ASC: {
    property: 'content.dueDate',
    order: 'asc',
    transform: 'time',
  },
  SORT_ASSIGNMENT_PLAYLIST_ORDER_ASC: {
    property: 'content.learningPathIndex',
    order: 'asc',
  },
};
