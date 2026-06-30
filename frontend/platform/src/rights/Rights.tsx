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

import { AdminPage } from '../adminPortal/types';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import RightsTree from './RightsTree';
import { PiGavelLight } from 'react-icons/pi';

const Rights: React.FC & AdminPage = () => {
  const T = useTranslations();
  const dispatch = useDispatch();
  return (
    <RightsTree
      translations={T}
      setPortalAlertStatus={(error, success, message) =>
        dispatch(setPortalAlertStatus(error, success, message))
      }
      rolesUrl="/api/v2/roles"
      rightTreeUrl="/api/v2/rights"
      rightsUrl="/api/v2/rights/all"
      postUrl="/api/v2/rights"
    />
  );
};

Rights.pageInfo = {
  identifier: 'rights',
  icon: PiGavelLight,
  link: '/Rights',
  group: 'users',
  right: 'loi.cp.admin.right.AdminRight',
};

export default Rights;
