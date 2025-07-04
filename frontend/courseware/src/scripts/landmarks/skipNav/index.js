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

import template from './skipNav.html';

const component = {
  template,

  controller: function () {
    this.skipNav = function ($event) {
      //interferes with angular routing
      $event.stopPropagation();
      $event.preventDefault();

      //This is required for Chrome and FF because on a link
      //jump it will not force the focus.
      var el = angular.element('#maincontent');
      el.focus();
    };
  },
};

import { angular2react } from 'angular2react';

export let SkipNav = 'SkipNav: ng module not found';

export default angular
  .module('lo.landmarks.skipNav', [])
  .component('skipNav', component)
  .run([
    '$injector',
    function ($injector) {
      SkipNav = angular2react('skipNav', component, $injector);
    },
  ]);
