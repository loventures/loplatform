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
import { useState } from 'react';
import { useSelector } from 'react-redux';
import { Button, Input, Label, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { useModal, usePolyglot } from '../../hooks';
import { DcmState } from '../../types/dcmState';

export interface StorySaveModalData {
  callback: (save: boolean, remember: boolean) => void;
}

const StorySaveModal = () => {
  const { modalOpen, toggleModal } = useModal();
  const polyglot = usePolyglot();
  const { callback } = useSelector((state: DcmState) => state.modal.data as StorySaveModalData);
  const [remember, setRemember] = useState(false);

  const handleClick = (save: boolean) => {
    toggleModal();
    callback(save, remember);
  };

  return (
    <Modal
      className="unsaved"
      isOpen={modalOpen}
      toggle={toggleModal}
      style={{ minWidth: '600px' }}
    >
      <ModalHeader>{polyglot.t('STORY_SAVE_MODAL.HEADER')}</ModalHeader>
      <ModalBody>{polyglot.t('STORY_SAVE_MODAL.BODY')}</ModalBody>
      <ModalFooter>
        <div className="form-check flex-grow-1">
          <Label check>
            <Input
              type="checkbox"
              checked={remember}
              onChange={e => setRemember(e.target.checked)}
            />
            {polyglot.t('STORY_SAVE_MODAL.REMEMBER')}
          </Label>
        </div>
        <Button
          color="primary"
          outline
          onClick={() => handleClick(false)}
        >
          {polyglot.t('STORY_SAVE_MODAL.KEEP_EDITING')}
        </Button>
        <Button
          color="success"
          onClick={() => handleClick(true)}
        >
          {polyglot.t('STORY_SAVE_MODAL.SAVE_CHANGES')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default StorySaveModal;
