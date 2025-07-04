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

import Course from '../../bootstrap/course';
import { withTranslation } from '../../i18n/translationContext';
import { lojector } from '../../loject';

const AppLogout = ({ translate, isPreviewing, isStudent }) => {
  if (isPreviewing || Course.groupType === 'PreviewSection') {
    return (
      <button
        className="dropdown-item"
        id="nav-user-dropdown-exit"
        onClick={() => {
          const returnUrl = sessionStorage.getItem('returnUrl');
          const courseUrl = window.lo_platform.course.url + '/' + window.location.hash;
          sessionStorage.removeItem('returnUrl');
          window.location = returnUrl ?? courseUrl;
        }}
      >
        {translate(
          isStudent
            ? 'APP_HEADER_EXIT_STUDO'
            : isPreviewing
              ? 'APP_HEADER_EXIT_INSTRUCTOR'
              : 'APP_HEADER_EXIT_AUTHOR'
        )}
      </button>
    );
  } else if (lojector.get('SessionService').isSudo()) {
    return (
      <button
        className="dropdown-item"
        id="nav-user-dropdown-exit"
        onClick={() => {
          lojector.get('SessionManager').exit();
        }}
      >
        {translate('APP_HEADER_EXIT')}
      </button>
    );
  } else if (window.lo_platform.session?.logoutReturnUrl) {
    return (
      <button
        className="dropdown-item"
        id="nav-user-dropdown-lti-return"
        onClick={() => {
          lojector.get('SessionManager').logout(window.lo_platform.session.logoutReturnUrl);
        }}
      >
        {translate('APP_HEADER_LOGOUT')}
      </button>
    );
  } else {
    return (
      <button
        className="dropdown-item"
        id="nav-user-dropdown-logout"
        onClick={() => {
          lojector.get('SessionManager').logout();
        }}
      >
        {translate('APP_HEADER_LOGOUT')}
      </button>
    );
  }
};

export default withTranslation(AppLogout);
