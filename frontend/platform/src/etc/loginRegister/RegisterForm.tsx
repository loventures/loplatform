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
import { Form, FormGroup } from 'reactstrap';

import { EmailRE, trim } from '../../services';
import { lojax } from '../../services/lojax';
import { RegisterRedeemUrl, UsersSelfUrl } from '../../services/URLs';
import PasswordInput from './PasswordInput';
import { FormInput, FormSubmit, Schema, validateAccessCode } from './Util';

interface RegisterFormProps {
  T: Polyglot;
}

interface Invalid {
  givenName?: React.ReactNode;
  familyName?: React.ReactNode;
  email?: React.ReactNode;
  password?: React.ReactNode;
  confirmPassword?: React.ReactNode;
  accessCode?: React.ReactNode;
}

const RegisterForm: React.FC<RegisterFormProps> = ({ T }) => {
  const [invalid, setInvalid] = useState<Invalid>({});
  const [submitting, setSubmitting] = useState(false);

  const val = (field: keyof Invalid, check: boolean, err: string): boolean => {
    if (check) {
      setInvalid({ [field]: T.t(`loginRegister.error.${err}`) });
      return false;
    }
    return true;
  };

  const onAccessCodeError = (reason: string) => {
    setSubmitting(false);
    setInvalid({ accessCode: T.t(`error.accessCode.${reason}`) });
  };

  const registerRedeem = (
    givenName: string,
    familyName: string,
    emailAddress: string,
    password: string,
    accessCode: string,
    form: HTMLFormElement
  ) => {
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
        if (res.status === 202) {
          const {
            data: { reason, messages },
          } = res;
          if (reason === 'DuplicateUser') {
            setSubmitting(false);
            setInvalid({ email: T.t(`loginRegister.error.duplicateUser`) });
          } else if (reason === 'InvalidPassword') {
            setSubmitting(false);
            setInvalid({
              password: messages.map((msg: string, idx: number) => <div key={idx}>{msg}</div>),
            });
          } else {
            // InvalidAccessCode
            onAccessCodeError('UnknownError');
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
                onAccessCodeError('SessionError');
              }
            })
            .catch(() => onAccessCodeError('UnknownError'));
        }
      })
      .catch(() => onAccessCodeError('UnknownError'));
  };

  const registerImpl = (
    givenName: string,
    familyName: string,
    emailAddress: string,
    password: string,
    accessCode: string,
    form: HTMLFormElement
  ) => {
    setInvalid({});
    setSubmitting(true);
    validateAccessCode(accessCode)
      .then(res => {
        if (res.data === 'Valid') {
          registerRedeem(givenName, familyName, emailAddress, password, accessCode, form);
        } else {
          onAccessCodeError(res.data);
        }
      })
      .catch(() => onAccessCodeError('UnknownError'));
  };

  const doRegister = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.target as HTMLFormElement;
    const named = (name: string) => (form.elements.namedItem(name) as HTMLInputElement).value;
    const givenName = trim(named('givenName'));
    const familyName = trim(named('familyName'));
    const email = trim(named('username'));
    const password = trim(named('password'));
    const confirmPassword = trim(named('confirmPassword'));
    const accessCode = trim(named('accessCode'));
    if (
      val('email', !email, 'required') &&
      val('email', !EmailRE.test(email), 'validEmail') &&
      val('password', !password, 'required') &&
      val('confirmPassword', password !== confirmPassword, 'passwordMatch') &&
      val('accessCode', !accessCode, 'required')
    ) {
      registerImpl(givenName, familyName, email, password, accessCode, form);
    }
  };

  return (
    <React.Fragment>
      <h3>{T.t('loginRegister.title.registerRedeem')}</h3>
      <Form
        id="register-form"
        onSubmit={doRegister}
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
            setInvalid={password => setInvalid({ password })}
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
};

export default RegisterForm;
