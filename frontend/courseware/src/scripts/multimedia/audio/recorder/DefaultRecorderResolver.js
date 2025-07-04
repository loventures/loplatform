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

import { isFunction } from 'lodash';
import HtmlRecorder from './html/htmlRecorder.js';

import userMediaStreamService from './userMediaStreamService.js';

export default angular
  .module('lo.multimedia.DefaultRecorderResolver', [userMediaStreamService.name])
  .service('mediaRecorderResolver', [
    'userMediaStreamService',
    function (userMediaStreamService) {
      function resolveRecorder(cb) {
        //does the browser support html5 audio ?
        navigator.getUserMedia =
          navigator.getUserMedia ||
          navigator.webkitGetUserMedia ||
          navigator.mozGetUserMedia ||
          navigator.msGetUserMedia;
        var supportsHtmlAudio = isFunction(navigator.getUserMedia);
        console.log(
          'browser ' + (supportsHtmlAudio ? 'does support' : "doesn't support") + ' html audio'
        );

        if (supportsHtmlAudio) {
          userMediaStreamService.getStream(false, (err, data) => {
            if (!err) {
              cb(null, new HtmlRecorder(window.audioUploadUrl, data.stream));
            } else {
              console.error('could not initialize streamService due to: ', err);
              cb(new Error(err));
            }
          });
        } else {
          cb(new Error('html audio not supported'));
        }
      }

      function releaseStream() {
        userMediaStreamService.releaseStream();
      }

      return {
        resolveRecorder,
        releaseStream,
      };
    },
  ]);
