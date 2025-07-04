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

import 'iframe-resizer/js/iframeResizer.contentWindow';

import iframeResizer from 'iframe-resizer/js/iframeResizer';

// WIP
// import '../../../../events/eventsBootstrap';

export default angular
  .module('lof.bootstrap.nonAngular', [])
  .run(function () {
    if (window.find) {
      window._find = window.find;
      window.find = function (...args) {
        console.error('You are using window.find, you probably forgot to import lodash find');
        window._find(...args);
      };
    }
  })
  .run([
    '$document',
    function ($document) {
      if (window.FastClick) {
        window.FastClick.attach($document[0].body);
      }
    },
  ])
  .run(function () {
    if (!String.prototype.endsWith) {
      String.prototype.endsWith = function (suffix) {
        return this.indexOf(suffix, this.length - suffix.length) !== -1;
      };
    }
  })
  .run(function () {
    if (!window.lo_platform.environment.isMock) {
      window.iFrameResize = iframeResizer;
    }
  });
