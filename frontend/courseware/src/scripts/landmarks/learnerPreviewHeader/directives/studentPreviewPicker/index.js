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

import UsersActionsService from '../../../../users/UsersActionsService';

import { createListSelector } from '../../../../list/createListSelectors';
import basicList from '../../../../list/directives/basicList';
import pickerTemplate from './pickerTemplate.html';

const sliceName = 'StudentPreviewUserPicker';

const component = {
  template: pickerTemplate,

  bindings: {
    close: '<',
    dismiss: '<',
  },

  controller: [
    '$scope',
    '$ngRedux',
    'UsersActionsService',
    function ($scope, $ngRedux, UsersActionsService) {
      this.$onInit = function () {
        const selector = createListSelector(sliceName, 'users');

        const _loadAction = UsersActionsService.makeLearnerLoadActionCreator(sliceName);
        const dispatchableLoadAction = () => _loadAction(this.listState.options);

        const pageAction = UsersActionsService.makeUserPagingActionCreator(
          sliceName,
          dispatchableLoadAction
        );
        const sortActions = UsersActionsService.makeUserSortActionCreators(
          sliceName,
          dispatchableLoadAction
        );
        const searchActions = UsersActionsService.makeUserSearchActionCreators(
          sliceName,
          dispatchableLoadAction
        );

        $ngRedux.connectToCtrl(selector, { loadAction: _loadAction, pageAction })(this);
        $scope.$on('$destroy', $ngRedux.connect(null, sortActions)((this.sortActions = {})));
        $scope.$on('$destroy', $ngRedux.connect(null, searchActions)((this.searchActions = {})));
        this.loadAction(this.listState.options);
      };

      this.select = user => {
        this.close(user);
      };
    },
  ],
};

import { angular2react } from 'angular2react';

export let StudentPreviewPicker = 'StudentPreviewPicker: ng module not found';

export default angular
  .module('ple.directives.studentPreviewPicker', [UsersActionsService.name, basicList.name])
  .component('studentPreviewPicker', component)
  .run([
    '$injector',
    function ($injector) {
      StudentPreviewPicker = angular2react('studentPreviewPicker', component, $injector);
    },
  ]);
