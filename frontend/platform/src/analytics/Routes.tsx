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

import Crumb from '../components/crumbRoute';
import NavigationBar from '../components/navigationBar';
import LoginRequired from '../errors/LoginRequired';
import EtcLoading from '../etc/EtcLoading';
import { setLoPlatform, setTranslations } from '../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../redux/state';
import { getPlatform, getTranslations, isDevelopment } from '../services';
import MetabaseEmbed from './MetabaseEmbed';

const AnalyticsRoutes: React.FC = () => {
  const dispatch = useDispatch();
  const T = useTranslations();
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
  }, [dispatch]);

  if (!loaded) {
    return <EtcLoading />;
  } else if (!lop.user) {
    return (
      <LoginRequired
        T={T}
        lo_platform={lop}
      />
    );
  } else {
    const {
      domain: { name },
    } = lop;
    return (
      <React.Fragment>
        <NavigationBar
          nonAdmin
          domainApp
        />
        <Routes>
          <Route
            path="/:embedType/:id"
            element={
              <Crumb
                title={T.t('page.analytics.name')}
                documentTitle={`${name} - ${T.t('page.analytics.name')}`}
              >
                <MetabaseEmbed T={T} />
              </Crumb>
            }
          />
        </Routes>
      </React.Fragment>
    );
  }
};

export default AnalyticsRoutes;
