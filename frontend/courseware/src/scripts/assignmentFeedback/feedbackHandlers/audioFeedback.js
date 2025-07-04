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

import tmpl from './audioFeedback.directive.html';

class AudioFeedbackDirective {
  constructor($timeout) {
    this.restrict = 'E';
    this.template = tmpl;
    this.$timeout = $timeout;
    this.scope = {
      feedbackManager: '=',
    };
    this.compile = () => {
      return this.link.bind(this);
    };
  }
  link(scope) {
    scope.recordingAccepted = recordingObject => {
      recordingObject.data.url = recordingObject.url;
      recordingObject.data.viewUrl = recordingObject.url;
      recordingObject.data.base64 = recordingObject.base64;
      scope.feedbackManager.addFile(recordingObject.data);
    };

    scope.recordingChanged = recording => {
      scope.feedbackManager.signalToolStatus(!!recording);
    };

    scope.recordingCancelled = () => {
      scope.feedbackManager.updateActiveTool(null);
    };
  }
}

angular.module('lo.feedback').directive('audioFeedback', [
  '$timeout',
  function AudioFeedbackDirectiveFactory($timeout) {
    return new AudioFeedbackDirective($timeout);
  },
]);
