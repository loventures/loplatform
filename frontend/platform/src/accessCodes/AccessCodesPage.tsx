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
import { IoKeyOutline } from 'react-icons/io5';
import { Navigate, Route, Routes } from 'react-router-dom';

import { AdminPage } from '../adminPortal/types';
import Crumb from '../components/crumbRoute';
import AccessCodeBatches from './AccessCodeBatches';
import AccessCodes from './AccessCodes';

const AccessCodesPage: React.FC & AdminPage = () => {
  return (
    <Routes>
      <Route
        path=""
        element={<AccessCodeBatches />}
      />
      <Route
        path=":batchId"
        element={
          <Crumb title="">
            {({ setLastCrumb }) => <AccessCodes setLastCrumb={setLastCrumb} />}
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

AccessCodesPage.pageInfo = {
  identifier: 'accessCodes',
  icon: IoKeyOutline,
  link: '/AccessCodes',
  group: 'users',
  right: 'loi.cp.admin.right.AdminRight',
};

export default AccessCodesPage;
