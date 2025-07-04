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

import {
  addNewMessage,
  instructorCloseMessage,
  putCategorization,
  QnaQuestionDto,
} from '../qna/qnaApi';
import React, { useEffect, useMemo, useState } from 'react';
import { FiSend } from 'react-icons/fi';
import { useDispatch } from 'react-redux';
import Select from 'react-select';
import { Button, FormGroup, Label } from 'reactstrap';

import { IoFile, isStagedFile } from '../api/fileUploadApi';
import { RichTextEditor } from '../contentEditor/directives/richTextEditor';
import { useTranslation } from '../i18n/translationContext';
import GroupMessagesByDate from './GroupMessagesByDate';
import { updateQnaQuestion } from './qnaActions';
import QnaAttachments from './QnaAttachments';

/** This belongs as domain config but we have no convenient place for it right now. */
const categoryMap: Record<string, string[]> = window.lo_platform.preferences.qnaCategories ?? {};

const answerToolbar = [
  { name: 'basicstyles', items: ['Bold', 'Italic'] },
  {
    name: 'paragraph',
    items: ['NumberedList', 'BulletedList', '-', 'Outdent', 'Indent', '-', 'Blockquote'],
  },
  { name: 'links', items: ['Link', 'Unlink'] },
  { name: 'insert', items: ['Maximize'] },
];

