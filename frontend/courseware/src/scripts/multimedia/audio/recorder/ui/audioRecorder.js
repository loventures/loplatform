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

import dayjs from 'dayjs';
import { isFunction } from 'lodash';

import mediaRecorderResolver from '../DefaultRecorderResolver.js';
import Stopwatch from '../Stopwatch.js';
import recorderTmpl from './audioRecorder.directive.html';

export default angular
  .module('lo.multimedia.AudioRecorder', [Stopwatch.name, mediaRecorderResolver.name])
  .component('audioRecorder', {
    template: recorderTmpl,
    bindings: {
      onAccept: '<',
      onChange: '<',
      onCancel: '<',
    },
    controller: [
      '$element',
      '$sce',
      '$uibModal',
      '$timeout',
      'Stopwatch',
      'mediaRecorderResolver',
      function ($element, $sce, $uibModal, $timeout, Stopwatch, mediaRecorderResolver) {
        this.$onInit = () => {
          console.log('Attempting to resolve recorder...');
          this.ready = false;
          this.audioSupported = false;

          try {
            mediaRecorderResolver.resolveRecorder((err, recorder) => {
              if (err) {
                console.log(
                  "Could not resolve recorder, assuming browser doesn't support audio recording: ",
                  err
                );
                this.audioSupported = false;
                this.ready = true;
              } else {
                this.audioSupported = true;
                this.recorder = recorder;
                this.recorder.initialize(() => {
                  //TODO make HTMLRecorder register with angular to avoid this
                  $timeout(() => (this.ready = true));
                });
              }
            });

            this.timer = new Stopwatch(1000, timer => {
              this.recordingTimeElapsed = timer.timeElapsed();
            });

            this.isRecording = false;
          } catch (e) {
            console.error('Error during audio init', e);
            this.audioSupported = false;
            this.ready = true;
          }
        };

        this.$onDestroy = () => {
          mediaRecorderResolver.releaseStream();
        };

        this.startRecording = () => {
          if (this.isPaused) {
            this.recorder.resume();
            this.isPaused = false;
            this.isRecording = true;
            this.timer.start();
          } else {
            this.timer.reset();
            this.timer.start();
            this.isPaused = false;
            this.isRecording = true;
            this.recorder.record();
          }
        };
        this.playBase64 = base64 => {
          //cannot use angular for this
          //the base64 string for audio
          //is about 200000 characters long per second
          //which will blow up angular watch completely.
          $element.find('audio.recording').attr('src', base64);
        };
        this.getBase64 = (data, cb) => {
          var reader = new window.FileReader();
          reader.readAsDataURL(this.recording.data);
          reader.onload = function () {
            cb(reader.result);
          };
        };
        this.stopRecording = () => {
          this.uploading = true;
          this.timer.stop();
          this.recorder.stop(recording => {
            if (recording) {
              this.recording = recording;
              this.recording.data.fileName = this.recording.name;
              this.recording.data.mimeType = this.recording.data.type;
              this.recording.url = $sce.trustAsResourceUrl(recording.url);
              this.getBase64(this.recording.data, base64 => {
                this.recording.base64 = base64;
                this.playBase64(this.recording.base64);
                $timeout(() => {
                  this.onChange(this.recording);
                });
              });
            }
          });
          this.isPaused = false;
          this.isRecording = false;
        };
        this.pauseRecording = () => {
          this.timer.stop();
          this.isPaused = true;
          this.isRecording = false;
          this.recorder.pause();
        };
        this.canRecord = () => {
          return !this.isRecording;
        };
        this.canPause = () => {
          return this.isRecording;
        };
        this.canStop = () => {
          return this.isRecording || this.isPaused;
        };
        this.supportsPause = () => {
          return this.recorder && isFunction(this.recorder.pause);
        };
        this.canClear = () => {
          if (!this.supportsClear()) {
            return false;
          } else {
            return this.recording;
          }
        };
        this.supportsClear = () => {
          return isFunction(this.recorder.clear);
        };
        this.clear = () => {
          //clears the recording and gets everything ready to record again.
          console.log('clearing recording...');
          this.recorder.clear();
          this.timer.reset();
          this.recording = null;
          this.recordingTimeElapsed = dayjs.duration(0);
          this.onChange();
        };
        this.acceptRecording = () => {
          this.onAccept(this.recording);
        };
      },
    ],
  });
