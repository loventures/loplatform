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

import PropTypes from 'prop-types';
import React from 'react';
import { Button, Form, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { ContentTypeURLEncoded, trim } from '../../services';
import { lojax } from '../../services/lojax';
import { PasswordRecoverUrl } from '../../services/URLs';
import { FormInput, FormSubmit } from './Util';

class RecoverModal extends React.Component {
  state = {
    emailError: null,
    recovered: false,
    recovering: null,
  };

  render() {
    const { onClose, T } = this.props;
    const { emailError, recovering, recovered } = this.state;
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
          onSubmit={this.onRecover}
        >
          {recovered ? (
            <ModalBody className="login-register">
              {T.t('loginRegister.recover.complete')}
            </ModalBody>
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
  }

  onRecover = e => {
    e.preventDefault();
    const { T } = this.props;
    const form = e.target;
    const email = trim(form.elements.email.value);
    if (!email) {
      this.setState({ emailError: T.t('loginRegister.error.required') });
    } else {
      this.recoverImpl(email);
    }
  };

  recoverImpl = email => {
    const data =
      `search=${encodeURIComponent(email)}` +
      '&properties=emailAddress&properties=userName' +
      '&redirect=/etc/ResetPassword/';
    this.setState({ recovering: true, emailError: null });
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
          this.onRecoverError(reason);
        } else {
          this.onRecovered();
        }
      })
      .catch(() => this.onRecoverError('UnknownError'));
  };

  onRecoverError = reason =>
    this.setState({
      emailError: this.props.T.t(`error.recovery.${reason}`),
      recovering: false,
    });

  onRecovered = () => {
    this.setState({ recovered: true });
  };
}

RecoverModal.propTypes = {
  onClose: PropTypes.func.isRequired,
  T: PropTypes.object.isRequired,
};

export default RecoverModal;