const AnswerQuestion: React.FC<{ question: QnaQuestionDto; onAddMessage?: () => void }> = ({
  question,
  onAddMessage,
}) => {
  const dispatch = useDispatch();
  const translate = useTranslation();

  const [messageInput, setMessageInput] = useState('');
  const [attachments, setAttachments] = useState(new Array<IoFile>());

  const [category, setCategory] = useState<string | undefined>(question.category);
  const [subcategory, setSubcategory] = useState<string | undefined>(question.subcategory);
  const [recategorize, setRecategorize] = useState(false);

  const [inputFocused, setInputFocused] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const categories = useMemo(
    () => Object.keys(categoryMap).map(o => ({ value: o, label: o })),
    [categoryMap]
  );

  // The react-json-schema-editor ui cannot encode an empty array, so we encode the empty
  // "Other" category as Other: ["Other"] and so if we see that we filter to empty list.
  const subcategories = useMemo(
    () =>
      (categoryMap[category ?? ''] ?? [])
        .filter((v, _, a) => a.length > 1 || v !== category)
        .map(o => ({ value: o, label: o })),
    [categoryMap, category]
  );

  useEffect(() => {
    setInputFocused(false);
    setMessageInput('');
    setAttachments([]);
  }, [question]);

  const canCategorize =
    (!categories.length || !!category) && (!subcategories.length || !!subcategory);

  const canSubmit = !!messageInput && canCategorize;

  const addMessage = () => {
    if (canSubmit && !submitting) {
      setSubmitting(true);

      addNewMessage(question.id, messageInput, attachments, category, subcategory)
        .then(updatedQuestion => {
          dispatch(updateQnaQuestion(updatedQuestion));
          setMessageInput('');
          setAttachments([]);
          onAddMessage?.();
        })
        .finally(() => setSubmitting(false));
    }
  };

  const closeMessage = () => {
    if (canCategorize && !submitting) {
      setSubmitting(true);

      instructorCloseMessage(question.id, category, subcategory)
        .then(updatedQuestion => {
          dispatch(updateQnaQuestion(updatedQuestion));
        })
        .finally(() => setSubmitting(false));
    }
  };

  const updateCategorization = () => {
    if (canCategorize && !submitting) {
      setSubmitting(true);
      putCategorization(question.id, category, subcategory)
        .then(updatedQuestion => {
          dispatch(updateQnaQuestion(updatedQuestion));
          setRecategorize(false);
        })
        .finally(() => setSubmitting(false));
    }
  };

  const isUploading = attachments.some(f => !isStagedFile(f));

  return (
    <div className="d-flex flex-column justify-content-between">
      <div className="d-flex justify-content-start flex-column border-dark group-by-date">
        <GroupMessagesByDate question={question} />
      </div>

      {!question.closed && !question.instructorMessage ? (
        <div
          className="qna-compose d-flex flex-column align-items-stretch py-2 px-2"
          style={{ position: 'sticky', bottom: 0 }}
        >
          {(!question.category || recategorize) && !!categories.length && (
            <Select
              options={categories}
              value={categories.find(cat => cat.value === category) ?? null}
              onChange={v => {
                setCategory(v?.value);
                setSubcategory(undefined);
              }}
              placeholder="Select question category"
              menuPosition="fixed"
              className="mt-1"
              styles={{
                control: x => (messageInput && !category ? { ...x, borderColor: 'red' } : x),
              }}
            />
          )}
          {(!question.category || recategorize) && !!subcategories.length && (
            <Select
              options={subcategories}
              value={subcategories.find(cat => cat.value === subcategory) ?? null}
              onChange={v => setSubcategory(v?.value)}
              placeholder="Select sub-category"
              menuPosition="fixed"
              className="mt-1"
              styles={{
                control: css =>
                  messageInput && !subcategory ? { ...css, borderColor: 'red' } : css,
              }}
            />
          )}
          {recategorize ? (
            <div className="d-flex justify-content-end mt-2">
              <Button
                color="primary"
                outline
                onClick={() => setRecategorize(false)}
              >
                Cancel
              </Button>
              <Button
                color="primary"
                className="ms-2"
                disabled={submitting || !canCategorize}
                onClick={updateCategorization}
              >
                Submit
              </Button>
            </div>
          ) : (
            <>
              <div className="chat-row">
                {inputFocused ? (
                  <RichTextEditor
                    isMinimal
                    focusOnRender
                    minHeight={50}
                    className="form-control qna-form-control"
                    placeholder={translate('QNA_ANSWER_PLACEHOLDER')}
                    content={messageInput}
                    onChange={setMessageInput}
                    toolbar={answerToolbar}
                    disabled={isUploading || submitting}
                  />
                ) : (
                  <textarea
                    rows={1}
                    className="form-control qna-form-control"
                    placeholder={translate('QNA_ANSWER_PLACEHOLDER')}
                    value={messageInput}
                    onFocus={() => setInputFocused(true)}
                  ></textarea>
                )}
              </div>
              <div className="d-flex flex-row flex-md-column">
                <FormGroup className="qna-attachments w-100 mb-0 mb-md-2">
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
                <Button
                  className="qna-submit ms-2 ms-md-0"
                  onClick={addMessage}
                  aria-label="Send"
                  disabled={submitting || !canSubmit}
                >
                  {translate('QNA_SEND')}
                  <FiSend className="ms-1" />
                </Button>
                <Button
                  color="primary"
                  className="qna-close ms-2 ms-md-0 mt-2"
                  onClick={closeMessage}
                  aria-label="Send"
                  disabled={submitting || !canCategorize}
                >
                  {translate('QNA_CLOSE_MESSAGE')}
                </Button>
              </div>
              {question?.category && (
                <>
                  <div
                    className="text-muted small text-center px-4 mt-3"
                    style={{ margin: '-.5rem 0 .5rem' }}
                  >
                    {question.subcategory
                      ? `${question.category} / ${question.subcategory}`
                      : question.category}
                  </div>
                  <Button
                    color="primary"
                    size="sm"
                    className="mt-2 align-self-center"
                    onClick={() => setRecategorize(true)}
                  >
                    Change Categorization
                  </Button>
                </>
              )}
            </>
          )}
        </div>
      ) : (
        <div
          className="text-muted small text-center px-4 mt-3"
          style={{ margin: '-.5rem 0 .5rem' }}
        >
          {question.subcategory
            ? `${question.category} / ${question.subcategory}`
            : question.category}
        </div>
      )}
    </div>
  );
};

export default AnswerQuestion;
