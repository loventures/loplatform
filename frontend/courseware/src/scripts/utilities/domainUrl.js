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

import { get } from 'lodash';

const isLocal = get(window, 'lo_platform.environment.isLocal', false);

//TODO: quick port hack, that lets *local* content work in webpack which is normally hardcoded/defaulted to 8181
//make port configurable in lo_platform.domain on the backend?
let port = get(window, 'lo_platform.domain.port', isLocal ? '8181' : '');

//use of lo_platform hostname allows server resources to be viewed during webpack hot deploy
let url = window.location.protocol + '//' + window.lo_platform.domain.hostName;
if (port) {
  url = url + ':' + port;
}

export default url;
