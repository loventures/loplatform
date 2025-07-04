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
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import WaitDotGif from '../WaitDotGif';
import ModalBar from './ModalBar';

class ReactTableModal extends React.Component {
  constructor(props) {
    super(props);
    this.baseName = `adminPage.${props.entity}.modal.${props.modalState.type}`;
    this.state = { count: 0 };
  }

  getRow = () => {
    const { selectedRows, modalState } = this.props;
    const selectedRow = selectedRows && selectedRows.length === 1 && selectedRows[0];
    return modalState.type === 'create' ? {} : selectedRow;
  };

  renderHeader = () => {
    const { getModalTitle, headerExtra, modalState, T } = this.props;
    const row = this.getRow();
    return (
      <ModalHeader tag="h2">
        {(getModalTitle && getModalTitle(modalState.type)) || T.t(`${this.baseName}.title`, row)}
        {headerExtra(row, modalState.type)}
      </ModalHeader>
    );
  };

  renderBody = () => {
    const { errorCount, modalState, renderForm, selectedRows, T } = this.props;
    const { error, info, type, validationErrors } = modalState;
    const row = this.getRow();
    return (
      <ModalBody>
        {(error || info) && (
          <ModalBar
            key={'modal-' + errorCount}
            value={error || info}
            type={error ? 'error' : 'info'}
          />
        )}
        {type === 'delete'
          ? T.t(`${this.baseName}.confirmDelete`, { ...row, smart_count: selectedRows.length })
          : renderForm(row, validationErrors, () =>
              this.setState(({ count }) => ({ count: 1 + count }))
            )}
      </ModalBody>
    );
  };

  renderFooter = () => {
    const { modalState, T, hideModal, footerExtra } = this.props;
    const { submitting, type } = modalState;
    const row = this.getRow();
    return (
      <ModalFooter>
        {footerExtra(row, type)}
        <Button
          id="react-table-close-modal-btn"
          disabled={submitting}
          onClick={hideModal}
        >
          {T.t('crudTable.modal.closeButton')}
        </Button>{' '}
        <Button
          id="react-table-submit-modal-btn"
          type="submit"
          color={type === 'delete' ? 'danger' : 'primary'}
          disabled={submitting}
        >
          {T.t(`crudTable.modal.${type}.submitButton`)}
          {submitting && (
            <WaitDotGif
              className="ms-2 waiting"
              color="light"
              size={16}
            />
          )}
        </Button>
      </ModalFooter>
    );
  };

  render() {
    const { autoComplete, onModalSubmit, hideModal } = this.props;
    return (
      <Modal
        id="react-table-modal"
        isOpen={true}
        className="crudTable-modal"
        backdrop="static"
        size="lg"
        toggle={hideModal}
      >
        {this.renderHeader()}
        <form
          id="reactTable-modalForm"
          className="admin-form"
          onSubmit={onModalSubmit}
          autoComplete={autoComplete}
        >
          {this.renderBody()}
          {this.renderFooter()}
        </form>
      </Modal>
    );
  }
}

ReactTableModal.propTypes = {
  autoComplete: PropTypes.string,
  entity: PropTypes.string.isRequired,
  T: PropTypes.object.isRequired,
  getModalTitle: PropTypes.func,
  footerExtra: PropTypes.func,
  headerExtra: PropTypes.func,
  modalState: PropTypes.object.isRequired,
  selectedRows: PropTypes.array.isRequired,
  renderForm: PropTypes.func.isRequired,
  errorCount: PropTypes.number.isRequired,
  onModalSubmit: PropTypes.func.isRequired,
  hideModal: PropTypes.func.isRequired,
};

export default ReactTableModal;
