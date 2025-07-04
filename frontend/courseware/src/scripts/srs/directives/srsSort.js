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

import srsSort from './srsSort.html';

export default angular.module('lo.srs.directives.sort', []).directive('srsSort', function () {
  return {
    restrict: 'EA',
    template: srsSort,
    // replace: true,
    scope: {
      store: '=', //the srs resource store
      props: '=?', //the property to sort by
    },
    controller: [
      '$scope',
      function ($scope) {
        if (!$scope.store) {
          throw 'No store to sort with';
        }

        $scope.props = $scope.props || $scope.store.sortByProps;

        /**
         * Sorts the store by the given property and ordering
         *
         * @param config {object} hash of form
         *  {
         *      property: PROPERTY_NAME,
         *      order: 1|-1|'asc'|'desc',
         *      startNew: true|false,
         *      clearAfter: true|false
         *  }
         * @return {promise} Resolves after sorting is complete
         */
        $scope.sortBy = function (config) {
          if ($scope.store.sort) {
            // store.sort takes an array of config objects
            return $scope.store.sort(true, false, config);
          }

          //TODO below should be deprecated when we have Store as a standard object
          {
            if (!$scope.store.load || !$scope.store.filters) {
              throw 'No filters/load function defined for the store';
            }

            $scope.store.filters.sort(config.property, config.order);
            $scope.store.load();
          }
        };
      },
    ],
  };
});
