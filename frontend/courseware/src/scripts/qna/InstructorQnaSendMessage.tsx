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

import { AxiosError } from 'axios';
import { find } from 'lodash';
import { RichTextEditor } from '../contentEditor/directives/richTextEditor';
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, Form, FormGroup, Input, Label, Row } from 'reactstrap';

import { IoFile } from '../api/fileUploadApi';
import LoadingSpinner from '../directives/loadingSpinner';
import { useTranslation } from '../i18n/translationContext';
import { RecipientPicker } from '../messaging/directives/recipientPicker';
import { QnaPageLink } from '../utils/pageLinks';
import { multicast } from './qnaApi';
import QnaAttachments from './QnaAttachments';
import { useHistory } from 'react-router';
import { updateQnaQuery } from '../qna/qnaActions';
import { useDispatch } from 'react-redux';
import { QnaSentFilter } from '../qna/qnaReducer';

const messageToolbar = [
  { name: 'basicstyles', items: ['Bold', 'Italic'] },
  {
    name: 'paragraph',
    items: ['NumberedList', 'BulletedList', '-', 'Outdent', 'Indent', '-', 'Blockquote'],
  },
  { name: 'links', items: ['Link', 'Unlink'] },
  { name: 'insert', items: ['Maximize'] },
];

type Recipient = {
  user: any; // UserClass in the angular, but we're only passing it through here, so any it is
  id: number;
};

// The Angular recipient-picker directive expects a superset of this data model.
// This proxy satisfies the directive while feeding us state updates.
// See ../lof/src/js/messaging/directives/recipientPicker.js
class Message {
  recipients: Recipient[];
  setRecipients: React.Dispatch<React.SetStateAction<Recipient[]>>;
  selectingEntireClass: boolean;
  setSelectingEntireClass: React.Dispatch<React.SetStateAction<boolean>>;

  constructor(
    recipients: Recipient[],
    setRecipients: React.Dispatch<React.SetStateAction<Recipient[]>>,
    selectingEntireClass: boolean,
    setSelectingEntireClass: React.Dispatch<React.SetStateAction<boolean>>
  ) {
    this.recipients = recipients;
    this.setRecipients = setRecipients;
    this.selectingEntireClass = selectingEntireClass;
    this.setSelectingEntireClass = setSelectingEntireClass;
  }

  addSelection(recipient: Recipient) {
    const index = this.recipients.indexOf(recipient);
    if (index === -1) {
      this.recipients.push(recipient);
    }
    this.setRecipients(this.recipients);
  }

  removeSelection(recipient: Recipient) {
    const index = this.recipients.indexOf(recipient);
    if (index !== -1) {
      this.recipients.splice(index, 1);
    }
    this.setRecipients(this.recipients);
  }

  setSelection(newRecipients: Recipient[]) {
    this.recipients = newRecipients;
    this.setRecipients(this.recipients);
  }

  selectEntireClass(selectingEntireClass: boolean) {
    this.selectingEntireClass = selectingEntireClass;
    this.setSelectingEntireClass(this.selectingEntireClass);
  }

  isSelected(user: Recipient) {
    return find(this.recipients, recipient => recipient.id === user.id);
  }

  hasRecipients() {
    return !!this.selectingEntireClass || this.recipients.length > 0;
  }
}

const InstructorQnaSendMessage: React.FC = () => {
  const translate = useTranslation();
  const history = useHistory();
  const dispatch = useDispatch();

  const [recipients, setRecipients] = useState<Recipient[]>([]);
  const [selectingEntireClass, setSelectingEntireClass] = useState(false);
  const [subject, setSubject] = useState('');
  const [messageInput, setMessageInput] = useState('');
  const [attachments, setAttachments] = useState(new Array<IoFile>());

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | undefined>();
  const [messageSent, setMessageSent] = useState(false);

  const canSubmit =
    !!subject && !!messageInput && (!!selectingEntireClass || recipients.length > 0);

  const message = new Message(
    recipients,
    setRecipients,
    selectingEntireClass,
    setSelectingEntireClass
  );

  const afterMessage = () => {
    setSubject('');
    setMessageInput('');
    setMessageSent(true);
    setAttachments([]);
    dispatch(
      updateQnaQuery({
        prefilter: QnaSentFilter,
        order: { property: 'created', direction: 'desc' },
        offset: 0,
      })
    );
    history.push(`/instructor/qna`);
  };

  const sendMessage = () => {
    if (canSubmit && !submitting) {
      setSubmitting(true);
      setError(undefined);
      setMessageSent(false);
      multicast(
        message.recipients.map((recipient: Recipient) => recipient.id),
        subject,
        messageInput,
        attachments
      )
        .then(afterMessage)
        .catch((error: AxiosError) => {
          setError(error.message);
        })
        .finally(() => {
          setSubmitting(false);
        });
    }
  };

  return (
    <Form className="qna-instructor-message">
      <RecipientPicker message={message} />
      <Input
        className="my-3"
        placeholder={translate('QNA_SUBJECT')}
        value={subject}
        onChange={e => setSubject(e.target.value)}
        disabled={submitting}
        required
      />
      <RichTextEditor
        isMinimal
        focusOnRender
        minHeight={50}
        className="form-control qna-form-control"
        placeholder={translate('QNA_INPUT_PLACEHOLDER')}
        content={messageInput}
        onChange={setMessageInput}
        toolbar={messageToolbar}
        disabled={submitting}
        required
      />
      <FormGroup className="qna-attachments w-100 mt-2 mb-0 mb-md-2">
        <Label
          for="qna-attachments"
          className="d-none d-md-block"
        >
          Attachments
        </Label>
        <QnaAttachments
          attachments={attachments}
          setAttachments={setAttachments}
        />
      </FormGroup>
      <Row className="p-3 justify-content-between">
        <Link
          className="btn btn-secondary"
          to={QnaPageLink.toLink()}
        >
          {translate('QNA_CANCEL')}
        </Link>
        <Button
          onClick={sendMessage}
          color="primary"
          disabled={!canSubmit || submitting}
        >
          {translate('QNA_SEND')}
        </Button>
      </Row>
      <Row className="p-3">
        {submitting && <LoadingSpinner message="QNA_MESSAGE_SENDING" />}
        {messageSent && <div className="text-success">{translate('QNA_MESSAGE_SENT')}</div>}
        {error && <div className="text-danger">{error}</div>}
      </Row>
    </Form>
  );
};

export default InstructorQnaSendMessage;
