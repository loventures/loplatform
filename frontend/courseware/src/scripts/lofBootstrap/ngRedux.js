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

/*
Connecting+Redux+to+Angular
*/

import ngRedux from 'ng-redux';

export default angular.module('lof.bootstrap.ngRedux', [ngRedux]).config([
  '$provide',
  function ($provide) {
    $provide.decorator('$ngRedux', [
      '$delegate',
      function ($delegate) {
        $delegate.connectToScope = function (...args) {
          const binder = $delegate.connect(...args);

          return function (scope) {
            const unsub = binder(scope);

            scope.$on('$destroy', unsub);

            return function () {
              unsub();
            };
          };
        };

        $delegate.connectToCtrl = function (...args) {
          const binder = $delegate.connect(...args);

          return function (ctrl) {
            const unsub = binder(ctrl);

            ctrl.$onDestroy = (function (oldOnDestroy = angular.noop) {
              return function () {
                oldOnDestroy.apply(ctrl);
                unsub();
              };
            })(ctrl.$onDestroy);

            return function () {
              unsub();
            };
          };
        };

        return $delegate;
      },
    ]);
  },
]);
