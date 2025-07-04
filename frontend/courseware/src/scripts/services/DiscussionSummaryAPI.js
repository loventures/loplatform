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

import { orderBy } from 'lodash';

import { toContentIdentifierForContext } from '../utilities/contentIdentifier.js';

import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';
import Course from '../bootstrap/course.js';

export default angular
  .module('lo.services.DiscussionSummaryAPI', [])
  .service('DiscussionSummaryAPI', [
    'Request',
    function (Request) {
      const service = {};

      const toContentIdentifier = toContentIdentifierForContext(Course.id);

      service.getSummary = function (discussionId) {
        const url = new UrlBuilder(loConfig.discussionBoard.userPostCount, {
          discussion: toContentIdentifier(discussionId),
        });
        return Request.promiseRequest(url, 'get').then(data => {
          return orderBy(data, 'user.fullName');
        });
      };

      return service;
    },
  ]);
