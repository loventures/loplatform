/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import classNames from 'classnames';
import { isEmpty } from 'lodash';
import { RichTextEditor } from '../../contentEditor/directives/richTextEditor';
import { withTranslation } from '../../i18n/translationContext';
import React, { useState } from 'react';

import withFocusOnMount from './WithFocusOnMount';
import messagingService from '../../services/messagingService.ts';

/** Length of time to display sent message banner. */
const SentMessageDisplayTime = 5000;

const initialState = {
  subject: '',
  body: '',
  sending: false,
  sent: false,
  valid: false,
};

export type ChatEmailProps = {
  recipientId: number;
};

const ChatEmailInner = ({ translate, onRef, state, setState, sendEmail }: any) => {
  const { subject, body, sending, sent, valid } = state;
  return (
    <React.Fragment>
      <div className="modal-body chat-email-body">
        <div className="chat-direct-message">
          <input
            ref={onRef}
            className="chat-direct-message-subject form-control"
            type="text"
            placeholder={translate('MESSAGING_ENTER_SUBJECT')}
            value={subject}
            required
            onChange={event => {
              if (event && event.target) {
                const subject = event.target.value;
                const valid = !isEmpty(subject) && !isEmpty(state.body);
                setState({ ...state, subject, valid });
              }
            }}
          />
          <RichTextEditor
            content={body}
            onChange={(body: any) => {
              const valid = !isEmpty(state.subject) && !isEmpty(body);
              setState({ ...state, body, valid });
            }}
            isMinimal="true"
          />
        </div>
      </div>
      <div>
        <div className="modal-footer chat-footer">
          {sent && <div className="alert alert-success">{translate('USER_CHAT_MESSAGE_SENT')}</div>}
          {!sent && (
            <button
              className="btn btn-primary send-chat-direct-message-button"
              disabled={!valid || sending}
              onClick={sendEmail}
            >
              <span>
                {sending ? translate('MESSAGING_SUBMITING') : translate('MESSAGING_SUBMIT')}
              </span>
              <span
                className={classNames('ms-1 icon', {
                  'icon-circle-right': sending,
                  'icon-envelope': !sending,
                })}
              />
            </button>
          )}
        </div>
      </div>
    </React.Fragment>
  );
};

const ChatEmailWithState = (props: any) => {
  const [state, setState] = useState(initialState);

  const sendEmail = () => {
    const { subject, body } = state;
    setState({ ...state, sending: true });
    messagingService
      .sendMessage({
        subject,
        body,
        recipients: [{ _type: 'user', user: props.recipientId }],
        uploads: [],
      })
      .then(() => {
        setState({ ...initialState, sent: true });
        window.setTimeout(() => {
          setState({ ...initialState, sent: false });
        }, SentMessageDisplayTime);
      });
  };

  return (
    <ChatEmailInner
      {...props}
      state={state}
      setState={setState}
      sendEmail={sendEmail}
    />
  );
};

const ChatEmail: React.ComponentType<ChatEmailProps> = withTranslation(
  withFocusOnMount(ChatEmailWithState)
);

export default ChatEmail;
