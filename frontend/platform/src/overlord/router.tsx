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

import axios from 'axios';
import classnames from 'classnames';
import Polyglot from 'node-polyglot';
import React, { useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Navigate, Route, Routes } from 'react-router-dom';

import AlertBar from '../components/AlertBar';
import { setLoPlatform, setPortalAlertStatus, setTranslations } from '../redux/actions/MainActions';
import { useLoPlatform } from '../redux/state';
import { getPlatform, getTranslations, isDevelopment } from '../services';
import { NoSessionExtensionHdr, UserIdHdr } from '../services/Headers';
import { hasRight } from '../services/Rights';
import Login from './Login';
import OverlordBar from './OverlordBar';
import allPages from './pages';
import Portal from './Portal';

type SetPage = (page: string | null) => void;

interface AdminPageProps {
  page: string | null;
  setPage: SetPage;
  children?: React.ReactNode;
}

const AdminPage: React.FC<AdminPageProps> = ({ page, setPage, children }) => {
  const [show, setShow] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setShow(true), 0);
    setPage(page);
    return () => {
      clearTimeout(timer);
      setPage(null);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div
      id="overlord-page"
      className={classnames('fade', { show })}
    >
      {children}
    </div>
  );
};

interface LegacyPageProps {
  page: string;
  setPage: SetPage;
  href?: string;
}

const LegacyPage: React.FC<LegacyPageProps> = ({ page, setPage, href }) => {
  const [show, setShow] = useState(false);
  const ifr = useRef<HTMLIFrameElement | null>(null);

  useEffect(() => {
    setPage(page);
    if (ifr.current) {
      ifr.current.onload = () => setShow(true);
    }
    return () => setPage(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <iframe
      id="overlord-frame"
      title="legacy"
      className={classnames('fade', { show })}
      src={href}
      ref={ifr}
    ></iframe>
  );
};

const OverlordRoutes: React.FC = () => {
  const dispatch = useDispatch();
  const lo_platform = useLoPlatform();

  const [loaded, setLoaded] = useState(false);
  const [fadeIn, setFadeIn] = useState(false);
  const [page, setPageState] = useState<string | null>(null);
  const [loggedOut, setLoggedOut] = useState(false);

  const interval = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const platformRef = useRef(lo_platform);
  platformRef.current = lo_platform;

  const startInterval = () => {
    const expired = (out: boolean) => setLoggedOut(out);
    const pollSession = () => {
      interval.current = undefined;
      const { user } = platformRef.current;
      if (user) {
        axios
          .get('/api/v0/session', {
            hideProgress: true,
            headers: { [NoSessionExtensionHdr]: 'true', [UserIdHdr]: user.id },
          } as any)
          .then(response => {
            expired(!response.data.valid);
            startInterval();
          })
          .catch(error => {
            if (error.response && error.response.status < 500) {
              // ignore server restart/...
              expired(true);
            }
            startInterval();
          });
      } else {
        startInterval();
      }
    };
    interval.current = setTimeout(pollSession, isDevelopment ? 15000 : 30000);
  };

  useEffect(() => {
    if (window.location !== window.parent.location) {
      window.top!.location.href = '/';
    } else {
      axios
        .all([getPlatform(), getTranslations(window.locale)])
        .then(([loPlatformRes, translationsRes]) => {
          dispatch(setLoPlatform(loPlatformRes.data));
          dispatch(
            setTranslations(
              new Polyglot({
                locale: loPlatformRes.data.domain.locale,
                phrases: translationsRes.data,
              })
            )
          );
          setLoaded(true);
          setTimeout(() => setFadeIn(true), 300);
          startInterval();
        });
    }
    return () => {
      if (interval.current) {
        clearTimeout(interval.current);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const setPage: SetPage = nextPage => {
    dispatch(setPortalAlertStatus(false, false, null as unknown as string));
    setPageState(nextPage);
  };

  if (!loaded) return null;

  const { user, clusterType } = lo_platform;

  const pages = allPages
    .filter(p => isDevelopment || hasRight(user, p.right))
    .filter(p => p.embed || p.Page);

  return (
    <div
      className={classnames(
        'overlord',
        clusterType,
        { unloaded: !fadeIn },
        { anonymous: loggedOut || !user }
      )}
    >
      <div className="eye right-eye"></div>
      <div className="eye left-eye"></div>
      <div className="overlorde">
        <div className="overimg"></div>
      </div>
      <OverlordBar page={page} />
      <AlertBar />
      {loggedOut ? (
        <div id="session-expired">Session Ended</div>
      ) : !user ? (
        <Login />
      ) : (
        <Routes>
          <Route
            path="/"
            element={
              <AdminPage
                page={null}
                setPage={setPage}
              >
                <Portal />
              </AdminPage>
            }
          />
          {pages.map(({ route, identifier, embed, Page }) => (
            <Route
              key={identifier}
              path={`${route || `/${identifier}`}/*`}
              element={
                Page ? (
                  <AdminPage
                    page={identifier}
                    setPage={setPage}
                  >
                    <Page />
                  </AdminPage>
                ) : (
                  <LegacyPage
                    page={identifier}
                    setPage={setPage}
                    href={embed}
                  />
                )
              }
            />
          ))}
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
      )}
    </div>
  );
};

export default OverlordRoutes;
