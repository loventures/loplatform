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
import { IoLibraryOutline } from 'react-icons/io5';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';

import { AdminPage, PageInfo } from '../adminPortal/types';
import Announcements from '../announcements';
import Crumb from '../components/crumbRoute';
import Configurations from '../groups/Configurations';
import { useTranslations } from '../redux/state';
import CourseOfferings from './CourseOfferings';

const Identifier = 'lwc/courseOfferings';

const CourseOfferingsIndex: React.FC = () => {
  const location = useLocation();
  const parsed = queryString.parse(location.search);
  const filtered = parsed && parsed.project;
  const filter = filtered
    ? {
        projectId: parseInt(parsed.project as string, 10),
      }
    : undefined;
  return <CourseOfferings initFilter={filter} />;
};

const CourseOfferingsMain: React.FC & AdminPage = () => {
  const T = useTranslations();
  return (
    <Routes>
      <Route
        path=""
        element={<CourseOfferingsIndex />}
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
                warning={T.t('adminPage.lwc/courseOfferings.configWarning')}
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
                controllerValue={Identifier}
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

CourseOfferingsMain.pageInfo = {
  identifier: 'courseOfferings',
  icon: IoLibraryOutline,
  link: '/CourseOfferings',
  group: 'courses',
  right: 'loi.cp.admin.right.AdminRight',
  entity: 'lwc/courseOfferings',
} as PageInfo;

export default CourseOfferingsMain;
