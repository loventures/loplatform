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

import * as React from 'react';
import { useSelector } from 'react-redux';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { useModal, usePolyglot } from '../../hooks';
import { DcmState } from '../../types/dcmState';

export interface MergeUpdatesModalData {
  callback: (action: 'Cancel' | 'Merge' | 'Save') => void;
}

const MergeUpdatesModal = () => {
  const { modalOpen, toggleModal } = useModal();
  const polyglot = usePolyglot();
  const { callback } = useSelector((state: DcmState) => state.modal.data as MergeUpdatesModalData);

  const handleClick = (action: 'Cancel' | 'Merge' | 'Save') => {
    toggleModal();
    callback(action);
  };

  return (
    <Modal
      className="unsaved"
      isOpen={modalOpen}
      toggle={toggleModal}
    >
      <ModalHeader>{polyglot.t('MERGE_UPDATES_MODAL.HEADER')}</ModalHeader>
      <ModalBody>{polyglot.t('MERGE_UPDATES_MODAL.BODY')}</ModalBody>
      <ModalFooter>
        <Button
          color="primary"
          outline
          onClick={() => handleClick('Cancel')}
        >
          {polyglot.t('MERGE_UPDATES_MODAL.CANCEL')}
        </Button>
        <Button
          color="warning"
          onClick={() => handleClick('Merge')}
        >
          {polyglot.t('MERGE_UPDATES_MODAL.MERGE_UPDATES')}
        </Button>
        <Button
          color="danger"
          onClick={() => handleClick('Save')}
        >
          {polyglot.t('MERGE_UPDATES_MODAL.SAVE_ANYWAY')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default MergeUpdatesModal;
