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
import { Col, Form, FormGroup, Row } from 'reactstrap';

import Error from '../components/Error';
import { ContentTypeURLEncoded, trim } from '../services';
import { PasswordResetUrl } from '../services/URLs';
import EtcLoading from './EtcLoading';
import PasswordInput from './loginRegister/PasswordInput';
import { FormInput, FormSubmit } from './loginRegister/Util';

class ResetPassword extends React.Component {
  state = {
    invalid: {},
    loggedIn: false,
    submitting: false,
    userName: '',
    validating: true,
    invalidToken: false,
  };

  componentDidMount() {
    const token = this.props.match.params.token;
    axios
      .get(`${PasswordResetUrl}?token=${encodeURIComponent(token)}`)
      .then(res => {
        if (res.status === 200) {
          this.setState({ userName: res.data.userName, validating: false });
        } else {
          this.setState({ validating: false, invalidToken: true });
        }
      })
      .catch(() => this.setState({ validating: false, invalidToken: true }));
  }

  onRecover = e => {
    e.preventDefault();
    const form = e.target;
    const password = trim(form.elements.password.value);
    const confirmPassword = trim(form.elements.confirmPassword.value);
    if (
      this.val(form, 'password', !password, 'required') &&
      this.val(form, 'confirmPassword', password !== confirmPassword, 'passwordMatch')
    ) {
      this.recoverImpl(password);
    }
  };

  recoverImpl = password => {
    const { T } = this.props;
    this.setState({ submitting: true, invalid: {} });
    const token = this.props.match.params.token;
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
            this.onPasswordError(messages.map((msg, idx) => <div key={idx}>{msg}</div>));
          } else {
            this.onPasswordError(T.t(`error.recovery.${reason}`));
          }
        }
      })
      .catch(() => this.onPasswordError(T.t('error.recovery.UnknownError')));
  };

  onPasswordError = msg =>
    this.setState({
      submitting: false,
      invalid: { password: msg },
    });

  val = (form, field, check, err) => {
    if (check) {
      const { T } = this.props;
      form.elements[field].focus();
      this.setState({ invalid: { [field]: T.t(`loginRegister.error.${err}`) } });
    } else {
      return true;
    }
  };

  renderErrorPage = T => (
    <Error
      message={T.t('error.invalidToken')}
      T={T}
      setLastCrumb={this.props.setLastCrumb}
    />
  );

  render() {
    const { T } = this.props;
    const { submitting, validating, invalid, invalidToken } = this.state;
    if (validating) return <EtcLoading />;
    if (invalidToken) return this.renderErrorPage(T);
    return (
      <div className="container login-form">
        <Row>
          <Col md={{ size: 6, offset: 3 }}>
            <h3 className="mt-4 mb-3">{T.t('page.resetPassword.name')}</h3>
            <Form
              id="reset-password-form"
              autoComplete="off"
              className="admin-form"
              onSubmit={this.onRecover}
              method="POST"
            >
              <FormGroup>
                <FormInput
                  id="reset-username"
                  name="userName"
                  value={this.state.userName}
                  label={T.t('resetPassword.userName.label')}
                  readOnly
                />
                <PasswordInput
                  id="reset-password"
                  name="password"
                  T={T}
                  invalid={invalid.password}
                  label={T.t('resetPassword.password.label')}
                  setInvalid={password => this.setState({ invalid: { password } })}
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
  }
}

ResetPassword.propTypes = {
  T: PropTypes.object.isRequired,
  setLastCrumb: PropTypes.func.isRequired,
};

export default ResetPassword;
