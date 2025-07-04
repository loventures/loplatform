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

import classNames from 'classnames';
import { useCourseSelector } from '../loRedux';
import CloseQuestionPrompt from '../qna/CloseQuestionPrompt';
import {
  QnaQuestionDto,
  addNewMessage,
  fetchQnaSummaries,
  multicastReply,
  postNewQuestion,
} from '../qna/qnaApi';
import { RichTextEditor } from '../contentEditor/directives/richTextEditor';
import { selectCurrentUser } from '../utilities/rootSelectors';
import React, { useState } from 'react';
import { FiSend } from 'react-icons/fi';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';
import { Button, FormGroup, Label } from 'reactstrap';

import { ContentLite } from '../api/contentsApi';
import { IoFile, isStagedFile } from '../api/fileUploadApi';
import { useTranslation } from '../i18n/translationContext';
import { StudentQnaQuestionLink } from '../utils/pageLinks';
import GroupMessagesByDate from './GroupMessagesByDate';
import { addQnaQuestion, setQnaSummaries, updateQnaQuestion } from './qnaActions';
import QnaAttachments from './QnaAttachments';

const questionToolbar = [
  { name: 'basicstyles', items: ['Bold', 'Italic'] },
  {
    name: 'paragraph',
    items: ['NumberedList', 'BulletedList', '-', 'Outdent', 'Indent', '-', 'Blockquote'],
  },
  { name: 'links', items: ['Link', 'Unlink'] },
  { name: 'insert', items: ['Maximize'] },
];

const AskQuestion: React.FC<{
  content: ContentLite;
  question?: QnaQuestionDto;
  firstQuestion?: boolean;
  onAddMessage?: () => void;
}> = ({ content, question, firstQuestion, onAddMessage }) => {
  const dispatch = useDispatch();
  const translate = useTranslation();
  const history = useHistory();

  const [messageInput, setMessageInput] = useState('');
  const [attachments, setAttachments] = useState(new Array<IoFile>());

  const [inputFocused, setInputFocused] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const canSubmit = !!messageInput;
  const user = useCourseSelector(selectCurrentUser);

  const afterMessage = () => {
    setMessageInput('');
    setAttachments([]);
    onAddMessage?.();
  };

  const addMessage = () => {
    if (canSubmit && !submitting) {
      setSubmitting(true);
      if (question && question.instructorMessage) {
        multicastReply(question.id, messageInput, attachments).then(newQuestion => {
          dispatch(addQnaQuestion(newQuestion));
          afterMessage();
          fetchQnaSummaries({
            prefilter: { property: 'creator', operator: 'eq', value: user.id },
          }).then(summaries => {
            dispatch(setQnaSummaries(summaries));
            history.push(StudentQnaQuestionLink.toLink(newQuestion.id));
          });
        });
      } else if (question) {
        addNewMessage(question.id, messageInput, attachments)
          .then(updatedQuestion => {
            dispatch(updateQnaQuestion(updatedQuestion));
            afterMessage();
            fetchQnaSummaries({
              prefilter: { property: 'creator', operator: 'eq', value: user.id },
            }).then(summaries => {
              dispatch(setQnaSummaries(summaries));
            });
          })
          .finally(() => setSubmitting(false));
      } else if (content) {
        postNewQuestion(content, messageInput, attachments)
          .then(question => {
            dispatch(addQnaQuestion(question));
            afterMessage();
          })
          .finally(() => setSubmitting(false));
      }
    }
  };

  const isUploading = attachments.some(f => !isStagedFile(f));

  return (
    <div className="d-flex flex-column">
      {question && (
        <div className="d-flex justify-content-start flex-column border-dark group-by-date">
          <GroupMessagesByDate question={question} />
          {question.reopened || question.closed ? null : question.open ? (
            <span className="text-center my-3 text-warning small">
              {translate('QNA_PENDING_RESPONSE')}
            </span>
          ) : (
            <CloseQuestionPrompt question={question} />
          )}
        </div>
      )}
      {!question && (
        <div className="d-flex align-items-center flex-column py-2">
          <div
            className={classNames(
              'system-message small mb-4 text-center px-2',
              !firstQuestion && 'mt-4 text-warning'
            )}
          >
            {translate(firstQuestion ? 'QNA_EMPTY_STATE' : 'QNA_FOLLOWUP_STATE', {
              title: content.name ?? translate('QNA_THIS_COURSE'),
            })}
          </div>
        </div>
      )}
      {(question?.instructorMessage || !question?.closed) && !user.isPreviewing && (
        <div
          className="qna-compose d-flex flex-column align-items-stretch py-2 px-2"
          style={{ position: 'sticky', bottom: 0 }}
        >
          <>
            <div className="chat-row">
              {inputFocused ? (
                <RichTextEditor
                  isMinimal
                  focusOnRender
                  minHeight={50}
                  className="form-control qna-form-control"
                  placeholder={translate('QNA_INPUT_PLACEHOLDER')}
                  content={messageInput}
                  onChange={setMessageInput}
                  toolbar={questionToolbar}
                  disabled={isUploading || submitting}
                />
              ) : (
                <textarea
                  rows={1}
                  className="form-control qna-form-control"
                  placeholder={translate('QNA_INPUT_PLACEHOLDER')}
                  value={messageInput}
                  onFocus={() => setInputFocused(true)}
                ></textarea>
              )}
            </div>
            <FormGroup className="qna-attachments w-100 mb-0 mb-md-2">
              <Label
                for="qna-attachments"
                className="sr-only"
              >
                Attachments
              </Label>
              <QnaAttachments
                attachments={attachments}
                setAttachments={setAttachments}
              />
            </FormGroup>
            <Button
              className="qna-submit ms-2 ms-md-0"
              onClick={addMessage}
              aria-label="Send"
              disabled={submitting || !canSubmit}
            >
              <FiSend />
            </Button>
          </>
        </div>
      )}
    </div>
  );
};

export default AskQuestion;
