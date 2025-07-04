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

import axios from 'axios';
import classnames from 'classnames';
import Polyglot from 'node-polyglot';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Redirect, Route, Switch, withRouter } from 'react-router-dom';
import { bindActionCreators } from 'redux';

import AlertBar from '../components/AlertBar';
import * as MainActions from '../redux/actions/MainActions';
import { getPlatform, getTranslations, isDevelopment } from '../services';
import { NoSessionExtensionHdr, UserIdHdr } from '../services/Headers';
import { hasRight } from '../services/Rights';
import Login from './Login';
import OverlordBar from './OverlordBar';
import allPages from './pages';
import Portal from './Portal';

class AdminPage extends React.Component {
  constructor() {
    super();
    this.state = {
      show: false,
    };
  }

  componentDidMount() {
    const { setPage, page } = this.props;
    setTimeout(() => this.setState({ show: true }), 0);
    setPage(page);
  }

  componentWillUnmount() {
    const { setPage } = this.props;
    setPage(null);
  }

  render() {
    const { show } = this.state;
    const { children } = this.props;
    return (
      <div
        id="overlord-page"
        className={classnames('fade', { show })}
      >
        {children}
      </div>
    );
  }
}

class LegacyPage extends React.Component {
  constructor() {
    super();
    this.state = {
      show: false,
    };
  }

  componentDidMount() {
    const { setPage, page } = this.props;
    setPage(page);
    this.ifr.onload = () => {
      this.setState({ show: true });
    };
  }

  componentWillUnmount() {
    const { setPage } = this.props;
    setPage(null);
  }

  render() {
    const { show } = this.state;
    const { href } = this.props;
    return (
      <iframe
        id="overlord-frame"
        title="legacy"
        className={classnames('fade', { show })}
        src={href}
        ref={f => (this.ifr = f)}
      ></iframe>
    );
  }
}

class Routes extends React.Component {
  constructor() {
    super();
    this.state = {
      loaded: false,
      fadeIn: false,
      page: null,
      loggedOut: false,
    };
  }

  componentDidMount() {
    if (window.location !== window.parent.location) {
      window.top.location = '/';
    } else {
      axios
        .all([getPlatform(), getTranslations(window.locale)])
        .then(([loPlatformRes, translationsRes]) => {
          this.props.setLoPlatform(loPlatformRes.data);
          this.props.setTranslations(
            new Polyglot({
              locale: loPlatformRes.data.domain.locale,
              phrases: translationsRes.data,
            })
          );
          this.setState({ loaded: true, lo_platform: loPlatformRes.data });
          setTimeout(() => this.setState({ fadeIn: true }), 300);
          this.setInterval();
        });
    }
  }

  componentWillUnmount() {
    if (this.interval) {
      clearTimeout(this.interval);
    }
  }

  setInterval = () => {
    const expired = loggedOut => this.setState({ loggedOut });
    const pollSession = () => {
      delete this.interval;
      const {
        lo_platform: { user },
      } = this.props;
      if (user) {
        axios
          .get('/api/v0/session', {
            hideProgress: true,
            headers: { [NoSessionExtensionHdr]: 'true', [UserIdHdr]: user.id },
          })
          .then(response => {
            expired(!response.data.valid);
            this.setInterval();
          })
          .catch(error => {
            if (error.response && error.response.status < 500) {
              // ignore server restart/...
              expired(true);
            }
            this.setInterval();
          });
      } else {
        this.setInterval();
      }
    };
    this.interval = setTimeout(pollSession, isDevelopment ? 15000 : 30000);
  };

  setPage = page => {
    this.props.setPortalAlertStatus(false, false, null);
    this.setState({ page });
  };

  render() {
    const { loaded, fadeIn, page, loggedOut } = this.state;
    if (!loaded) return null;

    const {
      history,
      lo_platform: { user, clusterType },
    } = this.props;

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
        <OverlordBar
          page={page}
          history={history}
        />
        <AlertBar />
        {loggedOut ? (
          <div id="session-expired">Session Ended</div>
        ) : !user ? (
          <Login />
        ) : (
          <Switch>
            <Route
              exact
              path="/"
              render={() => (
                <AdminPage
                  page={null}
                  setPage={this.setPage}
                >
                  <Portal history={history} />
                </AdminPage>
              )}
            />
            {pages.map(({ route, identifier, embed, Page }) => (
              <Route
                key={identifier}
                path={route || `/${identifier}`}
                render={p =>
                  Page ? (
                    <AdminPage
                      page={identifier}
                      setPage={this.setPage}
                    >
                      <Page {...p} />
                    </AdminPage>
                  ) : (
                    <LegacyPage
                      page={identifier}
                      setPage={this.setPage}
                      href={embed}
                    />
                  )
                }
              />
            ))}
            <Route render={() => <Redirect to="/" />} />
          </Switch>
        )}
      </div>
    );
  }
}

Routes.propTypes = {
  setLoPlatform: PropTypes.func.isRequired,
  setTranslations: PropTypes.func.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
  history: PropTypes.object,
  lo_platform: PropTypes.object,
  T: PropTypes.shape({
    /* translations.func here can be transiently undefined, sadly. */
    t: PropTypes.func,
  }),
};

function mapStateToProps(state) {
  return {
    T: state.main.translations,
    lo_platform: state.main.lo_platform,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators({ ...MainActions }, dispatch);
}

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Routes));
