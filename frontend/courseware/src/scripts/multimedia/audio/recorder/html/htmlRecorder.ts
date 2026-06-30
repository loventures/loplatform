/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

declare const $: any;
declare const require: any;

//Do setup work here
const HtmlRecorder: any = function (this: any, uploadUrl: any, stream: any) {
  (window as any).URL =
    window.URL || (window as any).webkitURL || (window as any).mozURL || (window as any).msURL;

  (navigator as any).getUserMedia =
    (navigator as any).getUserMedia ||
    (navigator as any).webkitGetUserMedia ||
    (navigator as any).mozGetUserMedia ||
    (navigator as any).msGetUserMedia;
  this.recording = null;
  this.paused = false;
  this.uploadUrl = uploadUrl;
  this.stream = stream;
};

//Every page only need one AudioContext
window.AudioContext = window.AudioContext || (window as any).webkitAudioContext;
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
HtmlRecorder.prototype.initialize = function (this: any, cb: any) {
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
  // Vite has no webpack `require.ensure` code-splitting — use a dynamic import.
  // lo-recorder builds its worker inline (InlineWorker) and ignores workerPath,
  // so the old recorderWorker.js?file lookup is dropped.
  import('./lo-recorder.js').then(({ Recorder }) => {
    self.recorder = new Recorder(input, {});
    cb(null);
  });
};

HtmlRecorder.prototype.record = function (this: any) {
  this.paused = false;
  this.recorder.record();
};

HtmlRecorder.prototype.pause = function (this: any) {
  this.paused = true;
  this.recorder.stop();
};

HtmlRecorder.prototype.stop = function (this: any, cb: any) {
  var self = this;
  this.recorder.stop();
  //create WAV download link using audio data blob
  this.recorder.exportWAV(function (blob: any) {
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

HtmlRecorder.prototype.upload = function (this: any, callBack: any) {
  if (this.recording != null) {
    $.ajax({
      type: 'POST',
      url: this.uploadUrl,
      data: this.recording.data,
      processData: false,
      contentType: false,
    }).done(function (data: any) {
      callBack(null, data);
    });
  }
};

HtmlRecorder.prototype.destroyRecorder = function () {
  //console.log('htmlRecorder destroyRecorder');
};

HtmlRecorder.prototype.resume = function (this: any) {
  this.recorder.record();
};

HtmlRecorder.prototype.destroy = function (this: any) {
  this.stream.stop();
};

HtmlRecorder.prototype.clear = function (this: any) {
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
