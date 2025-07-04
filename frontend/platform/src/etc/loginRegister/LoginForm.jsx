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
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Form, FormFeedback, FormGroup, Input, Label } from 'reactstrap';

import { login, trim } from '../../services';
import { PasswordSettingsUrl } from '../../services/URLs';
import ChangeModal from './ChangeModal';
import RecoverModal from './RecoverModal';
import { FormInput, FormSubmit, redeemAccessCode, validateAccessCode } from './Util';

class LoginForm extends React.Component {
  state = {
    accessCode: false,
    pendingAccessCode: null,
    invalid: {},
    modal: null,
    settings: {},
    submitting: false,
    token: null,
  };

  componentDidMount() {
    axios.get(PasswordSettingsUrl).then(res => this.setState({ settings: res.data }));
  }

  render() {
    const { T, rememberMe, useEmailAddress, title, color, path: pathOpt } = this.props;
    const {
      invalid,
      modal,
      submitting,
      settings: { recovery },
      token,
    } = this.state;
    const username = useEmailAddress ? 'email' : 'username';
    const path = pathOpt ?? `${location.pathname}${location.search}${location.hash}`;
    return (
      <React.Fragment>
        {title && modal !== 'change' && <h3>{title}</h3>}
        {modal !== 'change' && (
          <Form
            id="login-form"
            onSubmit={this.doLogin}
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
                onChange={() => invalid.email && this.setState({ invalid: {} })}
                innerRef={el => (this.unel = el)}
              />
              <FormInput
                autoComplete="current-password"
                id="login-password"
                invalid={invalid.password}
                name="password"
                type="password"
                label={T.t('loginRegister.login.password.label')}
                onChange={() => invalid.password && this.setState({ invalid: {} })}
                innerRef={el => (this.pwel = el)}
              />
              <Input
                type="hidden"
                name="path"
                value={path}
              />
              {this.renderAccessCodeInput(T)}
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
                  onClick={recovery ? this.toggleModal('recover') : null}
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
                onClick={recovery ? this.toggleModal('recover') : null}
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
            onClose={this.toggleModal(null)}
          />
        )}
        {modal === 'change' && (
          <ChangeModal
            T={T}
            onSuccess={this.onPasswordChanged}
            username={this.unel.value}
            path={path}
            token={token}
            color={color}
          />
        )}
      </React.Fragment>
    );
  }

  toggleModal = modal => e => {
    e && e.preventDefault && e.preventDefault();
    this.setState({ modal });
  };

  renderAccessCodeInput = T => {
    const {
      accessCode,
      invalid: { accessCode: invalid },
    } = this.state;
    const { allowRedemption } = this.props;
    return (
      allowRedemption && (
        <React.Fragment>
          <div className="form-check">
            <Input
              id="login-accessCode-check"
              name="accessCodeCheck"
              type="checkbox"
              className="mt-2"
              onChange={this.onAccessCodeCheck}
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
            className={classNames({ 'is-invalid': invalid })}
            title={T.t('loginRegister.login.accessCode.label')}
            style={{ display: accessCode ? 'block' : 'none' }}
            type="text"
            name="accessCode"
          />
          {accessCode && invalid && (
            <FormFeedback
              style={{ display: 'block' }}
              id="login-accessCode-problem"
            >
              {invalid}
            </FormFeedback>
          )}
        </React.Fragment>
      )
    );
  };

  onAccessCodeCheck = e => this.setState({ accessCode: e.target.checked });

  onPasswordChanged = (password, form) => {
    const { pendingAccessCode } = this.state;
    if (pendingAccessCode) {
      this.pwel.value = password;
      this.setState({ modal: null });
      this.redeemImpl(pendingAccessCode, form);
    } else {
      form.submit();
    }
  };

  doLogin = e => {
    e.preventDefault();
    const form = e.target;
    const email = trim(form.elements.username.value);
    const password = trim(form.elements.password.value);
    const remember = form.elements.remember && form.elements.remember.checked;
    const acChecked = form.elements.accessCodeCheck && form.elements.accessCodeCheck.checked;
    const accessCode = acChecked && trim(form.elements.accessCode.value);
    if (
      this.val('email', !email, 'required') &&
      this.val('password', !password, 'required') &&
      this.val('accessCode', acChecked && !accessCode, 'required')
    ) {
      this.loginImpl(email, password, accessCode, remember, form);
    }
  };

  loginImpl = (email, password, accessCode, remember, form) => {
    this.setState({ invalid: {}, submitting: true });
    login(email, password, remember)
      .then(response => {
        if (response.status === 202) {
          const {
            data: { reason, token },
          } = response;
          if (reason === 'PasswordExpired') {
            this.setState({
              modal: 'change',
              token,
              pendingAccessCode: accessCode,
              submitting: false,
            });
          } else {
            // TooManyFailedLogins has a message...
            this.onLoginError(reason);
          }
        } else {
          if (accessCode) {
            this.redeemImpl(accessCode, form);
          } else if (window.isDev) {
            document.location.reload();
          } else {
            form.submit();
          }
        }
      })
      .catch(() => this.onLoginError('UnknownError'));
  };

  redeemImpl = (accessCode, form) =>
    validateAccessCode(accessCode)
      .then(res => {
        if (res.data === 'Valid') {
          redeemAccessCode(accessCode)
            .then(() => form.submit())
            .catch(() => this.onAccessCodeError('UnknownError'));
        } else if (res.data === 'Redeemed') {
          form.submit();
        } else {
          // Inapplicable | Invalid
          this.onAccessCodeError(res.data);
        }
      })
      .catch(() => this.onAccessCodeError('UnknownError'));

  // stuffs

  val = (field, check, err) => {
    if (check) {
      const { T } = this.props;
      this.setState({ invalid: { [field]: T.t(`loginRegister.error.${err}`) } });
    } else {
      return true;
    }
  };

  onLoginError = reason => {
    const { T } = this.props;
    const invalid = { password: T.t(`error.login.${reason}`) };
    this.setState({ submitting: false, invalid });
  };

  onAccessCodeError = reason => {
    const { T } = this.props;
    const invalid = { accessCode: T.t(`error.accessCode.${reason}`) };
    this.setState({ submitting: false, invalid });
  };
}

LoginForm.propTypes = {
  T: PropTypes.object.isRequired,
  allowRedemption: PropTypes.bool,
  useEmailAddress: PropTypes.bool,
  rememberMe: PropTypes.bool,
  title: PropTypes.any,
  color: PropTypes.string,
  path: PropTypes.string,
};

export default LoginForm;
