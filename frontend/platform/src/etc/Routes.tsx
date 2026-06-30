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

import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Route, Routes } from 'react-router-dom';

import { Breadcrumbs } from '../components/breadcrumbs';
import Crumb from '../components/crumbRoute';
import Error from '../components/Error';
import NavigationBar from '../components/navigationBar';
import Localmail from '../localmail';
import { setLoPlatform, setTranslations } from '../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../redux/state';
import { isDevelopment } from '../services';
import { getPlatform, getTranslations } from '../services/';
import About from './About';
import EtcLoading from './EtcLoading';
import LoginRegister from './loginRegister';
import ResetPassword from './ResetPassword';

const EtcRoutes: React.FC = () => {
  const dispatch = useDispatch();
  const T: Polyglot = useTranslations();
  const lop = useLoPlatform();
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    Promise.all([getPlatform(), getTranslations(window.locale)]).then(
      ([platformRes, translationsRes]) => {
        dispatch(
          setTranslations(
            new Polyglot({
              locale: platformRes.data.domain.locale,
              phrases: translationsRes.data,
            })
          )
        );
        // for /etc/ pretend the api key user is not logged in
        const { user, ...lop } = platformRes.data;
        const loplat =
          isDevelopment && user && user.user_type === 'System' ? lop : { user, ...lop };
        dispatch(setLoPlatform(loplat));
        setLoaded(true);
      }
    );
  }, []);

  const naked = window.location.search.indexOf('naked') >= 0;
  return !loaded ? (
    <EtcLoading />
  ) : (
    <React.Fragment>
      {naked || (
        <React.Fragment>
          <NavigationBar nonAdmin />
          <Breadcrumbs
            className="breadcrumb admin-breadcrumb"
            separator="/"
          />
        </React.Fragment>
      )}
      <Routes>
        <Route
          path="/ResetPassword/:token"
          element={
            <Crumb title={T.t('page.resetPassword.name')}>
              {({ setLastCrumb }) => (
                <ResetPassword
                  setLastCrumb={setLastCrumb}
                  T={T}
                />
              )}
            </Crumb>
          }
        />
        <Route
          path="/LoginRegister"
          element={
            <Crumb title={T.t('page.register.name')}>
              <LoginRegister
                T={T}
                lop={lop}
              />
            </Crumb>
          }
        />
        <Route
          path="/About"
          element={
            <Crumb title={T.t('about.page.name')}>
              <About
                naked={naked}
                T={T}
              />
            </Crumb>
          }
        />
        <Route
          path="/Localmail"
          element={
            <Crumb title={T.t('page.localmail.name')}>
              <Localmail />
            </Crumb>
          }
        />
        <Route
          path="/Localmail/:account"
          element={
            <Crumb title={T.t('page.localmail.name')}>
              <Localmail />
            </Crumb>
          }
        />
        <Route
          path="*"
          element={
            <Crumb title={T.t('error.page.name')}>
              {({ setLastCrumb }) => (
                <Error
                  setLastCrumb={setLastCrumb}
                  T={T}
                />
              )}
            </Crumb>
          }
        />
      </Routes>
    </React.Fragment>
  );
};

export default EtcRoutes;
