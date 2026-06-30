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
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import WaitDotGif from '../WaitDotGif';
import ModalBar from './ModalBar';

type ModalType = 'create' | 'update' | 'delete' | null;

export interface ModalState {
  info: string | null;
  error: string | null;
  type: ModalType;
  submitting: boolean;
  validationErrors: Record<string, string>;
}

interface Row {
  id?: number;
  [key: string]: unknown;
}

interface ReactTableModalProps {
  autoComplete?: string;
  entity: string;
  T: Polyglot;
  getModalTitle?: (type: ModalType) => string | undefined;
  footerExtra: (row: Row | false, type: ModalType) => React.ReactNode;
  headerExtra: (row: Row | false, type: ModalType) => React.ReactNode;
  modalState: ModalState;
  selectedRows: Row[];
  renderForm: (
    row: Row | false,
    validationErrors: Record<string, string>,
    rerender: () => void
  ) => React.ReactNode;
  errorCount: number;
  onModalSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
  hideModal: () => void;
}

const ReactTableModal: React.FC<ReactTableModalProps> = ({
  autoComplete,
  entity,
  T,
  getModalTitle,
  footerExtra,
  headerExtra,
  modalState,
  selectedRows,
  renderForm,
  errorCount,
  onModalSubmit,
  hideModal,
}) => {
  const [, setCount] = useState(0);
  const baseName = `adminPage.${entity}.modal.${modalState.type}`;

  const getRow = (): Row | false => {
    const selectedRow = selectedRows && selectedRows.length === 1 && selectedRows[0];
    return modalState.type === 'create' ? {} : selectedRow;
  };

  const renderHeader = () => {
    const row = getRow();
    return (
      <ModalHeader tag="h2">
        {(getModalTitle && getModalTitle(modalState.type)) || T.t(`${baseName}.title`, row as any)}
        {headerExtra(row, modalState.type)}
      </ModalHeader>
    );
  };

  const renderBody = () => {
    const { error, info, type, validationErrors } = modalState;
    const row = getRow();
    return (
      <ModalBody>
        {(error || info) && (
          <ModalBar
            key={'modal-' + errorCount}
            value={(error || info) as string}
            type={error ? 'error' : 'info'}
          />
        )}
        {type === 'delete'
          ? T.t(`${baseName}.confirmDelete`, {
              ...(row as object),
              smart_count: selectedRows.length,
            })
          : renderForm(row, validationErrors, () => setCount(count => 1 + count))}
      </ModalBody>
    );
  };

  const renderFooter = () => {
    const { submitting, type } = modalState;
    const row = getRow();
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

  return (
    <Modal
      id="react-table-modal"
      isOpen={true}
      className="crudTable-modal"
      backdrop="static"
      size="lg"
      toggle={hideModal}
    >
      {renderHeader()}
      <form
        id="reactTable-modalForm"
        className="admin-form"
        onSubmit={onModalSubmit}
        autoComplete={autoComplete}
      >
        {renderBody()}
        {renderFooter()}
      </form>
    </Modal>
  );
};

export default ReactTableModal;
