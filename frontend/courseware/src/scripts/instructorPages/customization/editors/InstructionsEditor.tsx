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

import { CustomisableContent } from '../../../api/customizationApi';
import { RichTextEditor } from '../../../contentEditor/directives/richTextEditor';
import { useTranslation } from '../../../i18n/translationContext';
import React, { useState } from 'react';
import { Modal, ModalBody, ModalHeader } from 'reactstrap';
import Button from 'reactstrap/lib/Button';
import ModalFooter from 'reactstrap/lib/ModalFooter';

// DCM editor uses span style, where CKE uses strong.
const slightlyFixHtml = (html?: string | null) =>
  html
    ?.replace(/<span style="font-weight: bold(?:er)?;?">([^<]*)<\/span>/g, '<strong>$1</strong>')
    ?.replace(/<span style="font-style: italic?;?">([^<]*)<\/span>/g, '<em>$1</em>');

export const InstructionsEditor: React.FC<{
  content: CustomisableContent;
  toggle: () => void;
  update: (instructions: string) => void;
}> = ({ content, toggle, update }) => {
  const translate = useTranslation();
  const [instructions, setInstructions] = useState(slightlyFixHtml(content.instructions) ?? '');
  return (
    <Modal
      id="customizations-instructions-modal"
      isOpen
      size="lg"
      toggle={toggle}
    >
      <ModalHeader>Edit Instructions</ModalHeader>
      <ModalBody>
        <RichTextEditor
          content={instructions}
          onChange={setInstructions}
          focusOnRender
        />
      </ModalBody>
      <ModalFooter>
        <Button
          id="customizations-instructions-cancel"
          color="link"
          onClick={toggle}
        >
          {translate('CANCEL')}
        </Button>{' '}
        <Button
          id="customizations-instructions-submit"
          color="primary"
          onClick={() => update(instructions)}
        >
          {translate('Submit')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};
