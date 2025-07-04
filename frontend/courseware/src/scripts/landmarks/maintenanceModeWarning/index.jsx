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

import { angular2react } from 'angular2react';

import disconnectWarnings from './disconnectWarnings';

const component = {
  template: '<maintenance-mode-warning></<maintenance-mode-warning>',
};

export let MaintenanceModeWarning = 'MaintenanceModeWarning: ng module not found';

export default angular
  .module('lo.directives.maintenanceModeWarning', [disconnectWarnings.name])
  .component('maintenanceModeWarningReact', component)
  .run([
    '$injector',
    function ($injector) {
      MaintenanceModeWarning = angular2react('maintenanceModeWarningReact', component, $injector);
    },
  ]);
