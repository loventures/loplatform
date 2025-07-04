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

import listSearchTriggered from './listSearchTriggered.html';

export default angular
  .module('lo.list.directives.searchTriggered', [])
  .component('listSearchTriggered', {
    template: listSearchTriggered,
    bindings: {
      activeSearchString: '<?',
      searchAction: '<',
      searchPlaceholder: '<',
      focusLost: '<?',
      focusOnInit: '<?',
    },
    controller: [
      '$scope',
      '$element',
      function ($scope, $element) {
        this.$onInit = () => {
          this.searchString = this.activeSearchString || '';

          this.events = {
            focus: 'setSearchInputToFocus',
          };

          $element.on('keypress', event => {
            if (event.which === 13) {
              this.search();
            }
          });
        };

        this.search = () => this.searchAction(this.searchString);

        this.clearSearch = () => {
          //since for triggered search the change is not communicated
          //to the outside until user clicks button,
          //do a local clear in case the outside model stay the same.
          this.searchString = '';
          this.searchAction(null);
        };

        this.$onChanges = ({ activeSearchString }) => {
          if (activeSearchString) {
            this.searchString = activeSearchString.currentValue;
          }
        };

        this.iconCancelClick = () => {
          this.clearSearch();
          $scope.$broadcast(this.events.focus);
        };
      },
    ],
  });
