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

import uibAlert from 'angular-ui-bootstrap/src/alert';

import ToastActions from './ToastActions.js';
import { selectToastState } from './selector.js';

import toastContainerTemplate from './toastContainer.html';

const component = {
  template: toastContainerTemplate,

  controller: [
    '$scope',
    '$ngRedux',
    'ToastActions',
    function ($scope, $ngRedux, ToastActions) {
      this.$onInit = function () {
        $ngRedux.connectToCtrl(selectToastState, {
          dismissToast: ToastActions.dismissToastThunkActionCreator,
        })(this);
      };

      this.closeToast = toast => {
        this.dismissToast(toast.toastId);
      };
    },
  ],
};

import { angular2react } from 'angular2react';

export let ToastContainer = 'ToastContainer: ng module not found';

export default angular
  .module('lo.directives.toastContainer', [ToastActions.name, uibAlert])
  .component('toastContainer', component)
  .run([
    '$injector',
    function ($injector) {
      ToastContainer = angular2react('toastContainer', component, $injector);
    },
  ]);
