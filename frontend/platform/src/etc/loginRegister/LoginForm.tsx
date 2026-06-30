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
import classNames from 'classnames';
import Polyglot from 'node-polyglot';
import React, { useEffect, useRef, useState } from 'react';
import { Form, FormFeedback, FormGroup, Input, Label } from 'reactstrap';

import { login, trim } from '../../services';
import { PasswordSettingsUrl } from '../../services/URLs';
import ChangeModal from './ChangeModal';
import RecoverModal from './RecoverModal';
import { FormInput, FormSubmit, redeemAccessCode, validateAccessCode } from './Util';

interface LoginFormProps {
  T: Polyglot;
  allowRedemption?: boolean;
  useEmailAddress?: boolean;
  rememberMe?: boolean;
  title?: React.ReactNode;
  color?: string;
  path?: string;
}

interface Invalid {
  email?: React.ReactNode;
  password?: React.ReactNode;
  accessCode?: React.ReactNode;
}

interface PasswordSettings {
  recovery?: boolean;
}

type ModalKind = 'recover' | 'change' | null;

const LoginForm: React.FC<LoginFormProps> = ({
  T,
  rememberMe,
  useEmailAddress,
  title,
  color,
  path: pathOpt,
  allowRedemption,
}) => {
  const [accessCode, setAccessCode] = useState(false);
  const [pendingAccessCode, setPendingAccessCode] = useState<string | false | null>(null);
  const [invalid, setInvalid] = useState<Invalid>({});
  const [modal, setModal] = useState<ModalKind>(null);
  const [settings, setSettings] = useState<PasswordSettings>({});
  const [submitting, setSubmitting] = useState(false);
  const [token, setToken] = useState<string | null>(null);

  const unel = useRef<HTMLInputElement | null>(null);
  const pwel = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    axios.get(PasswordSettingsUrl).then(res => setSettings(res.data));
  }, []);

  const { recovery } = settings;
  const username = useEmailAddress ? 'email' : 'username';
  const path = pathOpt ?? `${location.pathname}${location.search}${location.hash}`;

  const toggleModal = (m: ModalKind) => (e?: React.SyntheticEvent) => {
    e && e.preventDefault && e.preventDefault();
    setModal(m);
  };

  const onAccessCodeCheck = (e: React.ChangeEvent<HTMLInputElement>) =>
    setAccessCode(e.target.checked);

  const val = (field: keyof Invalid, check: boolean, err: string): boolean => {
    if (check) {
      setInvalid({ [field]: T.t(`loginRegister.error.${err}`) });
      return false;
    }
    return true;
  };

  const onLoginError = (reason: string) => {
    setSubmitting(false);
    setInvalid({ password: T.t(`error.login.${reason}`) });
  };

  const onAccessCodeError = (reason: string) => {
    setSubmitting(false);
    setInvalid({ accessCode: T.t(`error.accessCode.${reason}`) });
  };

  const redeemImpl = (code: string, form: HTMLFormElement) =>
    validateAccessCode(code)
      .then(res => {
        if (res.data === 'Valid') {
          redeemAccessCode(code)
            .then(() => form.submit())
            .catch(() => onAccessCodeError('UnknownError'));
        } else if (res.data === 'Redeemed') {
          form.submit();
        } else {
          // Inapplicable | Invalid
          onAccessCodeError(res.data);
        }
      })
      .catch(() => onAccessCodeError('UnknownError'));

  const loginImpl = (
    email: string,
    password: string,
    code: string | false,
    remember: boolean,
    form: HTMLFormElement
  ) => {
    setInvalid({});
    setSubmitting(true);
    login(email, password, remember, false)
      .then(response => {
        if (response.status === 202) {
          const {
            data: { reason, token: respToken },
          } = response;
          if (reason === 'PasswordExpired') {
            setModal('change');
            setToken(respToken);
            setPendingAccessCode(code);
            setSubmitting(false);
          } else {
            // TooManyFailedLogins has a message...
            onLoginError(reason);
          }
        } else {
          if (code) {
            redeemImpl(code, form);
          } else if ((window as any).isDev) {
            document.location.reload();
          } else {
            form.submit();
          }
        }
      })
      .catch(() => onLoginError('UnknownError'));
  };

  const onPasswordChanged = (password: string, form: HTMLFormElement) => {
    if (pendingAccessCode) {
      if (pwel.current) pwel.current.value = password;
      setModal(null);
      redeemImpl(pendingAccessCode, form);
    } else {
      form.submit();
    }
  };

  const doLogin = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.target as HTMLFormElement;
    const email = trim((form.elements.namedItem('username') as HTMLInputElement).value);
    const password = trim((form.elements.namedItem('password') as HTMLInputElement).value);
    const rememberEl = form.elements.namedItem('remember') as HTMLInputElement | null;
    const remember = !!(rememberEl && rememberEl.checked);
    const acCheckEl = form.elements.namedItem('accessCodeCheck') as HTMLInputElement | null;
    const acChecked = !!(acCheckEl && acCheckEl.checked);
    const code =
      acChecked && trim((form.elements.namedItem('accessCode') as HTMLInputElement).value);
    if (
      val('email', !email, 'required') &&
      val('password', !password, 'required') &&
      val('accessCode', acChecked && !code, 'required')
    ) {
      loginImpl(email, password, code, remember, form);
    }
  };

  const renderAccessCodeInput = () => {
    const { accessCode: invalidAccessCode } = invalid;
    return (
      allowRedemption && (
        <React.Fragment>
          <div className="form-check">
            <Input
              id="login-accessCode-check"
              name="accessCodeCheck"
              type="checkbox"
              className="mt-2"
              onChange={onAccessCodeCheck}
            />
            <Label
              check
              id="login-accessCode-label"
              for="login-accessCode-check"
              className="mt-1 super-label"
            >
              {T.t('loginRegister.login.accessCode.revealer')}
            </Label>
          </div>
          <Input
            id="login-accessCode"
            className={classNames({ 'is-invalid': invalidAccessCode })}
            title={T.t('loginRegister.login.accessCode.label')}
            style={{ display: accessCode ? 'block' : 'none' }}
            type="text"
            name="accessCode"
          />
          {accessCode && invalidAccessCode && (
            <FormFeedback
              style={{ display: 'block' }}
              id="login-accessCode-problem"
            >
              {invalidAccessCode}
            </FormFeedback>
          )}
        </React.Fragment>
      )
    );
  };

  return (
    <React.Fragment>
      {title && modal !== 'change' && <h3>{title}</h3>}
      {modal !== 'change' && (
        <Form
          id="login-form"
          onSubmit={doLogin}
          method="POST"
          action="/api/v2/sessions/loginRedirect"
        >
          <FormGroup>
            <FormInput
              autoComplete="username"
              id="login-email"
              invalid={invalid.email}
              name="username"
              label={T.t(`loginRegister.login.${username}.label`)}
              onChange={() => invalid.email && setInvalid({})}
              innerRef={unel}
            />
            <FormInput
              autoComplete="current-password"
              id="login-password"
              invalid={invalid.password}
              name="password"
              type="password"
              label={T.t('loginRegister.login.password.label')}
              onChange={() => invalid.password && setInvalid({})}
              innerRef={pwel}
            />
            <Input
              type="hidden"
              name="path"
              value={path}
            />
            {renderAccessCodeInput()}
          </FormGroup>
          <FormSubmit
            id="login-submit"
            submitting={submitting}
            color={color}
            block
            label={T.t('loginRegister.login.submit.text')}
          />
          {rememberMe ? (
            <div className="mb-1 mt-4 d-flex flex-column flex-sm-row align-items-center justify-content-between">
              <FormGroup switch>
                <Input
                  type="switch"
                  role="switch"
                  id="login-remember"
                  name="remember"
                />
                <Label
                  check
                  for="login-remember"
                >
                  {T.t('loginRegister.login.keepMeLoggedIn')}
                </Label>
              </FormGroup>
              <a
                id="forgot-password-link"
                className="forgot-link2 mt-3 mt-sm-0"
                href=""
                onClick={recovery ? toggleModal('recover') : undefined}
                style={{ opacity: recovery ? 1 : 0, transition: 'opacity 0.5s ease-out' }}
              >
                {T.t('loginRegister.login.forgotPassword')}
              </a>
            </div>
          ) : (
            <a
              id="forgot-password-link"
              className="mt-3 forgot-link"
              href=""
              onClick={recovery ? toggleModal('recover') : undefined}
              style={{ opacity: recovery ? 1 : 0, transition: 'opacity 0.5s ease-out' }}
            >
              {T.t('loginRegister.login.forgotPassword')}
            </a>
          )}
        </Form>
      )}
      {modal === 'recover' && (
        <RecoverModal
          T={T}
          onClose={toggleModal(null)}
        />
      )}
      {modal === 'change' && (
        <ChangeModal
          T={T}
          onSuccess={onPasswordChanged}
          username={unel.current?.value ?? ''}
          path={path}
          token={token ?? ''}
          color={color}
        />
      )}
    </React.Fragment>
  );
};

export default LoginForm;
