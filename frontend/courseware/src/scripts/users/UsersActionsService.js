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

import { concat } from 'lodash';

import {
  makeSortActionCreator,
  makeVerbatimSearchActionCreator,
  makePagingActionCreator,
  makeSetPageSizeActionCreator,
  makeListLoadActionCreator,
} from '../list/makeListActionCreators.js';

import srs from '../utilities/srs.js';

import enrolledUserService from '../services/enrolledUserService.js';

export default angular
  .module('lo.users.UsersActionsService', [enrolledUserService.name])
  .service('UsersActionsService', [
    'enrolledUserService',
    function UsersActionsService(enrolledUserService) {
      const service = {};

      service.makeUserSearchActionCreators = (sliceName, loadingActionCreator) => ({
        SEARCH_BY_NAME: makeVerbatimSearchActionCreator(
          { sliceName },
          {
            properties: ['givenName', 'familyName', 'userName'],
            operator: 'or',
          },
          loadingActionCreator
        ),
      });

      service.makeUserSortActionCreators = (sliceName, loadingActionCreator) => ({
        SORT_GIVEN_NAME_ASC: makeSortActionCreator(
          { sliceName },
          {
            property: 'givenName',
            order: 'asc',
          },
          loadingActionCreator
        ),

        SORT_GIVEN_NAME_DESC: makeSortActionCreator(
          { sliceName },
          {
            property: 'givenName',
            order: 'desc',
          },
          loadingActionCreator
        ),

        SORT_FAMILY_NAME_ASC: makeSortActionCreator(
          { sliceName },
          {
            property: 'familyName',
            order: 'asc',
          },
          loadingActionCreator
        ),

        SORT_FAMILY_NAME_DESC: makeSortActionCreator(
          { sliceName },
          {
            property: 'familyName',
            order: 'desc',
          },
          loadingActionCreator
        ),
      });

      service.makeUserPagingActionCreator = (sliceName, loadingActionCreator) =>
        makePagingActionCreator({ sliceName }, loadingActionCreator);

      service.makeUserSetPageSizeActionCreator = (sliceName, loadingActionCreator) =>
        makeSetPageSizeActionCreator({ sliceName }, loadingActionCreator);

      service.createUserDataUpdateAction = usersData => ({
        type: 'DATA_LIST_UPDATE_MERGE',
        sliceName: 'users',
        data: {
          list: [...usersData],
        },
      });

      service.makeLearnerLoadActionCreator = (sliceName, isAppend = false, courseId) => {
        const loader = listOptions => {
          const query = srs.fromListOptions(listOptions);
          query.prefilters = concat(
            [['role.roleId', 'in', ['student', 'trialLearner']]],
            query.prefilters
          );
          return enrolledUserService.getUsers(courseId, query);
        };

        return makeListLoadActionCreator({ sliceName }, loader, isAppend, [
          service.createUserDataUpdateAction,
        ]);
      };

      return service;
    },
  ]);
