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

import { each } from 'lodash';

export default angular
  .module('lo.multimedia.userMediaStreamService', [])
  .service('userMediaStreamService', [
    '$timeout',
    function ($timeout) {
      class UserMediaStreamService {
        constructor($timeout) {
          this.$timeout = $timeout;
          this.data = {
            stream: null,
            info: {
              audio: false,
              video: false,
            },
          };
          this.initialized = false;
          this.initializing = false;
        }
        getStream(includeVideo, cb) {
          console.log('initializing user media stream, getting video:', includeVideo);
          if (!this.initializing) {
            if (this.initialized) {
              this.$timeout(() => cb(null, this.data));
            } else {
              this.initializing = true;
              console.log('gettung user media:', window.navigator.getUserMedia);
              window.navigator.getUserMedia(
                { audio: true, video: includeVideo },
                stream => {
                  //console.log('successfully initialized Audio and Video recorder', stream);
                  this.data.info.audio = true;
                  this.data.info.video = true;
                  this.data.stream = stream;
                  this.initialized = true;
                  this.initializing = false;
                  this.$timeout(() => cb(null, this.data));
                },
                e => {
                  console.log(
                    'Could not initialize Audio and Video recorder, trying just audio...',
                    e
                  );
                  window.navigator.getUserMedia(
                    { audio: true },
                    stream => {
                      //console.log('successfully initialized just Audio recorder', stream);
                      this.data.info.audio = true;
                      this.data.info.video = false;
                      this.data.stream = stream;
                      this.initialized = true;
                      this.initializing = false;
                      this.$timeout(() => cb(null, this.data));
                    },
                    e => {
                      this.initializing = false;
                      this.$timeout(() => cb(e));
                    }
                  );
                }
              );
            }
          } else {
            //we need to wait until the one getUserMedia is done getting called.
            this.$timeout(() => this.getStream(includeVideo, cb), 100);
          }
        }
        reinitStream(includeVideo, cb) {
          //take out the stream,
          this.initialized = false;
          this.data = {
            stream: null,
            info: {
              audio: false,
              video: false,
            },
          };
          //then call get stream
          this.getStream(includeVideo, cb);
        }
        releaseStream(cb) {
          if (!this.initializing) {
            if (this.initialized) {
              each(this.data.stream.getTracks(), track => {
                track.stop();
              });

              this.initialized = false;
            }
          } else {
            this.$timeout(() => this.releaseStream(cb), 100);
          }
        }
        streamExists() {
          return this.data.stream !== null;
        }
      }

      return new UserMediaStreamService($timeout);
    },
  ]);
