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
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { useModal, usePolyglot } from '../../hooks';

export interface ResetContentStatusModalData {
  partial: boolean;
  courseStatus?: string;
  callback: (reset: boolean) => void;
}

export const ResetContentStatusModal: React.FC = () => {
  const { modalOpen, toggleModal, data } = useModal<ResetContentStatusModalData>();
  const polyglot = usePolyglot();

  const handleClick = (reset: boolean) => {
    data.callback(reset);
    toggleModal();
  };

  return (
    <Modal
      className="clear-content-status"
      isOpen={modalOpen}
      toggle={toggleModal}
    >
      <ModalHeader>Reset Content Status?</ModalHeader>
      {data.partial ? (
        <ModalBody>Do you want to reset the content status of all descendant content?</ModalBody>
      ) : (
        <ModalBody>
          Do you want to reset the content status of all content in this project, and set the course
          status to {data.courseStatus}?
        </ModalBody>
      )}
      <ModalFooter>
        <Button
          color="outline-primary"
          onClick={() => toggleModal()}
        >
          {polyglot.t('CANCEL')}
        </Button>
        <Button
          color="success"
          onClick={() => handleClick(false)}
        >
          No
        </Button>
        <Button
          color="danger"
          onClick={() => handleClick(true)}
        >
          Reset
        </Button>
      </ModalFooter>
    </Modal>
  );
};
