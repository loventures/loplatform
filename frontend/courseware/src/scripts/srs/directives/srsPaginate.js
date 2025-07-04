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

import srsPaginate from './srsPaginate.html';
import pagination from 'angular-ui-bootstrap/src/pagination';
import uibPaginationMods from '../../vendor/uibPaginationMods.js';
import { DEFAULT_PAGE_SIZE } from '../../components/PaginateWithMax.js';

export default angular
  .module('lo.srs.directives.paginate', [pagination, uibPaginationMods.name])
  .directive('srsPaginate', function () {
    return {
      restrict: 'EA',
      template: srsPaginate,
      // replace: true,
      scope: {
        store: '=', //the srs resource store
      },
      controller: [
        '$scope',
        function ($scope) {
          if (!$scope.store) {
            throw 'No store to paginate with';
          }

          //Provided for tracking and updates via pagination
          $scope.store.currentPage = $scope.store.currentPage || 1;
          //Default page size
          $scope.store.pageSize = $scope.store.pageSize || DEFAULT_PAGE_SIZE;
          //Max amount of pagination pages before it uses ... (5 is a good size w/o wrap on smaller screens)
          $scope.store.maxSize = $scope.store.maxSize || 5;
          //Total count for the current query
          $scope.store.filterCount = $scope.store.filterCount || 0;

          $scope.updateMetadata = function (data) {
            data.totalCount && ($scope.store.totalCount = data.totalCount);
          };

          $scope.gotoPage = function (pageNumber) {
            if ($scope.store.gotoPage) {
              return $scope.store.gotoPage(pageNumber);
            }

            //TODO below should be deprecated when we have Store as a standard object

            if (!$scope.store.filters || !$scope.store.load) {
              throw 'No filters or load function defined';
            }

            $scope.store.filters.gotoPage(pageNumber - 1);
            $scope.store.load().then($scope.updateMetadata);
          };
        },
      ],
    };
  });
