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

import { Component } from 'react';
import { connect } from 'react-redux';
import { withTranslation } from '../../../i18n/translationContext';

import { Modal, ModalHeader, ModalFooter } from 'reactstrap';

import LoadingMessages from '../../../directives/LoadingMessages';

import ManageDiscussionsTable from './ManageDiscussionsTable';

import {
  saveUpdatesActionCreator,
  toggleModalActionCreator,
  closeModalActionCreator,
} from '../actions/modalActions';

import { selectManageDiscussionsModalComponent } from '../selectors/modalSelectors';

class ManageDiscussionsModal extends Component {
  state = {
    closed: {},
  };

  toggleClosed = discussion => {
    this.setState({
      closed: {
        ...this.state.closed,
        [discussion.id]: !this.state.closed[discussion.id],
      },
    });
  };

  saveUpdates = () => {
    this.props.saveUpdates(this.props.discussions, this.state);
  };

  cancelUpdates = () => {
    this.setState({ closed: {} });
    this.props.closeModal();
  };

  render() {
    const { translate, discussions, isModalOpen, closedSavingState, toggleModal } = this.props;

    return (
      <Modal
        isOpen={isModalOpen}
        toggle={() => toggleModal()}
        autoFocus={false}
      >
        <ModalHeader
          toggle={() => toggleModal()}
          closeAriaLabel={translate('MODAL_CLOSE')}
        >
          {translate('MANAGE_DISCUSSIONS_MODAL_HEADER')}
        </ModalHeader>

        <ManageDiscussionsTable
          discussions={discussions}
          closedStatus={this.state.closed}
          toggleClosed={this.toggleClosed}
          disabled={closedSavingState.loading}
        />

        <ModalFooter>
          <LoadingMessages loadingState={closedSavingState} />
          <button
            className="btn btn-primary"
            onClick={this.saveUpdates}
          >
            {translate('MANAGE_DISCUSSIONS_UPDATE_ACTION')}
          </button>
          <button
            className="btn btn-danger"
            onClick={this.cancelUpdates}
          >
            {translate('MANAGE_DISCUSSIONS_CANCEL')}
          </button>
        </ModalFooter>
      </Modal>
    );
  }
}

export default connect(selectManageDiscussionsModalComponent, {
  toggleModal: toggleModalActionCreator,
  closeModal: closeModalActionCreator,
  saveUpdates: saveUpdatesActionCreator,
})(withTranslation(ManageDiscussionsModal));
