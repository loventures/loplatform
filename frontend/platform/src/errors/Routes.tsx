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

import EtcLoading from '../etc/EtcLoading';
import { setLoPlatform, setTranslations } from '../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../redux/state';
import { getPlatform, getTranslations, isDevelopment } from '../services';
import GenericError from './GenericError';
import LoginRequired from './LoginRequired';

const Routes: React.FC = () => {
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
        // for errors pretend the api key user is not logged in
        const { user, ...lop } = platformRes.data;
        const loplat =
          isDevelopment && user && user.user_type === 'System' ? lop : { user, ...lop };
        if (window.lo_error_file === 'loggedOut') loplat.loggedOut = true;
        dispatch(setLoPlatform(loplat));
        setLoaded(true);
      }
    );
  }, [dispatch]);

  const error = window.lo_error_file;
  if (!loaded) {
    return <EtcLoading />;
  } else if (error === 'login' || error === 'loginRequired' || error === 'loggedOut') {
    const {
      domain: { name, image },
    } = lo_platform;
    return (
      <React.Fragment>
        {image && (
          <img
            className="domain-image fade"
            src={image.url}
            alt={name}
            aria-hidden
          />
        )}
        <LoginRequired
          T={T}
          lo_platform={lo_platform}
        />
      </React.Fragment>
    );
  } else {
    return (
      <GenericError
        T={T}
        lo_platform={lo_platform}
        error={error!}
      />
    );
  }
};

export default Routes;
