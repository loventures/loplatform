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

import React, { useState } from 'react';
import {
  Button,
  FormGroup,
  Input,
  Label,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from 'reactstrap';

import usePolyglot from '../../../hooks/usePolyglot';
import GradeChangePanel from './GradeChanges';
import { PublishAnalysis } from './PublishAnalysis';

const UpdateVersionModal: React.FC<{
  isOpen: boolean;
  mode: 'update' | 'publish';
  publishing: boolean;
  publishAnalysis: PublishAnalysis | null;
  isLive: boolean;
  closeModal: (published: boolean, updateStatus: boolean) => void;
}> = ({ isOpen, mode, publishing, publishAnalysis, isLive, closeModal }) => {
  const polyglot = usePolyglot();
  const [updateStatus, setUpdateStatus] = useState(true);

  return (
    <Modal
      id="update-version-modal"
      size="lg"
      isOpen={isOpen}
      toggle={() => closeModal(false, false)}
    >
      <ModalHeader>
        {polyglot.t(mode === 'update' ? 'UPDATE.PUBLISH_UPDATE' : 'UPDATE.PUBLISH_PROJECT')}
      </ModalHeader>
      <ModalBody className="mb-last-p-0">
        <p>
          {polyglot.t(mode === 'update' ? 'UPDATE.ABOUT_TO_UPDATE' : 'UPDATE.ABOUT_TO_PUBLISH', {
            count: publishAnalysis?.numStaleSections,
          })}
        </p>
        {publishAnalysis?.hasChanges() && (
          <GradeChangePanel
            creates={publishAnalysis?.creates ?? []}
            updates={publishAnalysis?.updates ?? []}
            deletes={publishAnalysis?.deletes ?? []}
          />
        )}
      </ModalBody>
      <ModalFooter>
        {!isLive && (
          <FormGroup
            check
            inline
            className="me-auto"
          >
            <Label check>
              <Input
                id="reset-status"
                className="me-auto"
                type="checkbox"
                checked={updateStatus}
                onChange={e => setUpdateStatus(e.target.checked)}
              />
              Update Project Status to &quot;Live&quot;
            </Label>
          </FormGroup>
        )}
        <Button
          color="primary"
          outline
          onClick={() => closeModal(false, false)}
        >
          {polyglot.t('UPDATE.CANCEL_BTN')}
        </Button>
        <Button
          color="primary"
          onClick={() => closeModal(true, updateStatus)}
          disabled={publishing}
        >
          {polyglot.t('UPDATE.UPDATE_BTN')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default UpdateVersionModal;
