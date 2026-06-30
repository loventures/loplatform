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
import Polyglot from 'node-polyglot';
import React, { useEffect, useRef, useState } from 'react';
import DocumentTytle from 'react-document-title';
import { IIdleTimer, useIdleTimer } from 'react-idle-timer';
import { useDispatch, useStore } from 'react-redux';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { Navbar } from 'reactstrap';
import { isEqual } from 'underscore';

import AdminPage from '../components/adminPage';
import AnnouncementBar from '../components/announcementBar';
import { Breadcrumb, Breadcrumbs } from '../components/breadcrumbs';
import Crumb from '../components/crumbRoute';
import Error from '../components/Error';
import LegacyIframe from '../components/legacyIframe';
import NavigationBar from '../components/navigationBar';
import SessionExpired from '../components/sessionExpired';
import LoginForm from '../etc/loginRegister/LoginForm';
import { setAnnouncements } from '../redux/actions/AnnouncementActions';
import { setLoPlatform, setTranslations } from '../redux/actions/MainActions';
import { setIdleState } from '../redux/actions/PresenceActions';
import { RootState, useTranslations, useTypedSelector } from '../redux/state';
import { getAdminPages, getAnnouncements, getPlatform, getTranslations } from '../services/';
import { PresenceService } from '../services/Presence';
import getAvailableAdminPages from './pages';
import Portal from './Portal';

/**
 * react-idle-timer v5 dropped the default-export <IdleTimer> component in favor of the
 * useIdleTimer hook. This thin wrapper restores the component shape used below: it forwards
 * the timer instance to `onIdler` once on mount (and null on unmount) and renders its
 * children. The instance's methods are stable useCallbacks, so capturing it once is safe.
 */
interface IdleTimerProps {
  onIdler: (idler: IIdleTimer | null) => void;
  children?: React.ReactNode;
  element?: Document | HTMLElement;
  onActive?: () => void;
  onIdle?: () => void;
  timeout?: number;
}

