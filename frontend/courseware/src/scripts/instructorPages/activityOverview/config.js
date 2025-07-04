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

export const searchByProps = {
  SEARCH_BY_USER_ALL: {
    properties: ['user.all'],
    operator: 'or',
  },
};

export const sortByProps = {
  SORT_GIVEN_NAME_ASC: {
    property: 'learner.givenName',
    order: 'asc',
  },
  SORT_GIVEN_NAME_DESC: {
    property: 'learner.givenName',
    order: 'desc',
  },
  SORT_SCORE_PERCENT_ASC: {
    property: 'grade.pointsAwarded',
    order: 'asc',
  },
  SORT_SCORE_PERCENT_DESC: {
    property: 'grade.pointsAwarded',
    order: 'desc',
  },
  SORT_SUBMISSION_DATE: {
    property: 'mostRecentSubmission',
    transform: 'time',
    order: 'asc',
  },
};
