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

export interface OverwriteChangesModalData {
  titles: string[];
  callback: (save: boolean) => void;
}

const OverwriteChangesModal = () => {
  const { modalOpen, toggleModal } = useModal();
  const polyglot = usePolyglot();
  const { callback, titles } = useSelector(
    (state: DcmState) => state.modal.data as OverwriteChangesModalData
  );

  const handleClick = (save: boolean) => {
    toggleModal();
    callback(save);
  };

  return (
    <Modal
      className="unsaved"
      isOpen={modalOpen}
      toggle={toggleModal}
      size="lg"
    >
      <ModalHeader>{polyglot.t('STORY_OVERWRITE_MODAL.HEADER')}</ModalHeader>
      <ModalBody className="mb-last-p-0">
        <p>{polyglot.t('STORY_OVERWRITE_MODAL.BODY.0')}</p>
        <ul>
          {titles.map((t, i) => (
            <li key={i}>{t}</li>
          ))}
        </ul>
        <p>{polyglot.t('STORY_OVERWRITE_MODAL.BODY.1')}</p>
      </ModalBody>
      <ModalFooter>
        <Button
          color="primary"
          outline
          onClick={() => handleClick(false)}
        >
          {polyglot.t('STORY_OVERWRITE_MODAL.CANCEL')}
        </Button>
        <Button
          color="danger"
          onClick={() => handleClick(true)}
        >
          {polyglot.t('STORY_OVERWRITE_MODAL.OVERWRITE')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default OverwriteChangesModal;
