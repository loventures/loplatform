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

import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';
import Course from '../bootstrap/course.js';

export default angular.module('lo.services.MessagingService', []).factory('MessagingService', [
  'Request',
  function (Request) {
    var service = {};

    service.getUserMessages = function () {
      var url = new UrlBuilder(loConfig.messaging.list);

      url.query.setFilter('label', 'eq', 'Inbox');
      url.query.setOrder('timestamp', 'desc');

      return Request.promiseRequest(url, 'get');
    };

    service.sendMessage = function (msg) {
      var url = loConfig.messaging.send;

      msg.context_id = Course.id;

      return Request.promiseRequest(url, 'post', msg);
    };

    return service;
  },
]);
