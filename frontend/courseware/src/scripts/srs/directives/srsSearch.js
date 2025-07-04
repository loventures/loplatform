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

import { values, map, isEmpty, each, debounce } from 'lodash';

import srsSearch from './srsSearch.html';
import srsSearchTriggered from './srsSearchTriggered.html';

export default angular
  .module('lo.srs.directives.search', [])
  .directive('srsSearch', function () {
    return {
      restrict: 'EA',
      template: srsSearch,
      scope: {
        store: '=', //the srs resource store
        props: '=?', //props to search in
        placeholder: '@?', //alternate placeholder text (by default, uses props)
        inputId: '=?',
      },
      controller: 'SRSSearchController',
    };
  })
  .directive('srsSearchTriggered', function () {
    return {
      restrict: 'EA',
      template: srsSearchTriggered,
      scope: {
        store: '=', //the srs resource store
        props: '=?', //props to search in
      },
      link: function (scope, element) {
        element.on('keypress', function (event) {
          if (event.which === 13) {
            scope.search();
          }
        });
      },
      controller: 'SRSSearchController',
    };
  })
  .controller('SRSSearchController', [
    '$scope',
    '$translate',
    function ($scope, $translate) {
      if (!$scope.store) {
        throw 'No store to search with';
      }

      $scope.searchField = '';

      $scope.props = $scope.props || $scope.store.searchByProps;

      if (!Array.isArray($scope.props)) {
        $scope.propKeys = Object.keys($scope.props);
        $scope.propValues = values($scope.props);
      } else {
        $scope.propKeys = $scope.props;
        $scope.propValues = $scope.props;
      }

      $scope.propKeys = map($scope.propKeys, function (key) {
        return 'SEARCH_' + key;
      });

      if ($scope.placeholder) {
        $translate($scope.placeholder).then(function (translatedPlaceholder) {
          $scope.searchPlaceholder = translatedPlaceholder;
        });
      } else {
        $translate($scope.propKeys).then(function (translatedProps) {
          $scope.searchPlaceholder = values(translatedProps).join(' or ');
        });
      }

      $scope.search = function () {
        if ($scope.store.search) {
          return $scope.store.search($scope.searchField, $scope.propValues);
        }

        //TODO below should be deprecated when we have Store as a standard object

        if (!$scope.store.load || !$scope.store.filters) {
          throw 'No filters/load function defined for the store';
        }

        return $scope.doSearch();
      };

      $scope.doSearch = function () {
        var unset = isEmpty($scope.searchField);

        if (unset) {
          $scope.store.filters.setFilterOp(null);
          each($scope.props, function (prop) {
            $scope.store.filters.removeFilter(prop);
          });
        } else {
          $scope.store.filters.setFilterOp('or');
          $scope.store.filters.setFilters(
            map($scope.props, function (prop) {
              return [prop, 'contains', $scope.searchField];
            })
          );
        }

        $scope.store.gotoPage(1);
      };

      $scope.searchAction = debounce($scope.search, 500);

      $scope.clearSearch = function () {
        $scope.searchField = '';
        $scope.search();
      };

      $scope.events = {
        iconCancelClicked: 'iconCancelClicked',
      };

      $scope.iconCancelClick = function () {
        $scope.clearSearch();
        $scope.$broadcast($scope.events.iconCancelClicked);
      };
    },
  ]);
