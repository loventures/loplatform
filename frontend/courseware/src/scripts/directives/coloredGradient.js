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

import { min } from 'lodash';

const dashToCamel = string => string.replace(/-([a-z])/g, g => g[1].toUpperCase());

export default angular
  .module('lo.directives.coloredGradient', [])
  .value('coloredTextLinker', function (className, filter) {
    const asAttrsProp = dashToCamel(className);

    return function (scope, element, attrs) {
      var currentClasses = '';
      element.addClass(className);
      scope.$watch(attrs[asAttrsProp], function (value) {
        element.removeClass(currentClasses);
        currentClasses = 'done-' + min([100, filter(value, 'color')]); //caps at 'done-100'
        element.addClass(currentClasses);
      });
    };
  })
  .directive('coloredGradeBg', [
    'coloredTextLinker',
    'gradeFilter',
    function (coloredTextLinker, gradeFilter) {
      return {
        restrict: 'A',
        scope: false,
        link: coloredTextLinker('colored-grade-bg', gradeFilter),
      };
    },
  ])
  .directive('coloredGrade', [
    'coloredTextLinker',
    'gradeFilter',
    function (coloredTextLinker, gradeFilter) {
      return {
        restrict: 'A',
        scope: false,
        link: coloredTextLinker('colored-grade', gradeFilter),
      };
    },
  ])
  .directive('coloredProgressBg', [
    'coloredTextLinker',
    'gradeFilter',
    function (coloredTextLinker, progressFilter) {
      return {
        restrict: 'A',
        scope: false,
        link: coloredTextLinker('colored-progress-bg', progressFilter),
      };
    },
  ])
  .directive('coloredProgress', [
    'coloredTextLinker',
    'gradeFilter',
    function (coloredTextLinker, progressFilter) {
      return {
        restrict: 'A',
        scope: false,
        link: coloredTextLinker('colored-progress', progressFilter),
      };
    },
  ]);
