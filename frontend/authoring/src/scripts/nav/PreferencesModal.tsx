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

import gretchen from '../grfetchen/';
import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
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

import { usePolyglot } from '../hooks';
import { AuthoringPreferences } from '../user/reducers';
import {
  receiveAuthoringPreferences,
  useAuthoringPreferences,
  useUserProfile,
} from '../user/userActions';

const PreferencesModal: React.FC<{
  toggle: () => void;
}> = ({ toggle }) => {
  const [submitting, setSubmitting] = useState(false);
  const polyglot = usePolyglot();
  const profile = useUserProfile();
  const dispatch = useDispatch();
  const initialPreferences = useAuthoringPreferences();
  const [preferences, setPreferences] = useState(initialPreferences);
  const patchPreferences = (patch: Partial<AuthoringPreferences>) =>
    setPreferences(p => ({ ...p, ...patch }));

  const onSave = () => {
    setSubmitting(true);
    const authoringPreferences = preferences;
    gretchen
      .put(`/api/v2/config/userPreferences/${profile.id}`)
      .data({ authoringPreferences })
      .exec()
      .then(() => {
        dispatch(receiveAuthoringPreferences(authoringPreferences));
        toggle();
      })
      .finally(() => setSubmitting(false));
    gretchen.put;
  };

  return (
    <Modal
      isOpen={true}
      toggle={toggle}
    >
      <ModalHeader>{polyglot.t('APP_HEADER_PREFERENCES')}</ModalHeader>
      <ModalBody className="ps-4">
        <FormGroup check>
          <Label check>
            <Input
              type="checkbox"
              checked={preferences.editModeDefault}
              onChange={e => patchPreferences({ editModeDefault: e.target.checked })}
            />
            Default to Editing Mode
          </Label>
        </FormGroup>
        <FormGroup
          check
          className="mb-0"
        >
          <Label check>
            <Input
              type="checkbox"
              checked={preferences.autoPreview}
              onChange={e => patchPreferences({ autoPreview: e.target.checked })}
            />
            Automatically Open HTML Preview
          </Label>
        </FormGroup>
      </ModalBody>
      <ModalFooter>
        <Button
          id="modal-prefs-cancel"
          color="secondary"
          onClick={toggle}
        >
          {polyglot.t('CANCEL')}
        </Button>
        <Button
          id="modal-link-confirm"
          color="primary"
          onClick={onSave}
          disabled={submitting}
        >
          {polyglot.t(`SAVE`)}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default PreferencesModal;
