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

import queryString from 'query-string';
import React from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';

import { AdminPage } from '../../adminPortal/types';
import Crumb from '../../components/crumbRoute';
import { useLoPlatform, useTranslations } from '../../redux/state';
import Configurations from '../Configurations';
import Enrollments from '../Enrollments';
import TestSections from './TestSections';
import { PiHardHat } from 'react-icons/pi';

const Identifier = 'testSections';

const TestSectionsIndex: React.FC<{ readOnly: boolean }> = ({ readOnly }) => {
  const location = useLocation();
  const parsed = queryString.parse(location.search);
  const adding = parsed && parsed.project;
  const initModal = adding
    ? {
        projectId: parseInt(parsed.project as string, 10),
      }
    : null;
  return (
    <TestSections
      initModal={initModal}
      readOnly={readOnly}
    />
  );
};

const TestSectionsMain: React.FC & AdminPage = () => {
  const T = useTranslations();
  const lo_platform = useLoPlatform();
  const rights = lo_platform.user.rights || [];
  const readOnly =
    !rights.includes('loi.cp.admin.right.CourseAdminRight') &&
    !rights.includes('loi.cp.course.right.ManageCoursesAdminRight');

  return (
    <Routes>
      <Route
        path=""
        element={<TestSectionsIndex readOnly={readOnly} />}
      />
      <Route
        path=":courseId/Configurations/*"
        element={
          <Crumb title="">
            {({ setLastCrumb }) => (
              <Configurations
                setLastCrumb={setLastCrumb}
                T={T}
                controllerValue={Identifier}
              />
            )}
          </Crumb>
        }
      />
      <Route
        path=":courseId/Enrollments/*"
        element={
          <Crumb title="">
            {({ setLastCrumb }) => (
              <Enrollments
                setLastCrumb={setLastCrumb}
                T={T}
                sudoUrl={course => course.url}
                controllerValue={Identifier}
                includeRights={true}
                readOnly={readOnly}
              />
            )}
          </Crumb>
        }
      />
      <Route
        path="*"
        element={
          <Navigate
            to="/"
            replace
          />
        }
      />
    </Routes>
  );
};

TestSectionsMain.pageInfo = {
  identifier: Identifier,
  icon: PiHardHat,
  link: '/TestSections',
  group: 'courses',
  right: 'loi.cp.course.right.ManageCoursesReadRight',
  entity: 'testSections',
};

export default TestSectionsMain;
