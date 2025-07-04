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
// import $ from 'jquery';
import Request from '../utilities/Request.js';

export default angular
  .module('lo.services.LTILauncher', [Request.name])
  .service('LTILauncherService', [
    '$translate',
    'Request',
    function LTILauncher($translate, Request) {
      const service = {};

      service.getConfig = url => {
        return Request.promiseRequest(url, 'get');
      };

      service.createLtiFormHtml = ltiConfig => {
        const formHtml = `
      <form
        action="${ltiConfig.url}"
        method="POST"
        target="${ltiConfig.newWindow ? '_blank' : ''}"
      >
        ${map(
          ltiConfig.parameters,
          ({ name, value }) => `<input name="${name}" value="${value}" type="hidden"/>`
        ).join(' ')}
        <button>${$translate.instant('LTI_LAUNCH_NEW_WINDOW')}</button>
      </form>
    `;

        return formHtml;
      };

      service.launchInNewWindow = launchPath => {
        return service.getConfig(launchPath).then(ltiConfig => {
          const formHtml = service.createLtiFormHtml(ltiConfig);
          $(formHtml).appendTo('body').submit().css('display', 'none');
        });
      };

      service.launchInIframe = (launchPath, iframe) => {
        return service.getConfig(launchPath).then(ltiConfig => {
          const formHtml = service.createLtiFormHtml(ltiConfig);
          const newDoc = iframe.contentWindow.document;

          newDoc.open();
          newDoc.write(formHtml);
          newDoc.close();

          //for new window launches, let the user click on it
          if (!ltiConfig.newWindow) {
            newDoc.forms[0].submit();
          }
        });
      };

      return service;
    },
  ]);
