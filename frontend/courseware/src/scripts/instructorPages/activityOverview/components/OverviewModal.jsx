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

import React, { Component } from 'react';
import { connect } from 'react-redux';
import { withTranslation } from '../../../i18n/translationContext';

import LoadingSpinner from '../../../directives/loadingSpinner';
import ErrorMessage from '../../../directives/ErrorMessage';

import { Modal, ModalHeader, ModalBody, Form, FormGroup, Label, Input } from 'reactstrap';

import { getUserFullName } from '../../../utilities/getUserFullName';

import {
  sendMessageActionCreator,
  toggleSendMessageModalActionCreator,
} from '../services/sendMessageActions';

import { selectOverviewModalComponent } from '../services/selectors';

class OverviewModal extends Component {
  state = {
    grade: null,
    message: '',
  };

  gradeChanged = grade => {
    this.setState({ grade });
  };

  messageChanged = message => {
    this.setState({ message });
  };

  sendMessage = e => {
    e.preventDefault();
    const shouldSendGrade = this.state.grade !== this.props.modalState.activeOverview.grade;
    this.props.sendMessage(
      this.props.contentId,
      this.props.modalState.activeOverview,
      shouldSendGrade && this.state.grade,
      this.state.message && this.state.message.trim()
    );
  };

  resetForm = () => {
    this.setState({
      grade: null,
      message: '',
    });
  };

  gradeIsValid = () => {
    const { grade } = this.state;
    return grade && grade >= 0 && grade <= 100;
  };

  render() {
    const { translate, loadingState, modalState, toggleModal, content } = this.props;

    const { grade, message } = this.state;

    const activeOverview = modalState.activeOverview;

    const displayGrade =
      grade !== null
        ? grade
        : (activeOverview.grade &&
            Math.round(
              (100 * activeOverview.grade.pointsAwarded) / activeOverview.grade.pointsPossible
            )) ||
          undefined; // eslint-disable-line

    // you can submit just a grade, or just a message, or both
    // ... but if you want to submit a grade, it has to be valid
    const maySubmit = (grade || message) && (!grade || this.gradeIsValid());

    return (
      <Modal
        isOpen={modalState.isOpen}
        toggle={() => toggleModal()}
        onClosed={this.resetForm}
      >
        <ModalHeader
          toggle={() => toggleModal()}
          closeAriaLabel={translate('MODAL_CLOSE')}
        >
          {translate('MANUAL_GRADE_HEADER')} {content.name} -{' '}
          {getUserFullName(activeOverview && activeOverview.learner)}
        </ModalHeader>
        <ModalBody>
          <Form onSubmit={this.sendMessage}>
            <FormGroup>
              <Label for="overview-modal-grade">{translate('MANUAL_GRADE_INSTRUCTIONS')}</Label>
              <Input
                type="number"
                min="0"
                max="100"
                id="overview-modal-grade"
                valid={this.gradeIsValid()}
                value={displayGrade}
                onChange={e => this.gradeChanged(e.target.value)}
              />
            </FormGroup>
            <FormGroup>
              <Label for="overview-modal-message">{translate('NOTIFICATION_INSTRUCTIONS')}</Label>
              <Input
                type="textarea"
                id="overview-modal-message"
                value={message}
                onChange={e => this.messageChanged(e.target.value)}
              />
            </FormGroup>
            <FormGroup>
              {loadingState.error && <ErrorMessage error={loadingState.error} />}
              {loadingState.loading && <LoadingSpinner />}
              <div className="d-flex justify-content-end">
                <button
                  className="btn btn-primary"
                  type="submit"
                  value="submit"
                  title={translate('MANUAL_GRADE_ADD_OVERRIDE_EXPLANATION')}
                  disabled={!maySubmit}
                >
                  {translate('MANUAL_GRADE_ADD_OVERRIDE')}
                </button>
              </div>
            </FormGroup>
          </Form>
        </ModalBody>
      </Modal>
    );
  }
}

export default connect(selectOverviewModalComponent, {
  toggleModal: toggleSendMessageModalActionCreator,
  sendMessage: sendMessageActionCreator,
})(withTranslation(OverviewModal));
