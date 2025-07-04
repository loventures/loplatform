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

import { addBookmarkAction } from '../../components/bookmarks/bookmarksReducer';
import { ContentWithRelationships } from '../../courseContentModule/selectors/assembleContentView';
import { useTranslation } from '../../i18n/translationContext';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button, Form, Input, Label, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

type ERBookmarkModalProps = {
  content: ContentWithRelationships;
  isOpen: boolean;
  setOpen: (open: boolean) => void;
};

const ERBookmarkModal: React.FC<ERBookmarkModalProps> = ({ content, isOpen, setOpen }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const [notes, setNotes] = useState('');
  const closeAdder = () => setOpen(false);
  useEffect(() => {
    if (!isOpen) setNotes('');
  }, [isOpen]);

  return (
    <Modal
      isOpen={isOpen}
      toggle={closeAdder}
      className="add-bookmark"
      aria-labelledby="bookmark-header"
    >
      <Form
        className="m-0"
        onSubmit={e => {
          e.preventDefault();
          dispatch(addBookmarkAction(content.id, notes));
          closeAdder();
        }}
      >
        <ModalHeader id="bookmark-header">{translate('BOOKMARK_ADD_TITLE')}</ModalHeader>
        <ModalBody>
          <Label for="bookmark-notes">{translate('BOOKMARK_NOTES')}</Label>
          <Input
            id="bookmark-notes"
            type="text"
            value={notes}
            onChange={e => setNotes(e.target.value)}
            placeholder={translate('BOOKMARK_NOTES_PLACEHOLDER')}
          />
        </ModalBody>
        <ModalFooter>
          <Button
            color="primary"
            outline
            onClick={closeAdder}
          >
            {translate('CLOSE')}
          </Button>
          <Button
            type="submit"
            color="primary"
            className="ms-2"
          >
            {translate('BOOKMARK_ADD')}
          </Button>
        </ModalFooter>
      </Form>
    </Modal>
  );
};

export default ERBookmarkModal;
