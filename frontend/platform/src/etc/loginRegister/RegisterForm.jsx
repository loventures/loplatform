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
import PropTypes from 'prop-types';
import React from 'react';
import { Form, FormGroup } from 'reactstrap';

import { EmailRE, trim } from '../../services';
import { lojax } from '../../services/lojax';
import { RegisterRedeemUrl, UsersSelfUrl } from '../../services/URLs';
import PasswordInput from './PasswordInput';
import { FormInput, FormSubmit, Schema, validateAccessCode } from './Util';

class RegisterForm extends React.Component {
  state = {
    invalid: {},
    submitting: false,
  };

  render() {
    const { T } = this.props;
    const { invalid, submitting } = this.state;
    return (
      <React.Fragment>
        <h3>{T.t('loginRegister.title.registerRedeem')}</h3>
        <Form
          id="register-form"
          onSubmit={this.doRegister}
          method="POST"
          action="/api/v2/sessions/loginRedirect"
        >
          <FormGroup>
            <FormInput
              id="register-givenName"
              invalid={invalid.givenName}
              name="givenName"
              autoComplete="given-name"
              label={T.t('loginRegister.register.givenName.label')}
            />
            <FormInput
              id="register-familyName"
              invalid={invalid.familyName}
              name="familyName"
              autoComplete="family-name"
              label={T.t('loginRegister.register.familyName.label')}
            />
            <FormInput
              autoComplete="username"
              id="register-email"
              invalid={invalid.email}
              name="username"
              label={T.t('loginRegister.register.email.label')}
            />
            <PasswordInput
              id="register-password"
              invalid={invalid.password}
              name="password"
              T={T}
              label={T.t('loginRegister.register.password.label')}
              setInvalid={password => this.setState({ invalid: { password } })}
            />
            <FormInput
              autoComplete="new-password"
              id="register-confirmPassword"
              invalid={invalid.confirmPassword}
              name="confirmPassword"
              label={T.t('loginRegister.register.confirmPassword.label')}
              type="password"
            />
            <FormInput
              id="register-accessCode"
              invalid={invalid.accessCode}
              name="accessCode"
              label={T.t('loginRegister.register.accessCode.label')}
            />
          </FormGroup>
          <FormSubmit
            id="register-submit"
            submitting={submitting}
            block
            label={T.t('loginRegister.register.submit.text')}
          />
        </Form>
      </React.Fragment>
    );
  }

  doRegister = e => {
    e.preventDefault();
    const form = e.target;
    const givenName = trim(form.elements.givenName.value);
    const familyName = trim(form.elements.familyName.value);
    const email = trim(form.elements.username.value);
    const password = trim(form.elements.password.value);
    const confirmPassword = trim(form.elements.confirmPassword.value);
    const accessCode = trim(form.elements.accessCode.value);
    if (
      this.val('email', !email, 'required') &&
      this.val('email', !EmailRE.test(email), 'validEmail') &&
      this.val('password', !password, 'required') &&
      this.val('confirmPassword', password !== confirmPassword, 'passwordMatch') &&
      this.val('accessCode', !accessCode, 'required')
    ) {
      this.registerImpl(givenName, familyName, email, password, accessCode, form);
    }
  };

  registerImpl = (givenName, familyName, emailAddress, password, accessCode, form) => {
    this.setState({ invalid: {}, submitting: true });
    validateAccessCode(accessCode)
      .then(res => {
        if (res.data === 'Valid') {
          this.registerRedeem(givenName, familyName, emailAddress, password, accessCode, form);
        } else {
          this.onAccessCodeError(res.data);
        }
      })
      .catch(() => this.onAccessCodeError('UnknownError'));
  };

  registerRedeem = (givenName, familyName, emailAddress, password, accessCode, form) => {
    const data = {
      givenName,
      familyName,
      emailAddress,
      password,
      userName: emailAddress,
    };
    const query = `?accessCode=${encodeURIComponent(accessCode)}` + `&accessCodeSchema=${Schema}`;
    lojax({
      method: 'post',
      url: `${RegisterRedeemUrl}${query}`,
      data: data,
    })
      .then(res => {
        const { T } = this.props;
        if (res.status === 202) {
          const {
            data: { reason, messages },
          } = res;
          if (reason === 'DuplicateUser') {
            const invalid = { email: T.t(`loginRegister.error.duplicateUser`) };
            this.setState({ submitting: false, invalid });
          } else if (reason === 'InvalidPassword') {
            const invalid = { password: messages.map((msg, idx) => <div key={idx}>{msg}</div>) };
            this.setState({ submitting: false, invalid });
          } else {
            // InvalidAccessCode
            this.onAccessCodeError('UnknownError');
          }
        } else {
          const { data: userId } = res;
          axios
            .get(UsersSelfUrl)
            .then(res => {
              const {
                data: { id: selfId },
              } = res;
              if (selfId === userId) {
                form.submit();
              } else {
                this.onAccessCodeError('SessionError');
              }
            })
            .catch(() => this.onAccessCodeError('UnknownError'));
        }
      })
      .catch(() => this.onAccessCodeError('UnknownError'));
  };

  // stuffs

  val = (field, check, err) => {
    if (check) {
      const { T } = this.props;
      this.setState({ invalid: { [field]: T.t(`loginRegister.error.${err}`) } });
    } else {
      return true;
    }
  };

  onAccessCodeError = reason => {
    const { T } = this.props;
    const invalid = { accessCode: T.t(`error.accessCode.${reason}`) };
    this.setState({ submitting: false, invalid });
  };
}

RegisterForm.propTypes = {
  T: PropTypes.object.isRequired,
};

export default RegisterForm;
