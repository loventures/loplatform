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

import { map } from 'lodash';

import fileListTmpl from './feedbackFileList.html';

export default angular.module('lo.feedback.feedbackFileList', []).directive('feedbackFileList', [
  'FileContainer',
  function (FileContainer) {
    return {
      restrict: 'E',
      template: fileListTmpl,
      scope: {
        files: '=?',
        rawFiles: '=?',
        feedbackManager: '=?',
        thumbnailSize: '=?',
        removeAction: '=?',
      },
      link(scope) {
        if (!scope.thumbnailSize) {
          if (scope.feedbackManager) {
            scope.thumbnailSize = scope.feedbackManager.thumbnailSizes[0];
          } else {
            scope.thumbnailSize = 'medium';
          }
        }

        scope.toggleFileRemoval = file => {
          scope.feedbackManager.toggleRemovalStaging(file);
        };

        scope.isStagedForRemoval = file => {
          return scope.feedbackManager.isStagedForRemoval(file);
        };

        if (scope.rawFiles) {
          scope.$watch(
            'rawFiles',
            files => (scope.files = map(files, file => new FileContainer(file)))
          );
        }
      },
    };
  },
]);

const component = {
  template: `
        <feedback-file-list
            raw-files="$ctrl.files"
        ></feedback-file-list>
    `,
  bindings: {
    files: '<',
  },
};

angular.module('lo.feedback.feedbackFileList').component('attachmentFileList', component);
