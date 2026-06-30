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
import React, { useState } from 'react';
import { Form, FormGroup, Input } from 'reactstrap';

import { ContentTypeURLEncoded, trim } from '../../services';
import { PasswordResetUrl } from '../../services/URLs';
import PasswordInput from './PasswordInput';
import { FormInput, FormSubmit } from './Util';

interface ChangeModalProps {
  color?: string;
  onSuccess: (password: string, form: HTMLFormElement) => void;
  token: string;
  username: string;
  path: string;
  T: Polyglot;
}

interface Invalid {
  newPassword?: React.ReactNode;
  confirmPassword?: React.ReactNode;
}

const ChangeModal: React.FC<ChangeModalProps> = ({
  color,
  onSuccess,
  token,
  username,
  path,
  T,
}) => {
  const [changing, setChanging] = useState(false);
  const [invalid, setInvalid] = useState<Invalid>({});

  const onChangeError = (error: React.ReactNode) => {
    setInvalid({ newPassword: error });
    setChanging(false);
  };

  const changeImpl = (password: string, form: HTMLFormElement) => {
    const data = `token=${encodeURIComponent(token)}` + `&password=${encodeURIComponent(password)}`;
    setChanging(true);
    setInvalid({});
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
            onChangeError(messages.map((msg: string, idx: number) => <div key={idx}>{msg}</div>));
          } else {
            onChangeError(T.t(`error.recovery.${reason}`));
          }
        }
      })
      .catch(() => onChangeError(T.t('error.recovery.UnknownError')));
  };

  const onChange = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.target as HTMLFormElement;
    const newPassword = trim((form.elements.namedItem('password') as HTMLInputElement).value);
    const confirmPassword = trim(
      (form.elements.namedItem('confirmPassword') as HTMLInputElement).value
    );
    if (!newPassword) {
      setInvalid({ newPassword: T.t('loginRegister.error.required') });
    } else if (newPassword !== confirmPassword) {
      setInvalid({ confirmPassword: T.t('loginRegister.error.passwordMatch') });
    } else {
      changeImpl(newPassword, form);
    }
  };

  return (
    <Form
      id="change-form"
      className="admin-form"
      onSubmit={onChange}
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
            value={username}
          />
          <PasswordInput
            id="change-newPassword"
            name="password"
            T={T}
            autoFocus
            invalid={invalid.newPassword}
            label={T.t('loginRegister.change.newPassword.label')}
            setInvalid={newPassword => setInvalid({ newPassword })}
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
            value={path}
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
};

export default ChangeModal;
