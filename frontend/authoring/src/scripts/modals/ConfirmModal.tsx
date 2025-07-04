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
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { trackConfirm } from '../analytics/AnalyticsEvents';
import { useModal, usePolyglot } from '../hooks';
import { DcmState } from '../types/dcmState';

export interface ConfirmModalData {
  confirmationType: ConfirmationTypes;
  color?: 'primary' | 'danger';
  words: {
    header: string;
    body?: string;
    htmlBody?: string;
    cancel?: string;
    confirm: string;
  };
  confirmCallback: () => void | Promise<void>;
}

export enum ConfirmationTypes {
  ConfirmUnderstandReuse = 'Confirm Understand Reuse',
  RemoveAsset = 'Remove Asset',
  RemoveCompetency = 'Remove Competency',
  RemoveCourseBannerImage = 'Remove Course Banner Image',
  RemoveRubricCriterion = 'Remove Rubric Criterion',
  RevertProject = 'Revert Project',
  DeleteProject = 'Delete Project',
  DeleteLink = 'Delete Link',
  ArchiveFeedback = 'Archive Feedback',
}

const ConfirmModal = () => {
  const polyglot = usePolyglot();
  const { modalOpen, toggleModal } = useModal();
  const { confirmationType, words, color, confirmCallback }: ConfirmModalData = useSelector(
    (state: DcmState) => state.modal.data
  );
  const { header, body, htmlBody, cancel, confirm } = words;
  const [ing, setIng] = useState(false);
  const spaceless = confirmationType.replace(/ /g, '');

  return (
    <Modal
      id={`modal-confirm-${spaceless}`}
      className={`confirm ${spaceless}`}
      isOpen={modalOpen}
      toggle={toggleModal}
    >
      <ModalHeader>{header}</ModalHeader>
      <ModalBody>
        {htmlBody ? (
          <p
            className="overflow-hidden mb-0"
            dangerouslySetInnerHTML={{ __html: htmlBody }}
          ></p>
        ) : (
          <p className="overflow-hidden mb-0">{body}</p>
        )}
      </ModalBody>
      <ModalFooter>
        <Button
          color="outline-primary"
          onClick={toggleModal}
          disabled={ing}
        >
          {cancel || polyglot.t('CANCEL')}
        </Button>
        <Button
          color={color || 'primary'}
          onClick={() => {
            setIng(true);
            Promise.resolve(confirmCallback())
              .then(() => {
                toggleModal();
                trackConfirm(confirmationType);
              })
              .finally(() => setIng(false));
          }}
          disabled={ing}
        >
          {confirm}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default ConfirmModal;
