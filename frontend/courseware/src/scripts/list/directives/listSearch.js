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

import { keys, debounce } from 'lodash';

import listSearch from './listSearch.html';

export default angular.module('lo.list.directives.search', []).component('listSearch', {
  template: listSearch,
  bindings: {
    activeSearchString: '<?',
    searchActions: '<',
  },
  controller: [
    '$scope',
    function ($scope) {
      this.searchString = '';

      this.singleSearchAction = actionObj => {
        const actionKeys = keys(actionObj);

        if (actionKeys.length > 1) {
          console.warn('we only support 1 search action right now');
        }

        this.searchActionKey = actionKeys[0];
        this.searchAction = actionObj[this.searchActionKey];
      };

      this.$onChanges = ({ activeSearchString, searchActions }) => {
        if (activeSearchString) {
          this.searchString = activeSearchString.currentValue;
        }

        if (searchActions) {
          this.singleSearchAction(searchActions.currentValue);
        }
      };

      this.search = debounce(() => this.searchAction(this.searchString), 500);

      this.clearSearch = () => this.searchAction(null);

      this.events = {
        iconCancelClicked: 'iconCancelClicked',
      };

      this.iconCancelClick = function () {
        this.clearSearch();
        $scope.$broadcast(this.events.iconCancelClicked);
      };
    },
  ],
});
