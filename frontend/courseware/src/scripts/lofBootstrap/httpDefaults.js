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

import User from '../utilities/User';

export default angular.module('lof.bootstrap.httpDefaults', [User.name]).run([
  '$http',
  'User',
  function ($http, User) {
    if (User) {
      $http.defaults.headers.common['X-UserId'] = +User.getId();
    }
    $http.defaults.headers.common['Accept'] = 'application/json, */*';
  },
]);
