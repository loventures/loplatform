/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
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

/* eslint-disable react/jsx-no-target-blank */

import axios from 'axios';
import { isEqual } from 'lodash';
import Polyglot from 'node-polyglot';
import PropTypes from 'prop-types';
import React from 'react';
import { Breadcrumbs } from 'react-breadcrumbs';
import DocumentTytle from 'react-document-title';
import IdleTimer from 'react-idle-timer';
import { connect } from 'react-redux';
import { Redirect, Route, Switch, withRouter } from 'react-router-dom';
import { bindActionCreators } from 'redux';

import AdminPage from '../components/adminPage';
import AnnouncementBar from '../components/announcementBar';
import CrumbRoute from '../components/crumbRoute';
import Error from '../components/Error';
import LegacyIframe from '../components/legacyIframe';
import NavigationBar from '../components/navigationBar';
import SessionExpired from '../components/sessionExpired';
import LoginForm from '../etc/loginRegister/LoginForm';
import LoPropTypes from '../react/loPropTypes';
import * as AnnouncementActions from '../redux/actions/AnnouncementActions';
import * as MainActions from '../redux/actions/MainActions';
import * as PresenceActions from '../redux/actions/PresenceActions';
import { getAdminPages, getAnnouncements, getPlatform, getTranslations } from '../services/';
import { PresenceService } from '../services/Presence';
import getAvailableAdminPages from './pages';
import Portal from './Portal';
import { Navbar } from 'reactstrap';

