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

import LoadingMessages from '../../../directives/LoadingMessages';
import { withTranslation } from '../../../i18n/translationContext';
import { getUserFullName } from '../../../utilities/getUserFullName';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { Form, FormGroup, Input, Label, Modal, ModalBody, ModalHeader } from 'reactstrap';

import {
  resetMessageStateActionCreator,
  sendMessageActionCreator,
  toggleModalActionCreator,
} from '../actions/modalActions';
import { selectLearnerTableModalComponent } from '../selectors/modalSelectors';

class LearnerTableModal extends Component {
  state = {
    message: '',
  };

  messageChanged = event => {
    this.setState({ message: event.target.value });
    if (this.props.messageState.loaded) {
      this.props.messageRestarted();
    }
  };

  sendMessage = e => {
    e.preventDefault();
    this.props.sendMessage(this.props.learner, this.state.message);
  };

  render() {
    const { translate, isModalOpen, learner, messageState, toggleModal } = this.props;

    const displayedMessage = messageState.loaded && !messageState.error ? '' : this.state.message;

    return (
      <Modal
        className="modal-lg"
        isOpen={isModalOpen}
        toggle={toggleModal}
      >
        <ModalHeader
          toggle={toggleModal}
          closeAriaLabel={translate('MODAL_CLOSE')}
        >
          <div className="flex-row-content align-items-start">
            <span className="flex-col-fluid word-wrap-all">{getUserFullName(learner)}</span>
          </div>
        </ModalHeader>
        <ModalBody>
          <Form
            onSubmit={this.sendMessage}
            className="mb-0"
          >
            <FormGroup>
              <Label for="overview-modal-grade">{translate('SEND_MESSAGE_TITLE')}</Label>
              <Input
                type="textarea"
                id="overview-modal-message"
                placeholder={translate('SEND_MESSAGE_PLACEHOLDER')}
                value={displayedMessage}
                onChange={this.messageChanged}
                disabled={messageState.loading}
              />
            </FormGroup>
            <FormGroup
              disabled={true}
              className="mb-0"
            >
              <LoadingMessages
                loadingState={messageState}
                success="STUDENT_AT_RISK_MESSAGE_SENT"
              />
              <div className="d-flex justify-content-start">
                <button
                  className="btn btn-primary"
                  type="submit"
                  value="submit"
                  disabled={!displayedMessage || messageState.loading}
                >
                  {translate('SEND_MESSAGE_BUTTON')}
                </button>
              </div>
            </FormGroup>
          </Form>
        </ModalBody>
      </Modal>
    );
  }
}

export default connect(selectLearnerTableModalComponent, {
  toggleModal: toggleModalActionCreator,
  sendMessage: sendMessageActionCreator,
  messageRestarted: resetMessageStateActionCreator,
})(withTranslation(LearnerTableModal));
