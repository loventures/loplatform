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

import classnames from 'classnames';
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Navigate, Route, Routes } from 'react-router-dom';

import Error from '../components/Error';
import Crumb from '../components/crumbRoute';
import OverlordBar from '../overlord/OverlordBar';
import { setLoPlatform, setTranslations } from '../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../redux/state';
import SysScript from '../script/ScriptPage';
import { getPlatform, getTranslations } from '../services';
import SysLoading from './SysLoading';

const SysRoutes: React.FC = () => {
  const dispatch = useDispatch();
  const T = useTranslations();
  const lo_platform = useLoPlatform();
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
        dispatch(setLoPlatform(platformRes.data));
        setLoaded(true);
      }
    );
  }, [dispatch]);

  if (!loaded) return <SysLoading />;
  const { user, clusterType } = lo_platform;
  const page = 'Script/Scala'; // Add behaviour once we have more pages...
  return (
    <div className={classnames('overlord', clusterType, { anonymous: !user })}>
      <div className="eye right-eye"></div>
      <div className="eye left-eye"></div>
      <div className="overlorde">
        <div className="overimg"></div>
      </div>
      <OverlordBar
        page={page}
        simple
      />
      <Routes>
        <Route
          path="/script"
          element={
            <Navigate
              to="/script/scala"
              replace
            />
          }
        />
        <Route
          path="/script/scala"
          element={
            <Crumb title={T.t('overlord.page.Script/Scala.name')}>
              <SysScript />
            </Crumb>
          }
        />
        <Route
          path="/script/sql"
          element={
            <Crumb title={T.t('overlord.page.Script/Scala.name')}>
              <SysScript />
            </Crumb>
          }
        />
        <Route
          path="/script/redshift"
          element={
            <Crumb title={T.t('overlord.page.Script/Scala.name')}>
              <SysScript />
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
    </div>
  );
};

export default SysRoutes;
