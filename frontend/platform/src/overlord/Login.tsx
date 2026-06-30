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

import classnames from 'classnames';
import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button, FormFeedback, Input, Label, Modal, ModalBody, ModalFooter } from 'reactstrap';

import { setLoPlatform } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { getPlatform, login } from '../services';

const Login: React.FC = () => {
  const T = useTranslations();
  const dispatch = useDispatch();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const onSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.target as HTMLFormElement;
    setSubmitting(true);
    setError(null);
    login(
      (form.elements.namedItem('username') as HTMLInputElement).value,
      (form.elements.namedItem('password') as HTMLInputElement).value,
      false,
      true
    )
      .then(response => {
        setSubmitting(false);
        if (response.status === 202) {
          setError(response.data.reason);
        } else {
          setSuccess(true);
        }
      })
      .catch(() => {
        setSubmitting(false);
        setError('UnknownError');
      });
  };

  const onClosed = () => {
    getPlatform(true).then(res => dispatch(setLoPlatform(res.data)));
  };

  const validProp = error ? { invalid: true } : {};
  return (
    <div className="container-fluid">
      <div>
        <Modal
          id="overlord-login"
          isOpen={!success}
          backdrop="static"
          autoFocus={false}
          backdropClassName="no-backdrop"
          onClosed={onClosed}
          fade={false}
        >
          <form
            onSubmit={onSubmit}
            autoComplete="off"
            method="POST"
          >
            <ModalBody className={classnames({ 'has-danger': !!error })}>
              <Label for="overlord-login-username">{T.t('overlord.login.username')}</Label>
              <Input
                id="overlord-login-username"
                name="username"
                bsSize="lg"
                {...validProp}
                className="mb-3"
                autoComplete="username"
              />
              <Label for="overlord-login-password">{T.t('overlord.login.password')}</Label>
              <Input
                id="overlord-login-password"
                type="password"
                bsSize="lg"
                {...validProp}
                name="password"
                autoComplete="current-password"
              />
              {error && <FormFeedback>{T.t(`error.login.${error}`)}</FormFeedback>}
            </ModalBody>
            <ModalFooter>
              <Button
                id="overlord-login-submit"
                color="primary"
                size="lg"
                block
                disabled={submitting}
              >
                {T.t('overlord.login.action.login')}
              </Button>
            </ModalFooter>
          </form>
        </Modal>
      </div>
    </div>
  );
};

export default Login;