const FakeCrumb = ({ title }) => (
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

const AdminWait = () => (
  <Navbar
    id="main-nav-bar-base"
    light
    expand
    className="navbar-toggleable-xl px-3"
    style={{ height: '3.5rem' }}
  />
);

const AdminWaitPage = ({ naked, title }) => (
  <React.Fragment>
    {!naked && <NavigationBar />}
    <DocumentTytle title={title} />
  </React.Fragment>
);

class AdminLoginPage extends React.Component {
  state = {
    phase: 0,
  };

  componentDidMount() {
    const { logout } = this.props;
    if (logout) {
      if (logout === 'logout') {
        setTimeout(this.onClick, 2500);
      }
      document.body.addEventListener('click', this.onClick);
    }
  }

  componentWillUnmount() {
    const { logout } = this.props;
    if (logout) {
      document.body.removeEventListener('click', this.onClick);
    }
  }

  onClick = () => {
    const {
      props: { logout },
      state: { phase },
    } = this;
    if (logout && !phase) {
      this.setState({ phase: 1 });
      setTimeout(() => this.setState({ phase: 2 }), 500);
    }
  };

  render() {
    const {
      props: { T, logout, naked },
      state: { phase },
    } = this;

    const login = !logout || phase >= 2;
    const msg = login ? 'adminPortal.login.title' : `adminPortal.logout.title.${logout}`;
    return (
      <React.Fragment>
        <div
          id="admin-login-page"
          className="admin-login"
        >
          {!naked && <NavigationBar noFade={logout} />}
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
              LO Platform &copy; 2007–2025{' '}
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
  }
}

class Routes extends React.Component {
  constructor() {
    super();
    this.state = {
      adminPages: {},
      loaded: false,
      error: null,
      login: false,
    };
  }

  shouldComponentUpdate(nextProps, nextState) {
    const { lo_platform: nextPlatform, ...nextRest } = nextProps;
    const { lo_platform, ...rest } = this.props;
    // prevent re-loading lo_platform from re-rendering everything
    return (
      !isEqual(nextState, this.state) ||
      !isEqual(nextRest, rest) ||
      !isEqual(nextPlatform.user, lo_platform.user) ||
      !isEqual(nextPlatform.logout, lo_platform.logout)
    );
  }

  componentDidMount() {
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
        this.props.setLoPlatform(lo_platform);
        this.props.setTranslations(translations);
        if (lo_platform.user) {
          return axios
            .all([getAdminPages(), getAnnouncements()])
            .then(([adminPagesRes, announcementsRes]) => {
              this.props.setAnnouncements(announcementsRes.data.objects);
              this.setState({ adminPages: adminPagesRes.data.adminPages, loaded: true });
              document.getElementById('admin-loading')?.remove();
              /* once we've mounted, summon the lazy modules in the background */
              // noinspection JSIgnoredPromiseFromCall
              Promise.all([
                import('../announcements/App'),
                import('../component/App'),
                import('../config/config'),
                import('../jobs/App'),
              ]);
            })
            .catch(e => {
              console.log(e);
              if (e.request && e.request.status === 403) {
                if (this.state.login) {
                  document.location = '/'; // redirect a non-ahmin login
                } else {
                  this.setState({ loaded: true, error: translations.t('error.accessDenied') });
                }
              } else {
                this.setState({ loaded: true, error: translations.t('error.unexpectedError') });
              }
            });
        } else {
          document.getElementById('admin-loading')?.remove();
          this.setState({ loaded: true });
        }
      });
  }

  render() {
    const {
      state: { error, loaded },
      props: {
        location,
        translations: T,
        lo_platform: { user, logout },
      },
    } = this;

    const naked = location.search && location.search.indexOf('naked') >= 0;

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
          logout={logout}
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

    const adminPages = Object.keys(this.state.adminPages).reduce((array, key) => {
      const pages = this.state.adminPages[key].map(page => {
        page.key = key;
        return page;
      });
      return array.concat(pages);
    }, []);

    const adminPageRoutes = adminPages.map(page => {
      const link = '/sys/admin/' + page.identifier;
      const path = `/${page.identifier.split('.').slice(-1)[0]}`;
      const frame = AdminPage(
        LegacyIframe,
        { headerStr: page.name, pageClass: 'legacy-admin-page' },
        { src: link, slug: path, title: page.name }
      );
      return (
        <CrumbRoute
          key={page.identifier}
          path={path}
          component={frame}
          title={page.name}
        />
      );
    });

    const Main = AdminPage(
      Portal,
      { headerStr: T.t('adminPortal.name') },
      { adminPages: this.state.adminPages }
    );

    const righteousReactfulRoutes = getAvailableAdminPages()
      .filter(page => !page.pageInfo.href)
      .map(page => {
        return (
          <CrumbRoute
            key={page.pageInfo.identifier}
            path={page.pageInfo.link}
            title={T.t(`adminPage.${page.pageInfo.identifier}.name`)}
            component={AdminPage(
              page,
              {
                headerStr: T.t(`adminPage.${page.pageInfo.identifier}.name`),
                pageInfo: page.pageInfo,
                pageClass: `adminPage-${page.pageInfo.identifier}`,
              },
              { history: this.props.history }
            )}
          />
        );
      });

    const onSessionExpired = () =>
      this.props.setLoPlatform({
        ...this.props.lo_platform,
        user: null,
        adminLink: null,
        logout: 'expired',
      });

    const switcher = () => (
      <Switch>
        <Route
          exact
          path="/"
          component={Main}
        />
        {righteousReactfulRoutes}
        {adminPageRoutes}
        <Route render={() => <Redirect to="/" />} />
      </Switch>
    );

    return (
      <IdleTimer
        ref={idler => {
          if (this.presence) {
            this.presence.stop();
            delete this.presence;
          }
          if (idler) {
            this.presence = new PresenceService(idler);
            this.presence.start();
          }
        }}
        element={document}
        activeAction={() => this.props.setIdleState(false)}
        idleAction={() => this.props.setIdleState(true)}
        timeout={60000}
        format="MM-DD-YYYY HH:MM:ss.SSS"
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
          <CrumbRoute
            title={T.t('adminPortal.name')}
            path="/"
            render={switcher}
          />
        </SessionExpired>
        <div id="lo-copyright">
          <div>
            LO Platform &copy; 2007–2025{' '}
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
  }
}

Routes.propTypes = {
  history: PropTypes.object,
  lo_platform: PropTypes.object,
  setAnnouncements: PropTypes.func,
  setLoPlatform: PropTypes.func,
  setTranslations: PropTypes.func,
  translations: LoPropTypes.translationsOpt,
};

const mapStateToProps = state => ({
  lo_platform: state.main.lo_platform,
  translations: state.main.translations,
});

const mapDispatchToProps = dispatch =>
  bindActionCreators({ ...MainActions, ...PresenceActions, ...AnnouncementActions }, dispatch);

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Routes));
