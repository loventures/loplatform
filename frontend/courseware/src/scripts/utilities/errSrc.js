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

/**
 * @ngdoc directive
 * @alias errSrc
 * @memberOf lo.utilities
 * @description
 *  err-src directive that updates an img tag to use the err-src
 *  image if the ng-src image failed to load.
 **/
export default angular.module('lo.utilities.errSrc', []).directive('errSrc', function () {
  return {
    link: ($scope, element, attrs) => {
      element.bind('error', () => {
        let errSrc = window.lo_platform.cdn_url + attrs.errSrc;
        if (attrs.src != errSrc) {
          attrs.$set('src', errSrc);
        }
      });
    },
  };
});
