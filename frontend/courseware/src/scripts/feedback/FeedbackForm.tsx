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

import { IoFile, isStagedFile } from '../api/fileUploadApi';
import { ImageLikeRE, SelectionStatus } from '../feedback/feedback';
import { AssigneeDto, getFeedback, postFeedback } from '../feedback/feedbackApi';
import FeedbackAttachments from '../feedback/FeedbackAttachments';
import { setFeedbackDtos } from '../feedback/feedbackReducer';
import { useFeedbackOpen } from '../feedback/FeedbackStateService';
import { useCourseSelector } from '../loRedux';
import { RichTextEditor } from '../contentEditor/directives/richTextEditor';
import { selectContent } from '../courseContentModule/selectors/contentEntrySelectors';
import { useTranslation } from '../i18n/translationContext';
import React, { useEffect, useState } from 'react';
import { TfiClose } from 'react-icons/tfi';
import { useDispatch } from 'react-redux';
import { Button, FormGroup, Input, Label } from 'reactstrap';

const feedbackToolbar = [
  { name: 'basicstyles', items: ['Bold', 'Italic'] },
  {
    name: 'paragraph',
    items: ['NumberedList', 'BulletedList', '-', 'Outdent', 'Indent', '-', 'Blockquote'],
  },
  { name: 'links', items: ['Link', 'Unlink'] },
  { name: 'insert', items: ['Table'] },
  //  { name: 'styles', items: [ 'Styles', 'Format', 'Font', 'FontSize' ] },
];

const MaxFeedbackLength = 4096;

const FeedbackForm: React.FC<{
  status: SelectionStatus;
  assignees: AssigneeDto[];
  reset: () => void;
}> = ({ status, assignees, reset }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const content = useCourseSelector(selectContent);
  const [, , toggleFeedbackOpen] = useFeedbackOpen();
  const [contentName, setContentName] = useState(content.name);
  const [asset, setAsset] = useState(status.asset);
  const [quote, setQuote] = useState(status.quote);
  const [id, setId] = useState(status.id);
  const [feedback, setFeedback] = useState(''); // your comment
  const [attachments, setAttachments] = useState(new Array<IoFile>()); // your attachments
  const [assignee, setAssignee] = useState<number | undefined>(undefined);
  const [submitting, setSubmitting] = useState(false);

  const resetFeedback = (assignee: number | undefined = undefined) => {
    setAsset(undefined);
    setQuote(undefined);
    setId(undefined);
    setFeedback('');
    setAttachments([]);
    setAssignee(assignee);
    reset();
  };

  // if the user gadget clicks a selection while we're open then adopt it
  useEffect(() => {
    if (status.active) {
      setAsset(status.asset);
      setQuote(status.quote);
      setId(status.id);
    }
  }, [status]);

  // if the user navigates to new content, reset things
  useEffect(() => {
    if (content.name !== contentName) {
      setContentName(content.name);
      resetFeedback(assignee);
    }
  }, [content.name]);

  const isUploading = attachments.some(f => !isStagedFile(f));
  const imageLike = !!quote?.match(ImageLikeRE);
  const invalid = feedback.length > MaxFeedbackLength;

  const submitFeedback = () => {
    setSubmitting(true);
    postFeedback(content, asset, quote, id, feedback, attachments, assignee)
      .then(() => getFeedback(content))
      .then(feedback => {
        dispatch(setFeedbackDtos({ id: content.id, feedback }));
        resetFeedback(assignee); // keep the assignee
        toggleFeedbackOpen(true, false);
      })
      .finally(() => setSubmitting(false));
  };

  const closeFeedbackAdd = () => {
    toggleFeedbackOpen(true, false);
    resetFeedback(assignee);
  };

  return (
    <div className="px-3 pt-2 pb-3 border-top">
      <div className="position-relative">
        <Button
          color="medium"
          outline
          className="p-1 border-0 text-muted position-absolute"
          style={{ lineHeight: 1, top: '.2rem', right: 0 }}
          onClick={closeFeedbackAdd}
          title={translate('FEEDBACK_ADD_CLOSE')}
          aria-label={translate('FEEDBACK_ADD_CLOSE')}
        >
          <TfiClose
            aria-hidden={true}
            size=".75rem"
            style={{ strokeWidth: 0.5 }}
          />
        </Button>

        {!!quote && (
          <FormGroup>
            <Label>{translate('FEEDBACK_QUOTE')}</Label>
            {imageLike ? (
              <div>
                <a
                  className="content-img-link btn btn-primary p-0"
                  href={quote}
                  target="_blank"
                  rel="noreferrer noopener"
                >
                  <img
                    src={quote}
                    alt={translate('FEEDBACK_QUOTE_IMAGE')}
                  />
                </a>
              </div>
            ) : (
              <div className="text-muted">{quote}</div>
            )}
          </FormGroup>
        )}
        <FormGroup>
          <Label for="feedback-input">{translate('FEEDBACK_FEEDBACK_LABEL')}</Label>
          {/* CKEditor is a big loser. Because it uses an iframe, our support for
           * pasting images is broken. It ignores the placeholder. It has terrible
           * styles and too many buttons. */}
          <RichTextEditor
            placeholder={translate('FEEDBACK_FEEDBACK_PLACEHOLDER')}
            isMinimal
            content={feedback}
            onChange={setFeedback}
            toolbar={feedbackToolbar}
            focusOnRender
          />
          {invalid && (
            <div className="mt-3 text-danger small">
              This feedback is too large to submit. If you have pasted images into the text, please
              delete them and attach them using the field below.
            </div>
          )}
        </FormGroup>
        <FormGroup>
          <Label for="feedback-attachments">Attachments</Label>
          <FeedbackAttachments
            attachments={attachments}
            setAttachments={setAttachments}
          />
        </FormGroup>
        {assignees.length > 0 && (
          <FormGroup className="mb-0">
            <Label for="feedback-assignee">{translate('FEEDBACK_ASSIGNEE')}</Label>
            <Input
              id="feedback-assignee"
              value={assignee?.toString() ?? ''}
              onChange={e => setAssignee(e.target.value ? parseInt(e.target.value, 10) : undefined)}
              type="select"
            >
              <option value="">{translate('FEEDBACK_UNASSIGNED')}</option>
              {assignees.map(a => (
                <option
                  key={a.id}
                  value={a.id}
                >
                  {a.fullName}
                </option>
              ))}
            </Input>
          </FormGroup>
        )}
      </div>
      <div
        className="pt-3 d-flex justify-content-end flex-grow-0"
        style={{ borderTop: '1px solid #dee2e6', gap: '.5rem' }}
      >
        <Button
          color="primary"
          outline
          onClick={() => resetFeedback()}
        >
          {translate('FEEDBACK_RESET')}
        </Button>
        <Button
          color="primary"
          onClick={submitFeedback}
          disabled={isUploading || !feedback || submitting || invalid}
        >
          {translate('FEEDBACK_SUBMIT')}
        </Button>
      </div>
    </div>
  );
};

export default FeedbackForm;
