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

import { isEmpty, isFunction } from 'lodash';

import basicList from './basicList.html';

import listSearch from './listSearch.js';
import listSort from './listSort.js';
import listPaginate from './listPaginate.js';

export default angular
  .module('lo.list.directives.basicList', [listSearch.name, listSort.name, listPaginate.name])
  .component('basicList', {
    template: basicList,
    bindings: {
      state: '<',

      sortActions: '<?',
      searchActions: '<?',
      pageAction: '<?',
      loadAction: '<?',

      //also via transclude
      icon: '<?',
      listTitle: '<?',
      headerButton: '<?',
      emptyMsg: '<?',
      filteredMsg: '<?',
    },
    transclude: {
      listSlot: 'listSlot',
      headerSlot: '?headerSlot',
      emptyMsgSlot: '?emptyMsgSlot',
      filteredMsgSlot: '?filteredMsgSlot',
    },
    controller: function () {
      this.$onChanges = ({ sortActions, searchActions, pageAction }) => {
        if (sortActions) {
          this.hasSort = !isEmpty(sortActions.currentValue);
        }
        if (searchActions) {
          this.hasSearch = !isEmpty(searchActions.currentValue);
        }
        if (pageAction) {
          this.usePagination = isFunction(pageAction.currentValue);
        }
      };
    },
  });
