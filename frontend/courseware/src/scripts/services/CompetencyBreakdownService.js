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

import Request from '../utilities/Request.js';
import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';

export default angular
  .module('lo.services.CompetencyBreakdownService', [Request.name])
  .factory('CompetencyBreakdownService', [
    'Request',
    function CompetencyBreakdownService(Request) {
      const service = {};

      service.getCompetencyStatus = (contentIdentifier, userId) => {
        const url = new UrlBuilder(loConfig.competencyStatus.byContentIdentifier, {
          contentIdentifier,
          viewAs: userId,
        });

        return Request.promiseRequest(url, 'get');
      };

      return service;
    },
  ]);
