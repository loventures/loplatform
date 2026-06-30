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

import React, { useEffect, useState } from 'react';
import DocumentTitle from 'react-document-title';
import { useDispatch } from 'react-redux';
import { useLocation } from 'react-router-dom';

import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTypedSelector } from '../redux/state';
import ScrollTopAlert from './ScrollTopAlert';

interface HeaderProps {
  headerStr?: string;
  pageClass?: string;
  [key: string]: unknown;
}

const createAdminPage = (
  WrappedComponent: React.ComponentType<any>,
  headerProps: HeaderProps,
  wrappedProps?: Record<string, unknown>
): React.FC => {
  const AdminPage: React.FC = () => {
    const location = useLocation();
    const dispatch = useDispatch();
    const adminPageError = useTypedSelector(state => state.main.adminPageError);
    const adminPageSuccess = useTypedSelector(state => state.main.adminPageSuccess);
    const adminPageMessage = useTypedSelector(state => state.main.adminPageMessage);
    const [opacity, setOpacity] = useState(0);

    const naked = () => !!location.search && location.search.indexOf('naked') >= 0;

    const hideAlert = () => dispatch(setPortalAlertStatus(false, false, ''));

    useEffect(() => {
      if (naked()) {
        document.body.classList.add('naked');
      }
      setTimeout(() => setOpacity(1), 0);
      return () => {
        if (naked()) {
          document.body.classList.remove('naked');
        }
        hideAlert();
      };
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const renderAlert = () => {
      if (adminPageError || adminPageSuccess) {
        return (
          <div className="container-fluid">
            <ScrollTopAlert
              id="admin-page-alert"
              color={adminPageError ? 'warning' : 'success'}
              toggle={hideAlert}
            >
              {adminPageMessage}
            </ScrollTopAlert>
          </div>
        );
      }
      return null;
    };

    const finalWrappedProps = {
      // sad effects initialization
      ...(wrappedProps || {}),
    };

    return (
      <DocumentTitle title={headerProps.headerStr}>
        <div className={headerProps.pageClass}>
          {renderAlert()}
          <div
            style={{ opacity, transition: 'opacity 0.5s ease-out' }}
            role="main"
          >
            <WrappedComponent {...finalWrappedProps} />
          </div>
        </div>
      </DocumentTitle>
    );
  };

  return AdminPage;
};

export default createAdminPage;
