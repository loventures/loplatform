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

'use strict';

var isJsonContentType = function isJsonContentType(response) {
  var contentType = response.headers && response.headers.get('content-type');
  return contentType && contentType.indexOf('application/json') !== -1;
};

var parseResponseBody = function parseResponseBody(response) {
  if (isJsonContentType(response)) {
    return response.json();
  } else if (response instanceof Error) {
    return Promise.reject(response);
  } else {
    return response.text();
  }
};

const api = {
  isJsonContentType,
  parseResponseBody,
};

export default api;
