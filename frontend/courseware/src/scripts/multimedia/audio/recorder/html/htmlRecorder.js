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

// import $ from 'jquery';

//Do setup work here
var HtmlRecorder = function (uploadUrl, stream) {
  window.URL = window.URL || window.webkitURL || window.mozURL || window.msURL;

  navigator.getUserMedia =
    navigator.getUserMedia ||
    navigator.webkitGetUserMedia ||
    navigator.mozGetUserMedia ||
    navigator.msGetUserMedia;
  this.recording = null;
  this.paused = false;
  this.uploadUrl = uploadUrl;
  this.stream = stream;
};

//Every page only need one AudioContext
window.AudioContext = window.AudioContext || window.webkitAudioContext;
if (window.AudioContext) {
  // Only try to initialize if we have support for it.
  try {
    // this will only work with some action from the user.. TODO: nest in function?
    HtmlRecorder.audioContext = new window.AudioContext();
  } catch (e) {
    console.log('Audio recording unsupported');
  }
}

/**
 * This function should serve to initialize the recorder, and prompt the user to allow
 * their microphone to be shared, the callback being called when the user has done so.
 */
HtmlRecorder.prototype.initialize = function (cb) {
  var self = this;
  if (!HtmlRecorder.audioContext) {
    try {
      // this will only work with some action from the user.. TODO: nest in function?
      HtmlRecorder.audioContext = new window.AudioContext();
    } catch (e) {
      console.log('Audio recording unsupported');
    }
  }
  var input = HtmlRecorder.audioContext.createMediaStreamSource(this.stream);
  //uncomment this for audio loopback squeal!
  //input.connect(audio_context.destination);
  require.ensure(
    [],
    function (require) {
      const Recorder = require('./lo-recorder.js');
      const workerPath = require('./recorderWorker.js?file');

      self.recorder = new Recorder(input, {
        workerPath: workerPath,
      });
      cb(null);
    },
    'recorderjs'
  );
};

HtmlRecorder.prototype.record = function () {
  this.paused = false;
  this.recorder.record();
};

HtmlRecorder.prototype.pause = function () {
  this.paused = true;
  this.recorder.stop();
};

HtmlRecorder.prototype.stop = function (cb) {
  var self = this;
  this.recorder.stop();
  //create WAV download link using audio data blob
  this.recorder.exportWAV(function (blob) {
    var url = URL.createObjectURL(blob);
    //console.log('created url at: ' + url);
    self.recording = {
      url: url,
      name: new Date().toISOString() + '.wav',
      data: blob,
    };
    //console.log('generated recording: ' + self.recording);
    cb(self.recording);
  });
};

HtmlRecorder.prototype.upload = function (callBack) {
  if (this.recording != null) {
    $.ajax({
      type: 'POST',
      url: this.uploadUrl,
      data: this.recording.data,
      processData: false,
      contentType: false,
    }).done(function (data) {
      callBack(null, data);
    });
  }
};

HtmlRecorder.prototype.destroyRecorder = function () {
  //console.log('htmlRecorder destroyRecorder');
};

HtmlRecorder.prototype.resume = function () {
  this.recorder.record();
};

HtmlRecorder.prototype.destroy = function () {
  this.stream.stop();
};

HtmlRecorder.prototype.clear = function () {
  this.recorder.clear();
};

HtmlRecorder.prototype.finish = function () {
  //console.log('htmlRecorder finish');
};

HtmlRecorder.prototype.resetRecorder = function () {
  //console.log('htmlRecorder resetRecorder');
};

HtmlRecorder.prototype.finishRecording = function () {
  //console.log('htmlRecorder finishRecording');
};

HtmlRecorder.prototype.stopPlayback = function () {
  //console.log('htmlRecorder stopPlayback');
};

HtmlRecorder.prototype.previewRecording = function () {
  //console.log('htmlRecorder previewRecording');
};

export default HtmlRecorder;
