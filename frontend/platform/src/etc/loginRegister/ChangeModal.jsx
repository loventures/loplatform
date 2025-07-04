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
import { Form, FormGroup, Input } from 'reactstrap';

import { ContentTypeURLEncoded, trim } from '../../services';
import { PasswordResetUrl } from '../../services/URLs';
import PasswordInput from './PasswordInput';
import { FormInput, FormSubmit } from './Util';

class ChangeModal extends React.Component {
  state = {
    changing: null,
    invalid: {},
  };

  render() {
    const {
      props: { T, color },
      state: { changing, invalid },
    } = this;
    return (
      <Form
        id="change-form"
        className="admin-form"
        onSubmit={this.onChange}
        method="POST"
        action="/api/v2/sessions/loginRedirect"
      >
        <div>
          <h3 className="mb-3">{T.t('loginRegister.change.title')}</h3>
          <p className="small">{T.t('loginRegister.change.passwordExpired')}</p>
          <FormGroup>
            <Input
              hidden
              type="text"
              autoComplete="username"
              name="username"
              value={this.props.username}
            />
            <PasswordInput
              id="change-newPassword"
              name="password"
              T={T}
              autoFocus
              invalid={invalid.newPassword}
              label={T.t('loginRegister.change.newPassword.label')}
              setInvalid={newPassword => this.setState({ invalid: { newPassword } })}
            />
            <FormInput
              id="change-confirmPassword"
              name="confirmPassword"
              type="password"
              autoComplete="new-password"
              invalid={invalid.confirmPassword}
              label={T.t('loginRegister.change.confirmPassword.label')}
            />
            <Input
              type="hidden"
              name="path"
              value={this.props.path}
            />
          </FormGroup>
          <FormSubmit
            id="change-submit"
            block
            className="mb-4"
            color={color}
            submitting={changing}
            label={T.t('loginRegister.change.button.change')}
          />
        </div>
      </Form>
    );
  }

  onChange = e => {
    e.preventDefault();
    const { T } = this.props;
    const form = e.target;
    const newPassword = trim(form.elements.password.value);
    const confirmPassword = trim(form.elements.confirmPassword.value);
    if (!newPassword) {
      this.setState({ invalid: { newPassword: T.t('loginRegister.error.required') } });
    } else if (newPassword !== confirmPassword) {
      this.setState({ invalid: { confirmPassword: T.t('loginRegister.error.passwordMatch') } });
    } else {
      this.changeImpl(newPassword, form);
    }
  };

  changeImpl = (password, form) => {
    const { onSuccess, token, T } = this.props;
    const data = `token=${encodeURIComponent(token)}` + `&password=${encodeURIComponent(password)}`;
    this.setState({ changing: true, invalid: {} });
    axios
      .post(PasswordResetUrl, data, ContentTypeURLEncoded)
      .then(res => {
        if (res.status === 204) {
          onSuccess(password, form);
        } else {
          const {
            data: { reason, messages },
          } = res;
          if (reason === 'InvalidPassword') {
            this.onChangeError(messages.map((msg, idx) => <div key={idx}>{msg}</div>));
          } else {
            this.onChangeError(T.t(`error.recovery.${reason}`));
          }
        }
      })
      .catch(() => this.onChangeError(T.t('error.recovery.UnknownError')));
  };

  onChangeError = error =>
    this.setState({
      invalid: { newPassword: error },
      changing: false,
    });
}

ChangeModal.propTypes = {
  color: PropTypes.string,
  onSuccess: PropTypes.func.isRequired,
  token: PropTypes.string.isRequired,
  username: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired,
  T: PropTypes.object.isRequired,
};

export default ChangeModal;
