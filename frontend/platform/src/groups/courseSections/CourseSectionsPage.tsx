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

import axios from 'axios';
import React, { useEffect } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';

import { AdminPage } from '../../adminPortal/types';
import Announcements from '../../announcements';
import Crumb from '../../components/crumbRoute';
import { useLoPlatform, useTranslations } from '../../redux/state';
import Configurations from '../Configurations';
import Enrollments from '../Enrollments';
import CourseSections from './CourseSections';
import { IoSchoolOutline } from 'react-icons/io5';

const Identifier = 'courseSections';

interface CustomFilter {
  property: string;
  value: unknown;
  operator?: string;
  prefilter?: boolean;
}

interface CourseSectionsPageProps {
  customFilters?: CustomFilter[];
  user?: number;
  setLastCrumb?: (title: string, documentTitle?: string) => void;
}

const CourseSectionsMain: React.FC<CourseSectionsPageProps> & AdminPage = props => {
  const { customFilters, user, setLastCrumb } = props;
  const T = useTranslations();
  const lo_platform = useLoPlatform();
  const rights = lo_platform.user.rights || [];
  const readOnly =
    !rights.includes('loi.cp.admin.right.CourseAdminRight') &&
    !rights.includes('loi.cp.course.right.ManageCoursesAdminRight');

  useEffect(() => {
    const userFilter =
      customFilters && customFilters.find(filter => filter.property === 'user_id');
    const userId = userFilter && userFilter.value;
    if (userId) {
      axios.get(`/api/v2/users/${userId}`).then(res => {
        const params = { fullName: res.data.fullName };
        setLastCrumb?.(T.t('adminPage.courseSections.name.withUser', params));
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Routes>
      <Route
        path=""
        element={
          <CourseSections
            customFilters={customFilters}
            user={user}
            readOnly={readOnly}
          />
        }
      />
      <Route
        path=":courseId/Configurations/*"
        element={
          <Crumb title="">
            {({ setLastCrumb }) => (
              <Configurations
                setLastCrumb={setLastCrumb}
                T={T}
                controllerValue="courseSections"
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
                controllerValue="courseSections"
                includeRights={true}
                readOnly={readOnly}
              />
            )}
          </Crumb>
        }
      />
      <Route
        path=":courseId/Announcements"
        element={
          <Crumb title="">
            {({ setLastCrumb }) => (
              <Announcements
                setLastCrumb={setLastCrumb}
                T={T}
                controllerValue="courseSections"
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

CourseSectionsMain.pageInfo = {
  identifier: Identifier,
  icon: IoSchoolOutline,
  link: '/CourseSections',
  group: 'courses',
  right: 'loi.cp.course.right.ManageCoursesReadRight',
  entity: 'courseSections',
};

export default CourseSectionsMain;