const IdleTimer: React.FC<IdleTimerProps> = ({ onIdler, children, ...props }) => {
  const idleTimer = useIdleTimer(props);
  const idleTimerRef = useRef(idleTimer);
  idleTimerRef.current = idleTimer;
  useEffect(() => {
    onIdler(idleTimerRef.current);
    return () => onIdler(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return <>{children}</>;
};

const FakeCrumb: React.FC<{ title: string }> = ({ title }) => (
  <DocumentTytle title={title}>
    <div className="breadcrumb admin-breadcrumb border-bottom">
      <nav className="breadcrumbs ">
        <span className="breadcrumbs__section">
          <a
            className="breadcrumbs__crumb breadcrumbs__crumb--active"
            aria-current="true"
          >
            {title}
          </a>
        </span>
      </nav>
    </div>
  </DocumentTytle>
);

const AdminWait: React.FC = () => (
  <Navbar
    id="main-nav-bar-base"
    light
    expand
    className="navbar-toggleable-xl px-3"
    style={{ height: '3.5rem' }}
  />
);

const AdminWaitPage: React.FC<{ naked: boolean; title: string }> = ({ naked, title }) => (
  <React.Fragment>
    {!naked && <NavigationBar />}
    <DocumentTytle title={title} />
  </React.Fragment>
);

interface AdminLoginPageProps {
  T: Polyglot;
  logout?: string;
  naked: boolean;
}

const AdminLoginPage: React.FC<AdminLoginPageProps> = ({ T, logout, naked }) => {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    if (!logout) return;
    const onClick = () => {
      setPhase(p => {
        if (p) return p;
        setTimeout(() => setPhase(2), 500);
        return 1;
      });
    };
    let timer: ReturnType<typeof setTimeout> | undefined;
    if (logout === 'logout') {
      timer = setTimeout(onClick, 2500);
    }
    document.body.addEventListener('click', onClick);
    return () => {
      if (timer) clearTimeout(timer);
      document.body.removeEventListener('click', onClick);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = !logout || phase >= 2;
  const msg = login ? 'adminPortal.login.title' : `adminPortal.logout.title.${logout}`;
  return (
    <React.Fragment>
      <div
        id="admin-login-page"
        className="admin-login"
      >
        {!naked && <NavigationBar noFade={!!logout} />}
        {!naked && <DocumentTytle title={T.t(msg)} />}
        {login ? (
          <div className="login-form dark">
            <LoginForm
              T={T}
              color="dark"
              title={T.t('adminPortal.loginRequired')}
              rememberMe
            />
          </div>
        ) : logout ? (
          <div className="admin-logout">
            <h3 className={`logout-hdr logout-${logout} ${phase ? 'blur' : ''}`}>
              {T.t(`adminPortal.loggedOut.message.${logout}`)}
            </h3>
            {logout === 'expired' && (
              <div className="click-to-login">{T.t('adminPortal.loggedOut.clickToLogin')}</div>
            )}
          </div>
        ) : null}
        <div id="lo-copyright">
          <div>
            LO Platform &copy; 2007–2026{' '}
            <a
              id="lo-link"
              href="https://learningobjects.com/"
              target="_blank"
              rel="noopener"
              style={{ color: 'inherit !important' }}
            >
              LO Ventures LLC
            </a>
          </div>
        </div>
      </div>
    </React.Fragment>
  );
};

/**
 * Catches render-time errors thrown by an admin page so a single broken page
 * shows a fallback instead of blanking the entire portal (nav/breadcrumbs, which
 * render outside this boundary, keep working). Reset by keying on the pathname,
 * so navigating to another route clears the error.
 */
interface RouteErrorBoundaryProps {
  fallback: React.ReactNode;
  children?: React.ReactNode;
}

class RouteErrorBoundary extends React.Component<
  RouteErrorBoundaryProps,
  { hasError: boolean }
> {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error('Admin page failed to render:', error, info);
  }

  render() {
    return this.state.hasError ? this.props.fallback : this.props.children;
  }
}

const AdminRoutes: React.FC = () => {
  const dispatch = useDispatch();
  const location = useLocation();
  const T = useTranslations();
  // shouldComponentUpdate guard ported from the original class router: only
  // re-render Routes when user/logout change, not on every lo_platform refresh
  // (e.g. after saving config or domain settings), so the routed page isn't
  // remounted (which would reset in-progress page state). NavigationBar keeps its
  // own lo_platform subscription, so the domain name/logo still updates.
  const user = useTypedSelector(state => state.main.lo_platform.user, isEqual);
  const logout = useTypedSelector(state => state.main.lo_platform.logout, isEqual);
  const store = useStore<RootState>();

  const [adminPages, setAdminPages] = useState<Record<string, any[]>>({});
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const presence = useRef<PresenceService | null>(null);
  const loginRef = useRef(false);

  useEffect(() => {
    axios
      .all([getPlatform(), getTranslations(window.locale)])
      .then(([loPlatformRes, translationsRes]) => ({
        lo_platform: loPlatformRes.data,
        translations: new Polyglot({
          locale: loPlatformRes.data.domain.locale,
          phrases: translationsRes.data,
        }),
      }))
      .then(({ lo_platform, translations }) => {
        dispatch(setLoPlatform(lo_platform));
        dispatch(setTranslations(translations));
        if (lo_platform.user) {
          return axios
            .all([getAdminPages(), getAnnouncements()])
            .then(([adminPagesRes, announcementsRes]) => {
              dispatch(setAnnouncements(announcementsRes.data.objects));
              setAdminPages(adminPagesRes.data.adminPages);
              setLoaded(true);
              document.getElementById('admin-loading')?.remove();
              /* once we've mounted, summon the lazy modules in the background */
              // noinspection JSIgnoredPromiseFromCall
              Promise.all([
                import('../announcements/Announcements'),
                import('../component/ComponentPage'),
                import('../config/config'),
                import('../jobs/JobsPage'),
              ]);
            })
            .catch(e => {
              console.log(e);
              if (e.request && e.request.status === 403) {
                if (loginRef.current) {
                  document.location.href = '/'; // redirect a non-ahmin login
                } else {
                  setLoaded(true);
                  setError(translations.t('error.accessDenied'));
                }
              } else {
                setLoaded(true);
                setError(translations.t('error.unexpectedError'));
              }
            });
        } else {
          document.getElementById('admin-loading')?.remove();
          setLoaded(true);
        }
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const naked = !!location.search && location.search.indexOf('naked') >= 0;

  if (!loaded) {
    return !T.t ? (
      <AdminWait />
    ) : (
      <AdminWaitPage
        naked={naked}
        title={T.t('adminPortal.loading.title')}
      />
    );
  } else if (!user) {
    return (
      <AdminLoginPage
        T={T}
        naked={naked}
        logout={logout as string | undefined}
      />
    );
  } else if (error) {
    return (
      <React.Fragment>
        {!naked && <NavigationBar />}
        {!naked && <FakeCrumb title={T.t('adminPortal.error.title')} />}
        <Error
          T={T}
          setLastCrumb={() => {}}
          message={error}
        />
      </React.Fragment>
    );
  }

  const flatAdminPages = Object.keys(adminPages).reduce<any[]>((array, key) => {
    const pages = adminPages[key].map(page => {
      page.key = key;
      return page;
    });
    return array.concat(pages);
  }, []);

  const adminPageRoutes = flatAdminPages.map(page => {
    const link = '/sys/admin/' + page.identifier;
    const path = `/${page.identifier.split('.').slice(-1)[0]}`;
    const Frame = AdminPage(
      LegacyIframe,
      { headerStr: page.name, pageClass: 'legacy-admin-page' },
      { src: link, slug: path, title: page.name }
    );
    return (
      <Route
        key={page.identifier}
        path={path}
        element={
          <Crumb title={page.name}>
            <Frame />
          </Crumb>
        }
      />
    );
  });

  const Main = AdminPage(Portal, { headerStr: T.t('adminPortal.name') }, { adminPages });

  const righteousReactfulRoutes = getAvailableAdminPages()
    .filter(page => !page.pageInfo.href)
    .map(page => {
      const PageComponent = AdminPage(
        page as unknown as React.ComponentType<any>,
        {
          headerStr: T.t(`adminPage.${page.pageInfo.identifier}.name`),
          pageInfo: page.pageInfo,
          pageClass: `adminPage-${page.pageInfo.identifier}`,
        }
      );
      return (
        <Route
          key={page.pageInfo.identifier}
          path={`${page.pageInfo.link}/*`}
          element={
            <Crumb title={T.t(`adminPage.${page.pageInfo.identifier}.name`)}>
              <PageComponent />
            </Crumb>
          }
        />
      );
    });

  const onSessionExpired = () =>
    dispatch(
      setLoPlatform({
        ...store.getState().main.lo_platform,
        user: null,
        adminLink: null,
        logout: 'expired',
      })
    );

  const switcher = (
    <RouteErrorBoundary
      key={location.pathname}
      fallback={
        <Error
          T={T}
          setLastCrumb={() => {}}
          message={T.t('error.unexpectedError')}
        />
      }
    >
      <Routes>
        <Route
          path="/"
          element={<Main />}
        />
        {righteousReactfulRoutes}
        {adminPageRoutes}
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
    </RouteErrorBoundary>
  );

  return (
    <IdleTimer
      onIdler={idler => {
        if (presence.current) {
          presence.current.stop();
          presence.current = null;
        }
        if (idler) {
          presence.current = new PresenceService(idler);
          presence.current.start();
        }
      }}
      element={document}
      onActive={() => dispatch(setIdleState(false))}
      onIdle={() => dispatch(setIdleState(true))}
      timeout={60000}
    >
      {!naked && (
        <>
          <NavigationBar />
          <Breadcrumbs
            className="breadcrumb admin-breadcrumb border-bottom"
            separator="/"
          />
          <AnnouncementBar />
        </>
      )}
      <SessionExpired onExpired={onSessionExpired}>
        <DocumentTytle title={T.t('adminPortal.name')}>
          <Breadcrumb data={{ title: T.t('adminPortal.name'), pathname: '/' }}>
            {switcher}
          </Breadcrumb>
        </DocumentTytle>
      </SessionExpired>
      <div id="lo-copyright">
        <div>
          LO Platform &copy; 2007–2026{' '}
          <a
            id="lo-link"
            href="https://learningobjects.com/"
            target="_blank"
            rel="noopener"
            style={{ color: 'inherit !important' }}
          >
            LO Ventures LLC
          </a>
        </div>
      </div>
    </IdleTimer>
  );
};

export default AdminRoutes;
