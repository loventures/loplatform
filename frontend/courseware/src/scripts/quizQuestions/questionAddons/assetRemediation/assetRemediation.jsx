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

import ResourceActivity from '../../../contentPlayerComponents/activityViews/resource/ResourceActivity.js';

import resource1Remediation from './resource1Remediation.jsx';
import fileBundleRemediation from './fileBundleRemediation.jsx';
import htmlRemediation from './htmlRemediation.jsx';

import {
  CONTENT_TYPE_RESOURCE,
  CONTENT_TYPE_HTML,
  CONTENT_TYPE_SCORM,
  CONTENT_TYPE_FILE_BUNDLE,
} from '../../../utilities/contentTypes.js';
import { react2angularWithQueryProvider } from '../../../utils/react2AngularWithQueryProvider.js';

export default angular
  .module('lo.questions.addons.assetRemediation', [
    resource1Remediation.name,
    fileBundleRemediation.name,
    htmlRemediation.name,
  ])
  .component(
    'resourceRemediationRenderer',
    react2angularWithQueryProvider(ResourceActivity, ['content', 'assetInfo', 'displayFiles'])
  )
  .component('assetRemediation', {
    bindings: {
      remediation: '<',
    },
    template: `
    <div compile="$ctrl.comp"></div>
  `,
    controller: function () {
      this.$onInit = () => {
        const compName = this.getCompName(this.remediation);
        this.comp = `
        <${compName} remediation="$ctrl.remediation">
        </${compName}>
      `;
      };

      this.getCompName = remediation => {
        if (remediation._type === 'assetRemediation') {
          switch (remediation.assetType) {
            case CONTENT_TYPE_RESOURCE:
              return 'resource1-remediation';
            case CONTENT_TYPE_FILE_BUNDLE:
              return 'file-bundle-remediation';
            case CONTENT_TYPE_SCORM:
              return 'scorm-remediation';
            case CONTENT_TYPE_HTML:
              return 'htms-remediation';
            default:
              return 'div';
          }
        }

        switch (remediation._type) {
          case '.ResourceAssetRemediation':
            return 'legacy-resource1-remediation';
          case '.FileBundleAssetRemediation':
            return 'legacy-file-bundle-remediation';
          case '.HtmlAssetRemediation':
            return 'legacy-htms-remediation';
          default:
            return 'div';
        }
      };
    },
  });
