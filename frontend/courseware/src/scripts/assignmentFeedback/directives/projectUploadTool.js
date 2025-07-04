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

import template from './projectUploadTool.html';

angular.module('lo.feedback').component('projectUploadTool', {
  template,
  bindings: {
    feedbackManager: '<',
  },
  controller: [
    '$element',
    function ($element) {
      this.onFileSelect = function (files) {
        if (files && files[0]) {
          this.feedbackManager.addFile(files[0]);
        }
        this.clearFileSelector();
      };

      this.clearFileSelector = function () {
        //In Chrome/Safari when you finish selecting file
        //the filepath stays in the input element until you
        //choose something else, which prevent a change event
        //when you try to upload the same file immediately
        $element.find('input[type="file"]').val('');
      };
    },
  ],
});
