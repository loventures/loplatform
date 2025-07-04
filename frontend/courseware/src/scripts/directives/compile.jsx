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

import { forEach } from 'lodash';
import { processMathHtml, queueMathTypeset, wrapMath } from '../utilities/mathml.jsx';

export default angular
  .module('lo.directives')
  .directive('compile', [
    '$compile',
    '$rootScope',
    function ($compile, $rootScope) {
      return function (scope, element, attrs) {
        //TODO: If we replace scope.$watch with attrs.$observe, we may see a small
        //performance improvement.  However, we would need to call this directive
        //as <div compile="{{ html }}"> instead of <div compile="html">.

        scope.$watch(
          function (scope) {
            // watch the 'compile' expression for changes
            return scope.$eval(attrs.compile);
          },
          function recompile(val) {
            var value = val + '';

            if (!value) {
              return;
            }

            var html = '';

            // compile the new DOM and link it to the current
            // scope.
            // NOTE: we only compile .childNodes so that

            // we don't get into infinite loop compiling ourselves

            const doCompile = function () {
              try {
                return $compile(html);
              } catch (e) {
                $rootScope.$broadcast('CompileDirectiveFailed', {
                  type: 'HTML Content',
                  label: 'Failed to compile HTML content',
                  payload: {
                    html,
                  },
                });
                return $compile('<div></div>');
              }
            };

            element.empty();

            const hasMath = value.indexOf('<math') !== -1;
            const hasMathTex = value.indexOf('class="math-tex"') !== -1;

            if (hasMath) {
              value = wrapMath(value);
            }

            html = angular.element('<div>' + value + '</div>');

            forEach(html.find('.math-tex'), el => angular.element(el).attr('ng-non-bindable', ''));

            doCompile(html.html())(scope, function (el) {
              element.append(el);
              if (hasMath || hasMathTex) {
                processMathHtml(el[0]);
                queueMathTypeset(el[0]);
              }
            });
          }
        );
      };
    },
  ])
  .directive('bindMath', function () {
    return function (scope, element, attrs) {
      scope.$watch(
        function (scope) {
          // watch the 'bindMath' expression for changes
          return scope.$eval(attrs.bindMath);
        },
        function recompile(val) {
          var value = val + '';

          if (!value) {
            return;
          }

          var html = '';

          element.empty();

          const hasMath = value.indexOf('<math') !== -1;
          const hasMathTex = value.indexOf('class="math-tex"') !== -1;

          if (hasMath) {
            value = wrapMath(value);
          }

          html = angular.element('<div>' + value + '</div>');

          forEach(html.find('.math-tex'), el => angular.element(el).attr('ng-non-bindable', ''));

          element.append(html.html());
          if (hasMath || hasMathTex) {
            processMathHtml(element[0]);
            queueMathTypeset(element[0]);
          }
        }
      );
    };
  });
