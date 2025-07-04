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

import { CONTENT_TYPE_RESOURCE } from '../../../utilities/contentTypes.js';
import { READING_INSTRUCTIONS } from '../../../utilities/resource1Types.js';

const template = `
    <resource-remediation-renderer
        ng-if="$ctrl.loaded"
        content="$ctrl.content"
        asset-info="$ctrl.assetInfo"
        display-files="$ctrl.displayFiles"
    ></resource-remediation-renderer>
`;

export default angular
  .module('lo.questions.addons.resource1Remediation', [])
  .component('legacyResource1Remediation', {
    bindings: {
      remediation: '<',
    },
    template,
    controller: function () {
      this.$onInit = () => {
        const instructions = this.remediation.instructions || this.remediation.instruction;
        this.questionId = {
          typeId: CONTENT_TYPE_RESOURCE,
          competencies: [],
          activity: {
            ...this.remediation,
            instructions,
          },
        };
        this.loaded = true;
      };
    },
  })
  .component('resource1Remediation', {
    bindings: {
      remediation: '<',
    },
    template,
    controller: function () {
      this.$onInit = () => {
        this.questionId = {
          typeId: CONTENT_TYPE_RESOURCE,
          competencies: [],
          activity: {
            resourceType: this.remediation.resourceType || READING_INSTRUCTIONS,
          },
          node_name: this.remediation.reference.nodeName,
        };
        this.loaded = true;
      };
    },
  });
