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

import listTemplate from './questionResourceRemediationList.html';
import modalTemplate from './questionResourceRemediationModal.html';

import assetRemediation from './assetRemediation/assetRemediation.jsx';

export default angular
  .module('lo.questions.addons.questionResourceRemediation', [assetRemediation.name])
  .component('questionResourceRemediationList', {
    template: listTemplate,
    bindings: {
      resources: '<',
    },
    controller: [
      '$uibModal',
      function ($uibModal) {
        this.showRemediation = remediation => {
          $uibModal.open({
            component: 'resource-remediation-modal',
            size: 'question-remdiation',
            resolve: {
              remediation: () => remediation,
            },
          });
        };
      },
    ],
  })
  .component('resourceRemediationModal', {
    template: modalTemplate,
    bindings: {
      resolve: '<',
      _dismiss: '&dismiss',
    },
    controller: function () {
      this.$onInit = () => {
        this.title = this.resolve.remediation.title;
        this.dismiss = () => {
          this._dismiss();
        };
      };
    },
  });
