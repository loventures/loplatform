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
import { useEffect, useState } from 'react';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { useModal, usePolyglot } from '../hooks';

export interface DiscardChangesModalData {
  discard: () => void;
  escape?: boolean; // invoked by escape
}

const DiscardChangesModal: React.FC = () => {
  const { modalOpen, toggleModal, data } = useModal<DiscardChangesModalData>();
  const polyglot = usePolyglot();

  const onDiscard = () => {
    data.discard();
    toggleModal();
  };

  const [escaped, setEscaped] = useState(!data.escape);
  useEffect(() => {
    if (data.escape) {
      // If this modal was summoned by escape then don't let releasing the escape key
      // toggle the modal immediately.
      const listener = (e: KeyboardEvent) => {
        if (e.key === 'Escape') setEscaped(true);
      };
      window.addEventListener('keydown', listener);
      return () => window.removeEventListener('keydown', listener);
    }
  }, [data.escape]);

  return (
    <Modal
      className="unsaved"
      isOpen={modalOpen}
      toggle={toggleModal}
      keyboard={escaped}
    >
      <ModalHeader>{polyglot.t('DISCARD_CHANGES_MODAL.HEADER')}</ModalHeader>
      <ModalBody>{polyglot.t('DISCARD_CHANGES_MODAL.BODY')}</ModalBody>
      <ModalFooter>
        <Button
          color="outline-primary"
          onClick={toggleModal}
        >
          {polyglot.t('CANCEL')}
        </Button>
        <Button
          color="danger"
          onClick={onDiscard}
        >
          {polyglot.t('DISCARD_CHANGES_MODAL.DISCARD_CHANGES')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default DiscardChangesModal;
