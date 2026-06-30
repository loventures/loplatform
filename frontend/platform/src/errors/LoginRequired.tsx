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
import React, { useCallback, useEffect, useState } from 'react';
import DocumentTytle from 'react-document-title';

import LoginForm from '../etc/loginRegister/LoginForm';
import { LogoutUrl } from '../services/URLs';
import { LoPlatform } from '../types/loPlatform';

interface LoginRequiredProps {
  T: Polyglot;
  lo_platform: LoPlatform;
  setLoPlatform?: (lop: LoPlatform) => void;
}

const LoginRequired: React.FC<LoginRequiredProps> = ({ T, lo_platform }) => {
  const [phase, setPhase] = useState(0);
  const [opacityPrime, setOpacityPrime] = useState(0);

  const loggedOut = lo_platform.loggedOut as boolean | undefined;

  const onClick = useCallback(
    (e?: Event | React.SyntheticEvent) => {
      if (loggedOut && !phase) {
        e && (e as Event).preventDefault && (e as Event).preventDefault();
        setPhase(1);
        setTimeout(() => setPhase(2), 500);
      }
    },
    [loggedOut, phase]
  );

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === ' ' || e.key === 'Enter') {
        onClick(e);
      }
    };
    document.body.classList.add('login-page');
    if (loggedOut) {
      document.body.addEventListener('click', onClick);
      document.body.addEventListener('keypress', onKey);
      document.body.classList.add('logged-out');
      axios.delete(LogoutUrl);
    }
    return () => {
      document.body.removeEventListener('click', onClick);
      document.body.removeEventListener('keypress', onKey);
      document.body.classList.remove('logged-out', 'login-page');
    };
  }, [loggedOut, onClick]);

  const { name } = lo_platform.domain;
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
          ref={() => {
            !opacityPrime && setTimeout(() => setOpacityPrime(1), 0);
          }}
        >
          <LoginForm
            T={T}
            title={domainLogin || login ? name : title}
            color="dark"
            rememberMe
          />
        </div>
      ) : loggedOut ? (
        <div className={classnames('login-form logged-out p-3', phase && 'blur')}>
          <h3
            className="logout-hdr logout-logout mt-0"
            aria-hidden={!!phase}
          >
            {T.t(`adminPortal.loggedOut.message.logout`)}
          </h3>
          <a
            href=""
            onClick={onClick}
            className="click-to-login"
          >
            {T.t('adminPortal.loggedOut.clickToLogin')}
          </a>
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
  );
};

export default LoginRequired;
