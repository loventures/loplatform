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

import React from 'react';
import { useDispatch } from 'react-redux';
import {
  Button,
  Input,
  FormGroup,
  Label,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from 'reactstrap';

import { toggleFeedbackOn } from '../../feedback/feedbackActions';
import { useFeedbackOn } from '../../feedback/feedbackHooks';
import { useDcmSelector } from '../../hooks';
import {
  setNarrativeInlineViewAction,
  toggleFlagMode,
  toggleKeywordsMode,
  toggleOmegaEdit,
  toggleSynchronous,
} from '../storyActions';

export const SessionModal: React.FC<{ open: boolean; toggle: () => void }> = ({ open, toggle }) => {
  return (
    <Modal
      toggle={toggle}
      isOpen={open}
    >
      <ModalHeader>Session Settings</ModalHeader>
      {open && <SessionForm />}
      <ModalFooter>
        <Button
          color="secondary"
          onClick={toggle}
        >
          Close
        </Button>
      </ModalFooter>
    </Modal>
  );
};

const SessionForm: React.FC = () => {
  const dispatch = useDispatch();
  const { inlineView, omegaEdit, synchronous, flagMode, keyWords } = useDcmSelector(
    s => s.story
  );
  const feedbackOn = useFeedbackOn();
  return (
    <ModalBody>
      <FormGroup switch>
        <Label>
          <Input
            id="inline-view-mode"
            type="switch"
            checked={inlineView}
            onChange={() => dispatch(setNarrativeInlineViewAction(!inlineView))}
          />
          Inline View Mode
        </Label>
      </FormGroup>
      <FormGroup switch>
        <Label>
          <Input
            id="keywords-mode"
            type="switch"
            checked={keyWords}
            onClick={() => dispatch(toggleKeywordsMode())}
          />
          Keywords Mode
        </Label>
      </FormGroup>
      <FormGroup switch>
        <Label>
          <Input
            id="ninja-raptor-mode"
            type="switch"
            checked={omegaEdit}
            onChange={() => dispatch(toggleOmegaEdit())}
          />
          Ninja Raptor Edit Mode
        </Label>
      </FormGroup>
      <FormGroup switch>
        <Label>
          <Input
            id="synchronous-edit-mode"
            type="switch"
            checked={synchronous}
            onChange={() => dispatch(toggleSynchronous())}
          />
          Real Time Edit Mode
        </Label>
      </FormGroup>
      <FormGroup switch>
        <Label>
          <Input
            id="language-flags-mode"
            type="switch"
            checked={flagMode}
            onClick={() => dispatch(toggleFlagMode())}
          />
          Language Flags Mode
        </Label>
      </FormGroup>
      <FormGroup
        className="mb-0"
        switch
      >
        <Label>
          <Input
            id="selection-feedback-mode"
            type="switch"
            checked={feedbackOn}
            onClick={() => dispatch(toggleFeedbackOn())}
          />
          Selection Feedback Mode
        </Label>
      </FormGroup>
    </ModalBody>
  );
};
