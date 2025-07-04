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
import PropTypes from 'prop-types';
import React from 'react';

import NavigationBar from '../components/navigationBar';
import LoginForm from '../etc/loginRegister/LoginForm';
import { LogoutUrl } from '../services/URLs';
import DocumentTytle from 'react-document-title';
import classnames from 'classnames';

class LoginRequired extends React.Component {
  state = {
    phase: 0,
    opacityPrime: 0,
  };

  componentDidMount() {
    const {
      lo_platform: { loggedOut },
    } = this.props;
    document.body.classList.add('login-page');
    if (loggedOut) {
      document.body.addEventListener('click', this.onClick);
      document.body.addEventListener('keypress', this.onKey);
      document.body.classList.add('logged-out');
      axios.delete(LogoutUrl);
    }
  }

  componentWillUnmount() {
    document.body.removeEventListener('click', this.onClick);
    document.body.removeEventListener('keypress', this.onKey);
    document.body.classList.remove('logged-out', 'login-page');
  }

  onClick = e => {
    const {
      props: {
        lo_platform: { loggedOut },
      },
      state: { phase },
    } = this;
    if (loggedOut && !phase) {
      e && e.preventDefault && e.preventDefault();
      this.setState({ phase: 1 });
      setTimeout(() => this.setState({ phase: 2 }), 500);
    }
  };

  onKey = e => {
    if (e.key === ' ' || e.key === 'Enter') {
      this.onClick(e);
    }
  };

  render() {
    const {
      props: {
        T,
        lo_platform: {
          domain: { name },
          loggedOut,
        },
      },
      state: { phase, opacityPrime },
    } = this;
    const login = !loggedOut || phase >= 2;
    const domainLogin = login && window.location.pathname === '/';
    const title = T.t(
      domainLogin
        ? 'error.domainLogin'
        : login
          ? 'error.loginRequired'
          : 'adminPortal.loggedOut.message.logout'
    );
    return (
      <div
        id="login-required"
        className="login-required"
      >
        <DocumentTytle title={`${name} - ${title}`} />
        {login ? (
          <div
            className="login-form dark"
            style={{ opacity: loggedOut ? opacityPrime : 1 }}
            ref={() => !opacityPrime && setTimeout(() => this.setState({ opacityPrime: 1 }), 0)}
          >
            <LoginForm
              T={T}
              title={domainLogin || login ? name : title}
              color="dark"
              rememberMe
            />
          </div>
        ) : loggedOut ? (
          <div className={classnames("login-form logged-out p-3", phase && 'blur')}>
            <h3
              className="logout-hdr logout-logout mt-0"
              aria-hidden={!!phase}
            >
              {T.t(`adminPortal.loggedOut.message.logout`)}
            </h3>
            <a
              href=""
              onClick={this.onClick}
              className="click-to-login"
            >
              {T.t('adminPortal.loggedOut.clickToLogin')}
            </a>
          </div>
        ) :null}
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
    );
  }
}

LoginRequired.propTypes = {
  T: PropTypes.object.isRequired,
  lo_platform: PropTypes.object.isRequired,
  setLoPlatform: PropTypes.func,
};

export default LoginRequired;
