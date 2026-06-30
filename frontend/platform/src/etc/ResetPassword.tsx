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
import { useParams } from 'react-router-dom';
import { Col, Form, FormGroup, Row } from 'reactstrap';

import Error from '../components/Error';
import { ContentTypeURLEncoded, trim } from '../services';
import { PasswordResetUrl } from '../services/URLs';
import EtcLoading from './EtcLoading';
import PasswordInput from './loginRegister/PasswordInput';
import { FormInput, FormSubmit } from './loginRegister/Util';

interface Invalid {
  password?: React.ReactNode;
  confirmPassword?: React.ReactNode;
}

type ResetPasswordProps = {
  T: Polyglot;
  setLastCrumb: (crumb: string) => void;
};

const ResetPassword: React.FC<ResetPasswordProps> = ({ T, setLastCrumb }) => {
  const [invalid, setInvalid] = useState<Invalid>({});
  const [submitting, setSubmitting] = useState(false);
  const [userName, setUserName] = useState('');
  const [validating, setValidating] = useState(true);
  const [invalidToken, setInvalidToken] = useState(false);

  const { token = '' } = useParams<{ token: string }>();

  useEffect(() => {
    axios
      .get(`${PasswordResetUrl}?token=${encodeURIComponent(token)}`)
      .then(res => {
        if (res.status === 200) {
          setUserName(res.data.userName);
          setValidating(false);
        } else {
          setValidating(false);
          setInvalidToken(true);
        }
      })
      .catch(() => {
        setValidating(false);
        setInvalidToken(true);
      });
  }, []);

  const onPasswordError = (msg: React.ReactNode) => {
    setSubmitting(false);
    setInvalid({ password: msg });
  };

  const recoverImpl = (password: string) => {
    setSubmitting(true);
    setInvalid({});
    const data = `token=${encodeURIComponent(token)}` + `&password=${encodeURIComponent(password)}`;
    axios
      .post(PasswordResetUrl, data, ContentTypeURLEncoded)
      .then(res => {
        if (res.status === 204) {
          document.location.href = '/';
        } else {
          const {
            data: { reason, messages },
          } = res;
          if (reason === 'InvalidPassword') {
            onPasswordError(messages.map((msg: string, idx: number) => <div key={idx}>{msg}</div>));
          } else {
            onPasswordError(T.t(`error.recovery.${reason}`));
          }
        }
      })
      .catch(() => onPasswordError(T.t('error.recovery.UnknownError')));
  };

  const val = (form: HTMLFormElement, field: string, check: boolean, err: string): boolean => {
    if (check) {
      (form.elements.namedItem(field) as HTMLInputElement).focus();
      setInvalid({ [field]: T.t(`loginRegister.error.${err}`) });
      return false;
    }
    return true;
  };

  const onRecover = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.target as HTMLFormElement;
    const password = trim((form.elements.namedItem('password') as HTMLInputElement).value);
    const confirmPassword = trim(
      (form.elements.namedItem('confirmPassword') as HTMLInputElement).value
    );
    if (
      val(form, 'password', !password, 'required') &&
      val(form, 'confirmPassword', password !== confirmPassword, 'passwordMatch')
    ) {
      recoverImpl(password);
    }
  };

  if (validating) return <EtcLoading />;
  if (invalidToken)
    return (
      <Error
        message={T.t('error.invalidToken')}
        T={T}
        setLastCrumb={setLastCrumb}
      />
    );
  return (
    <div className="container login-form">
      <Row>
        <Col md={{ size: 6, offset: 3 }}>
          <h3 className="mt-4 mb-3">{T.t('page.resetPassword.name')}</h3>
          <Form
            id="reset-password-form"
            autoComplete="off"
            className="admin-form"
            onSubmit={onRecover}
            method="POST"
          >
            <FormGroup>
              <FormInput
                id="reset-username"
                name="userName"
                value={userName}
                label={T.t('resetPassword.userName.label')}
                readOnly
              />
              <PasswordInput
                id="reset-password"
                name="password"
                T={T}
                invalid={invalid.password}
                label={T.t('resetPassword.password.label')}
                setInvalid={password => setInvalid({ password })}
              />
              <FormInput
                id="reset-confirmPassword"
                invalid={invalid.confirmPassword}
                name="confirmPassword"
                label={T.t('resetPassword.confirmPassword.label')}
                type="password"
              />
            </FormGroup>
            <FormSubmit
              id="reset-submit"
              block
              submitting={submitting}
              label={T.t('resetPassword.button.text')}
            />
          </Form>
        </Col>
      </Row>
    </div>
  );
};

export default ResetPassword;
