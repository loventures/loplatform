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

import SessionManager from '../services/SessionService';
import Course from '../bootstrap/course';

export default angular.module('ple.bootstrap.instructorRole', [SessionManager.name]).run([
  'SessionManager',
  function (SessionManager) {
    SessionManager.start(function () {
      const ltiReturnLink =
        get(window, 'lo_platform.session.logoutReturnUrl') ||
        get(window, 'lo_platform.session.returnUrl');
      if (Course.LTI && ltiReturnLink) {
        window.location.href = ltiReturnLink;
      } else {
        window.location = '/'; // .reload(true);
      }
    });
  },
]);
