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

import { withTranslation } from '../../i18n/translationContext';
import React, { useEffect, useRef, useState } from 'react';

import ChatOutput from './ChatOutput';
import { chatAPI as ChatAPI } from '../../services/chatAPI.ts';
import { presenceSession } from '../../presence/presenceSessionImpl';
import { Alert, ModalBody, ModalFooter, Form, Input } from 'reactstrap';

const ChatMessages = ({ roomId, chatToUser, isContextOffline, offlineMessage, translate }: any) => {
  const visibility = presenceSession.visibility;

  const inputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState(false);
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.focus();
    }
    return () => {
      ChatAPI.notifyTypingStatus(roomId, false);
    };
  }, [roomId]);

  const memo = useRef(false);
  const typing = !!message;
  useEffect(() => {
    if (memo.current !== typing) {
      memo.current = typing;
      ChatAPI.notifyTypingStatus(roomId, typing);
    }
  }, [roomId, typing]);

  return (
    <>
      <ModalBody className="chat-body">
        {visibility ? (
          <ChatOutput
            roomId={roomId}
            showFullName={!chatToUser}
          />
        ) : (
          <Alert
            className="m-2"
            color="info"
          >
            {translate('NO_CHAT_WHEN_INVISIBLE')}
          </Alert>
        )}
      </ModalBody>
      <ModalFooter className="chat-footer flex-column align-items-stretch">
        {isContextOffline ? (
          <Alert
            className="m-2"
            color="danger"
          >
            {offlineMessage}
          </Alert>
        ) : (
          <Form
            onSubmit={e => {
              e.preventDefault();
              if (submitting) return;
              setSubmitting(true);
              ChatAPI.sendChatMessage(roomId, { message }).then(
                () => {
                  setMessage('');
                  setError(false);
                  setSubmitting(false);
                },
                () => {
                  setError(true);
                  setSubmitting(false);
                }
              );
            }}
          >
            <Input
              type="text"
              className="form-control chat-input"
              name="message"
              value={message}
              onChange={e => setMessage(e.target.value)}
              autoComplete="off"
              placeholder={translate('CHAT_MESSAGE_PLACEHOLDER')}
              disabled={!visibility}
              innerRef={inputRef}
            />
            {error && (
              <Alert
                id="chat-submit-error"
                className="mt-2 text-left"
                color="danger"
              >
                {translate('CHAT_MESSAGE_ERROR')}
              </Alert>
            )}
          </Form>
        )}
      </ModalFooter>
    </>
  );
};

export default withTranslation(ChatMessages);
