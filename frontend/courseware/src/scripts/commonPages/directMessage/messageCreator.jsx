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

import messaging from '../../messaging';

const component = {
  bindings: {
    recipients: '<',
    entireClass: '<',
  },
  template: `
    <full-message-creator
      recipients="$ctrl.recipients"
      entire-class="$ctrl.entireClass">
    </full-message-creator>
  `,
};

export let FullMessageCreator = 'FullMessageCreator: ng module not found';

export default angular
  .module('ple.pages.sendMessage.messageCreator', [messaging.name])
  .component('fullMessageCreatorReact', component)
  .run([
    '$injector',
    function ($injector) {
      FullMessageCreator = angular2react('fullMessageCreatorReact', component, $injector);
    },
  ]);
