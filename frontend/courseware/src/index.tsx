/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { createRoot } from 'react-dom/client';

import 'iframe-resizer/js/iframeResizer.contentWindow';
import iframeResizer from 'iframe-resizer/js/iframeResizer';

import axios from 'axios';
import cookies from 'browser-cookies';

// MUST be the first scripts import: eagerly initializes the pure `Roles` + `settings` singletons (the
// re-homed `bootstrap/globalFeatures.js` config block) before any consumer reads them at module load —
// notably `utilities/currentUserData.ts`, which reads `Roles.getPrimaryRole()` at import time.
import './scripts/bootstrap/coreInit.ts';
// Pure side-effects re-homed from the deleted Angular bootstrap tree: axiosConfig sets the global axios X-CSRF
// defaults; lscacheExtend monkey-patches lscache.userLoad (a runtime dep of enrolledUserService).
import './scripts/lofBootstrap/axiosConfig.js';
import './scripts/utilities/lscacheExtend.jsx';

import ERAppRoot from './scripts/ERAppRoot';

import './styles/main.scss';

// Plain DOM/global setup re-homed from the AngularJS `lof.bootstrap.nonAngular` `.run` blocks. These are
// not React-dependent, so they run once at module load.
if ((window as any).find) {
  (window as any)._find = (window as any).find;
  (window as any).find = function (...args: any[]) {
    console.error('You are using window.find, you probably forgot to import lodash find');
    (window as any)._find(...args);
  };
}

if ((window as any).FastClick) {
  (window as any).FastClick.attach(document.body);
}

if (!String.prototype.endsWith) {
  // eslint-disable-next-line no-extend-native
  String.prototype.endsWith = function (suffix: string) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
  };
}

if (!window.lo_platform.environment.isMock) {
  (window as any).iFrameResize = iframeResizer;
}

axios.defaults.headers.common['X-UserId'] = window.lo_platform?.user?.id;

// This supports preview roles for inline LTI launch. This is safe because a preview
// role is only accepted if you have preview user permission on the back-end. We can't
// tack the role onto the launch query because those are server-rendered and cached.
if (window.lo_platform?.user?.user_type === 'Preview') {
  const isInstructor = window.lo_platform.course_roles?.some(r => r.includes('TeachCourseRight'));
  const course = window.lo_platform.course?.id;
  const role = isInstructor ? 'Instructor' : 'Learner';
  cookies.set('X-PreviewRole', `${course}:${role}`, {
    path: '/',
    secure: true,
    samesite: 'Lax',
  });
} else {
  cookies.erase('X-PreviewRole', { path: '/', secure: true, samesite: 'Lax' });
}

createRoot(document.getElementById('course-app')!).render(<ERAppRoot />);
