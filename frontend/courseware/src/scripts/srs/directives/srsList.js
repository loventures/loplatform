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

import { isEmpty } from 'lodash';

import srsList from './srsList.html';
export default angular.module('lo.srs.directives.list', []).directive('srsList', function () {
  return {
    restrict: 'EA',
    template: srsList,
    scope: {
      store: '=',
      headerText: '=?',
      iconCls: '=?',
      emptyMsg: '=?',
      filteredMsg: '=?',
      //This doesn't work for all cases outside the dashboard,
      //but this list directive is deprecated
      //so it works good enough for now.
      emptyIsGood: '=?',
      headerButton: '=?',
    },
    transclude: true,
    controller: [
      '$scope',
      function ($scope) {
        $scope.title = $scope.headerText || $scope.store.title;
        $scope.iconCls = $scope.iconCls || $scope.store.iconCls;
        $scope.hasSort = !isEmpty($scope.store.sortByProps);
        $scope.hasSearch = !isEmpty($scope.store.searchByProps);
      },
    ],
  };
});
