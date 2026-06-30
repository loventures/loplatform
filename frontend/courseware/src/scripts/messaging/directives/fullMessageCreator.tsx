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

import classnames from 'classnames';
import React, { useReducer, useState } from 'react';

import { FeedbackTools } from '../../assignmentFeedback/directives/feedbackTools.tsx';
import { RichTextEditor } from '../../contentEditor/directives/richTextEditor.tsx';
import { richTextEditor } from '../../contentEditor/index.js';
import { useTranslation } from '../../i18n/translationContext';
import { FullMessage } from '../FullMessage.ts';
import { RecipientPicker } from './recipientPicker.tsx';

const newFullMessage = (): any => new (FullMessage as new () => any)();

/**
 * React port of the `fullMessageCreator` directive (B2, messaging): the message-compose form —
 * recipient picker, subject, rich-text body, attachment/audio tools, and send. Previously an Angular
 * directive bridged into React via angular2react; now native React, composing the already-React
 * `RecipientPicker` (#1481) + `RichTextEditor` (#1461) + the now-React `FeedbackTools` (the audio
 * recorder within it stays Angular via an angular2react bridge). Its only renderer is the React
 * `SendMessage` page. The `FullMessage` model stays Angular (via lojector). The old `$broadcast('validate')`
 * is the `validateCount` the RecipientPicker watches. DOM preserved for `SendMessagePage`:
 * `.full-message-creator`, `.message-subject` (+ `.invalid-feedback`), `input[name=titlefield]`,
 * `.message-controls button.btn-primary`, `.message-success`, the error status.
 */
export const FullMessageCreator: React.FC<{ recipients?: any[]; entireClass?: string }> = ({
  recipients,
  entireClass,
}) => {
  const translate = useTranslation();

  const [message, setMessage] = useState<any>(() => {
    const m = newFullMessage();
    m.selectingEntireClass = entireClass === 'true';
    m.setSelection(recipients);
    return m;
  });
  const [messageGen, setMessageGen] = useState(0);
  const [validateCount, setValidateCount] = useState(0);
  const [titleError, setTitleError] = useState('');
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState(false);
  const [errorContent, setErrorContent] = useState<any>(null);
  const [, forceRender] = useReducer((x: number) => x + 1, 0);

  const resetMessage = () => {
    setMessage(newFullMessage());
    setMessageGen(g => g + 1);
  };

  const uiMessageSent = () => {
    setSent(true);
    window.setTimeout(() => setSent(false), 1000 * 5);
    // Force them to see the green success, THEN let them type again.
    window.setTimeout(() => resetMessage(), 1000);
  };

  const sendMessage = () => {
    // Child directives validate their own state; bump the counter the RecipientPicker watches.
    setValidateCount(c => c + 1);
    setTitleError(message.title ? '' : translate('MESSAGING_ENTER_SUBJECT_ERROR'));

    if (message.hasRecipients() && message.title) {
      setSending(true);
      setSent(false);
      setError(false);
      message.send().then(
        () => {
          setSending(false);
          resetMessage();
          uiMessageSent();
        },
        (err: any) => {
          // eslint-disable-next-line no-console
          console.error('Error sending message', err);
          setError(true);
          setErrorContent(err);
          setSending(false);
          setSent(false);
        }
      );
    }
  };

  const validateAndSend = () => {
    message.feedbackManager.confirmResetByModal().then(sendMessage);
  };

  return (
    <div className="full-message-creator">
      <form name="message-form">
        <div className="form-group">
          <RecipientPicker
            message={message}
            validateCount={validateCount}
          />
        </div>

        <div className="form-group message-subject">
          <div className="text-input-decorator">
            <div className={classnames('input-line', { 'validation-error': !!titleError })}>
              <input
                className="form-control"
                type="text"
                name="titlefield"
                value={message.title || ''}
                placeholder={translate('MESSAGING_ENTER_SUBJECT')}
                onChange={e => {
                  message.title = e.target.value;
                  forceRender();
                }}
              />
              <label
                className="required-asterisk"
                aria-label={translate('TEXT_INPUT_REQUIRED')}
              >
                *
              </label>
            </div>
            {!!titleError && <div className="invalid-feedback">{titleError}</div>}
          </div>
        </div>

        <div className="form-group">
          <RichTextEditor
            key={messageGen}
            content={message.content}
            onChange={(html: string) => {
              message.content = html;
            }}
          />

          <div className="media-items">
            <FeedbackTools
              key={messageGen}
              feedbackManager={message.feedbackManager}
            />
          </div>
        </div>

        <div className="form-group message-controls">
          <button
            className="btn btn-primary"
            type="button"
            disabled={sending}
            onClick={validateAndSend}
          >
            {!sending && (
              <>
                <span>{translate('MESSAGING_SUBMIT')}</span>
                <span
                  className="icon icon-envelope"
                  aria-hidden="true"
                />
              </>
            )}
            {sending && (
              <>
                <span>{translate('MESSAGING_SUBMITING')}</span>
                <span
                  className="icon icon-circle-right"
                  aria-hidden="true"
                />
              </>
            )}
          </button>

          {sent && (
            <div className="status-message text-success">
              <span className="icon icon-envelope message-success" />
              <span className="message-success">{translate('MESSAGING_SENT')}</span>
            </div>
          )}

          {error && (
            <div className="status-message text-danger">
              <span className="icon icon-warning message-error" />
              <span className="message-error">{translate('MESSAGING_ERROR')}</span>
              <span className="message-error">{String(errorContent ?? '')}</span>
            </div>
          )}
        </div>
      </form>
    </div>
  );
};

