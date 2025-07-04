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

import { get } from 'lodash';

const setPageLabel = (element, label) => {
  const pageEls = angular.element(element.find('a'));
  if (!pageEls || !pageEls[0]) {
    console.error('Could not find element, did Angular UI Bootstrap pagination change?', element);
    return;
  }
  const pageEl = angular.element(pageEls[0]);
  pageEl.attr('aria-label', label);
};

/**
 * @ngdoc overview
 * @description
 *  Enhances the angular ui-bootstrap pagination widget to be more accessible friendly by
 *  adding in screen-reader friendly labels
 **/
export default angular
  .module('lo.vendor.uib.paginationMods', [])
  .directive('paginationPage', [
    '$translate',
    function ($translate) {
      return {
        restrict: 'C',
        link: ($scope, $element) => {
          $scope.$watch(
            () => {
              const pageActive = get($scope, 'page.active', false);
              return pageActive === true;
            },
            isActive => {
              const pageNumber = get($scope, 'page.number', -1);
              if (isActive) {
                setPageLabel($element, $translate.instant('CURR_PAGE', { pageNumber }));
              } else {
                setPageLabel($element, $translate.instant('GOTO_PAGE', { pageNumber }));
              }
            }
          );
        },
      };
    },
  ])

  .directive('paginationPrev', [
    '$translate',
    function ($translate) {
      return {
        restrict: 'C',
        link: ($scope, $element) => setPageLabel($element, $translate.instant('PREV_PAGE')),
      };
    },
  ])

  .directive('paginationNext', [
    '$translate',
    function ($translate) {
      return {
        restrict: 'C',
        link: ($scope, $element) => setPageLabel($element, $translate.instant('NEXT_PAGE')),
      };
    },
  ]);
