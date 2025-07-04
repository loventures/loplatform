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

import textInputDecorator from './textInputDecorator.html';
/**
 * @ngdoc directive
 * @name lo.directives.textInputDecorator:textInputDecorator
 * @restrict E
 * @description add standard styling and behavior to text input fields to comply
 * with WCAG guidelines on usability.

 * If 'required' attribyte is set, will put an asterisk on the left side of the input field.
 * If 'error' attribute is set, will put red border on input field and a tool tip displaying the message
 * set in 'error'.  If 'error' attribute is null or empty will do no error styling.
 * This assumes to contents of this transclusion directive is nothing but the <input> tag, but it
 * can handle either a block or inline <input> tag.
 *
  * @example
    <example module="lo.directives">
      <file name="index.html">
         <div>
            <text-input-decorator error="{{errorMessage}}" required="true">
                <input type="text" placeholder="Enter text here..."/>
            </text-input-decorator>
         </div>
      </file>
    </example>

 */
angular.module('lo.directives').directive('textInputDecorator', function () {
  return {
    restrict: 'E',
    transclude: true,
    template: textInputDecorator,
    scope: {
      error: '@',
      required: '<',
    },
    controller: [
      '$scope',
      '$element',
      function ($scope, $element) {
        $scope.labelId = 'text-input-' + $scope.$id;
        $element.find('input').attr('id', $scope.labelId);
      },
    ],
  };
});
