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

import { getAssetRenderUrl } from '../../../utilities/assetRendering.js';

export default angular
  .module('lo.questions.addons.htmlRemediation', [])
  .component('legacyHtmlRemediation', {
    bindings: {
      remediation: '<',
    },
    template: '<div compile="$ctrl.html"></div>',
    controller: function () {
      this.$onInit = () => {
        this.html = this.remediation.html;
        this.loaded = true;
      };
    },
  })
  .component('htmlRemediation', {
    bindings: {
      remediation: '<',
    },
    template: `
        <embedded-content url="$ctrl.url"></embedded-content>
    `,
    controller: function () {
      this.$onInit = () => {
        this.url = getAssetRenderUrl(
          this.remediation.reference.nodeName,
          this.remediation.reference.commit
        );
        this.loaded = true;
      };
    },
  });
