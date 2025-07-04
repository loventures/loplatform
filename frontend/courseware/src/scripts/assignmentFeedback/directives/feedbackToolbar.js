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

import browser from 'jquery.browser';
import feedbackToolbar from './feedbackToolbar.html';

class FeedbackToolbar {
  constructor($timeout, userMediaStreamService, Settings) {
    this.restrict = 'E';
    this.settings = Settings;
    this.template = feedbackToolbar;
    this.scope = {
      feedbackManager: '=',
      showHelp: '<',
    };

    this.$timeout = $timeout;
    this.userMediaStreamService = userMediaStreamService;

    this.compile = () => {
      return this.link.bind(this);
    };
  }

  checkAudioSupport() {
    return (
      !browser.unknown &&
      !browser.msie &&
      !browser.safari &&
      this.settings.isFeatureEnabled('AllowAudioRecording')
    );
  }

  link(scope, element) {
    scope.audioSupported = this.checkAudioSupport();
    scope.isMac = window.navigator.userAgent.toUpperCase().indexOf('MAC') != -1;

    scope.updateActiveTool = ($event, type) => {
      if (scope.activeFeedbackTool === type || (type === 'audio' && !scope.audioSupported)) {
        return;
      }
      scope.feedbackManager.updateActiveTool(type);
    };

    scope.onFileSelect = function (files) {
      if (files && files[0]) {
        scope.feedbackManager.addFile(files[0]);
      }
      scope.clearFileSelector();
    };

    //this is copied from loUpload
    scope.clearFileSelector = function () {
      //In Chrome/Safari when you finish selecting file
      //the filepath stays in the input element until you
      //choose something else, which prevent a change event
      //when you try to upload the same file immediately
      element.find('input[type="file"]').val('');
    };

    scope.$on('destroy', () => this.userMediaStreamService.releaseStream());
  }
}

angular.module('lo.feedback').directive('feedbackToolbar', [
  '$timeout',
  'userMediaStreamService',
  'Settings',
  function ($timeout, userMediaStreamService, Settings) {
    return new FeedbackToolbar($timeout, userMediaStreamService, Settings);
  },
]);
