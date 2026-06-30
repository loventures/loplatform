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

import Polyglot from 'node-polyglot';
import React, { useState } from 'react';
import { Button, Form, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { ContentTypeURLEncoded, trim } from '../../services';
import { lojax } from '../../services/lojax';
import { PasswordRecoverUrl } from '../../services/URLs';
import { FormInput, FormSubmit } from './Util';

interface RecoverModalProps {
  onClose: () => void;
  T: Polyglot;
}

const RecoverModal: React.FC<RecoverModalProps> = ({ onClose, T }) => {
  const [emailError, setEmailError] = useState<React.ReactNode>(null);
  const [recovered, setRecovered] = useState(false);
  const [recovering, setRecovering] = useState(false);

  const onRecoverError = (reason: string) => {
    setEmailError(T.t(`error.recovery.${reason}`));
    setRecovering(false);
  };

  const recoverImpl = (email: string) => {
    const data =
      `search=${encodeURIComponent(email)}` +
      '&properties=emailAddress&properties=userName' +
      '&redirect=/etc/ResetPassword/';
    setRecovering(true);
    setEmailError(null);
    lojax({
      method: 'post',
      url: PasswordRecoverUrl,
      data: data,
      ...ContentTypeURLEncoded,
    })
      .then(response => {
        if (response.status === 202) {
          const {
            data: { reason },
          } = response;
          onRecoverError(reason);
        } else {
          setRecovered(true);
        }
      })
      .catch(() => onRecoverError('UnknownError'));
  };

  const onRecover = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.target as HTMLFormElement;
    const email = trim((form.elements.namedItem('email') as HTMLInputElement).value);
    if (!email) {
      setEmailError(T.t('loginRegister.error.required'));
    } else {
      recoverImpl(email);
    }
  };

  return (
    <Modal
      id="recover-modal"
      isOpen={true}
      backdrop="static"
      toggle={onClose}
    >
      <ModalHeader>{T.t('loginRegister.recover.title')}</ModalHeader>
      <Form
        id="recover-form"
        autoComplete="off"
        className="admin-form mb-0"
        onSubmit={onRecover}
      >
        {recovered ? (
          <ModalBody className="login-register">{T.t('loginRegister.recover.complete')}</ModalBody>
        ) : (
          <ModalBody className="login-form">
            <FormInput
              id="recover-email"
              name="email"
              invalid={emailError}
              label={T.t('loginRegister.recover.email.label')}
            />
          </ModalBody>
        )}
        <ModalFooter>
          <Button
            id="recover-close"
            onClick={onClose}
          >
            {T.t('loginRegister.recover.button.close')}
          </Button>
          {!recovered && (
            <FormSubmit
              id="recover-submit"
              className="ms-2"
              submitting={recovering}
              label={T.t('loginRegister.recover.button.recover')}
            />
          )}
        </ModalFooter>
      </Form>
    </Modal>
  );
};

export default RecoverModal;
