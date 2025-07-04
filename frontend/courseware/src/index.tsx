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

import angular from 'angular';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import axios from 'axios';
import cookies from 'browser-cookies';
import ERAppRoot from './scripts/ERAppRoot';

import ngApp from './scripts/ngApp';
import NgReady from './scripts/NgReady';

import './styles/main.sass';

angular.element(document).ready(function () {
  const courseNgEle = document.createElement('div');
  courseNgEle.setAttribute('id', 'course-app-ng');
  document.body.appendChild(courseNgEle);
  angular.bootstrap(courseNgEle, [ngApp.name], { strictDi: true });
});

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

createRoot(document.getElementById('course-app')!).render(
  <StrictMode>
    <NgReady
      ngModuleName={ngApp.name}
      render={() => <ERAppRoot />}
    />
  </StrictMode>
);
