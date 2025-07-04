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

import multiSelectPicker from './multiSelectPicker.html';
import { each, identity, filter } from 'lodash';

export default angular
  .module('lo.directives.multiSelectPicker', [])
  .component('multiSelectPickerModal', {
    template: multiSelectPicker,
    bindings: {
      resolve: '<',
      close: '&',
      dismiss: '&',
    },
    controller: function () {
      this.selectionStatus = {};
      this.selection = {};

      this.$onInit = () => {
        each(this.resolve.selected, item => {
          this.selection[item.id] = item;
          this.selectionStatus[item.id] = true;
        });
      };

      this.select = function (item) {
        this.selection[item.id] = this.selectionStatus[item.id] ? item : null;
      };

      this.closeModal = function () {
        this.close({ $value: filter(this.selection, identity) });
      };
    },
  });
