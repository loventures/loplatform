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

import dropdown from 'angular-ui-bootstrap/src/dropdown';
import template from './loSelect.html';

export default angular.module('lof.directives.loSelect', [dropdown]).component('loSelect', {
  template: template,
  bindings: {
    selected: '<',
    //Currently the templates needs to knowing what
    //scope varialbes are available to them
    //we can change this to be a function that
    //takes the variable name as a parameter
    selectedTemplate: '<',
    options: '<',
    optionTemplate: '<',
    onSelect: '<',
  },
  controller: function () {},
});
