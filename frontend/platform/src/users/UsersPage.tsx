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

import React from 'react';
import { useDispatch } from 'react-redux';
import { Navigate, Route, Routes, useParams } from 'react-router-dom';

import Crumb, { SetLastCrumb } from '../components/crumbRoute';
import CourseSections from '../groups/courseSections/CourseSectionsPage';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../redux/state';
import Users from './Users';
import { IoPersonOutline } from 'react-icons/io5';

// CourseSections nested under a user: derives the user_id prefilter from the route
// param. Mounted at ":userId/CourseSections/*" so CourseSections' own nested routes
// resolve relative to it.
const UserCourseSections: React.FC<{ setLastCrumb: SetLastCrumb }> = ({ setLastCrumb }) => {
  const { userId } = useParams<{ userId: string }>();
  return (
    <CourseSections
      setLastCrumb={setLastCrumb}
      customFilters={[
        {
          property: 'user_id',
          operator: 'eq',
          value: userId,
          prefilter: true,
        },
      ]}
      user={userId as unknown as number}
    />
  );
};

const UsersPage = () => {
  const T = useTranslations();
  const lo_platform = useLoPlatform();
  const dispatch = useDispatch();
  const dispatchPortalAlertStatus = (error: boolean, success: boolean, message: string) =>
    dispatch(setPortalAlertStatus(error, success, message));
  return (
    <Routes>
      <Route
        path=""
        element={
          <Users
            translations={T}
            lo_platform={lo_platform}
            setPortalAlertStatus={dispatchPortalAlertStatus}
          />
        }
      />
      <Route
        path=":userId/CourseSections/*"
        element={
          <Crumb title="">
            {({ setLastCrumb }) => <UserCourseSections setLastCrumb={setLastCrumb} />}
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

(UsersPage as any).pageInfo = {
  identifier: 'users',
  icon: IoPersonOutline,
  link: '/Users',
  group: 'users',
  right: 'loi.cp.admin.right.UserAdminRight',
  entity: 'users',
};

export default UsersPage;
