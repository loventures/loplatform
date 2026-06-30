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
import React, { useEffect, useState } from 'react';
import { Col, Row } from 'reactstrap';

import { LoPlatform } from '../../types/loPlatform';
import { SelfRegistrationUrl } from '../../services/URLs';
import EtcLoading from '../EtcLoading';
import LoginForm from './LoginForm';
import RegisterForm from './RegisterForm';

interface LoginRegisterProps {
  lop: LoPlatform;
  T: Polyglot;
}

const LoginRegister: React.FC<LoginRegisterProps> = ({ lop, T }) => {
  const [loaded, setLoaded] = useState(false);
  const [enabled, setEnabled] = useState(false);

  const { user } = lop;

  useEffect(() => {
    if (user) {
      document.location.href = '/';
    } else {
      axios.get(SelfRegistrationUrl).then(res => {
        setLoaded(true);
        setEnabled(res.data.enabled);
      });
    }
  }, []);

  return user || !loaded ? (
    <EtcLoading />
  ) : (
    <div className="container login-register login-form mb-4">
      <Row>
        <React.Fragment>
          <Col md={{ offset: enabled ? 0 : 3, size: 6 }}>
            <LoginForm
              T={T}
              useEmailAddress
              allowRedemption
              title={T.t('loginRegister.title.loginRedeem')}
              path="/"
            />
          </Col>
          {enabled && (
            <Col md={6}>
              <RegisterForm T={T} />
            </Col>
          )}
        </React.Fragment>
      </Row>
    </div>
  );
};

export default LoginRegister;
