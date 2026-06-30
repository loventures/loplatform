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

import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTypedSelector } from '../redux/state';
import ScrollTopAlert from './ScrollTopAlert';

const AlertBar: React.FC = () => {
  const dispatch = useDispatch();
  const adminPageError = useTypedSelector(state => state.main.adminPageError);
  const adminPageSuccess = useTypedSelector(state => state.main.adminPageSuccess);
  const adminPageMessage = useTypedSelector(state => state.main.adminPageMessage);

  const hideAlert = () => dispatch(setPortalAlertStatus(false, false, ''));

  return adminPageError || adminPageSuccess ? (
    <div className="container-fluid">
      <ScrollTopAlert
        id="admin-page-alert"
        color={adminPageError ? 'warning' : 'success'}
        toggle={hideAlert}
      >
        {adminPageMessage}
      </ScrollTopAlert>
    </div>
  ) : null;
};

export default AlertBar;
