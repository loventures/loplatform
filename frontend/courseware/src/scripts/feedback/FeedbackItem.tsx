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

import 'yet-another-react-lightbox/styles.css';

import { ContentLite } from '../api/contentsApi';
import { ImageLikeRE } from '../feedback/feedback';
import {
  FeedbackDto,
  closeFeedback,
  getFeedback,
  getFeedbackAttachmentUrl,
  postFeedbackReply,
  reopenFeedback,
} from '../feedback/feedbackApi';
import { setFeedbackDtos } from '../feedback/feedbackReducer';
import dayjs from 'dayjs';
import localized from 'dayjs/plugin/localizedFormat';
import { useTranslation } from '../i18n/translationContext';
import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import TextareaAutosize from 'react-textarea-autosize';
import { useKey } from 'react-use';
import { Badge, Button } from 'reactstrap';
import Lightbox from 'yet-another-react-lightbox';

dayjs.extend(localized);

const FeedbackItem: React.FC<{ content: ContentLite; feedback: FeedbackDto }> = ({
  content,
  feedback,
}) => {
  const dispatch = useDispatch();
  const translate = useTranslation();
  const imageLike = !!feedback.quote?.match(ImageLikeRE);
  const [reply, setReply] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [attachments, setAttachments] = useState(new Array<any>());
  const [index, setIndex] = useState(-1);
  useKey('Escape', () => setIndex(-1));

  const updateFeedback = (promise: Promise<any>) => {
    setSubmitting(true);
    promise
      .then(() => getFeedback(content))
      .then(feedback => {
        dispatch(setFeedbackDtos({ id: content.id, feedback }));
        setReply('');
      })
      .finally(() => setSubmitting(false));
  };

  const doReply = () => {
    if (!submitting) updateFeedback(postFeedbackReply(feedback.id, reply));
  };

  const doClose = () => {
    if (!submitting) updateFeedback(closeFeedback(feedback.id));
  };

  const doReopen = () => {
    if (!submitting) updateFeedback(reopenFeedback(feedback.id));
  };

  return (
    <div className="feedback-item">
      <div
        className="d-flex justify-content-between px-3 pt-3 pb-2"
        style={{ gap: '0.5rem' }}
      >
        <div className="text-muted text-truncate">{dayjs(feedback.created).format('lll')}</div>
        <Badge
          color={feedback.closed ? 'success' : 'primary'}
          className="align-self-center text-truncate"
        >
          {feedback.status ?? translate('FEEDBACK_NEW')} (
          {feedback.assignee?.fullName ?? translate('FEEDBACK_UNASSIGNED')})
        </Badge>
      </div>
      {!!feedback.quote && (
        <div className="mb-1 mx-3">
          <div>{translate('FEEDBACK_QUOTE')}:</div>
          {imageLike ? (
            <div className="ps-3">
              <a
                className="content-img-link btn btn-primary p-0"
                href={feedback.quote}
                target="_blank"
                rel="noreferrer noopener"
              >
                <img
                  src={feedback.quote}
                  alt={translate('FEEDBACK_QUOTE_IMAGE')}
                />
              </a>
            </div>
          ) : (
            <span className="feedback-quote text-muted">{feedback.quote}</span>
          )}
        </div>
      )}
      <div className="mt-1">
        <div
          className="py-2 px-3 feedback-content overflow-hidden"
          dangerouslySetInnerHTML={{ __html: feedback.feedback }}
        />
      </div>
      {!!feedback.attachments.length && (
        <div className="feedback-attachments mt-2 mb-1 px-3">
          <div>{translate('FEEDBACK_FEEDBACK_ATTACHMENTS')}:</div>
          <div
            className="d-flex flex-wrap align-items-center mt-1"
            style={{ gap: '.25rem' }}
          >
            {feedback.attachments.map((attachment, i) => (
              <button
                className="preview p-0"
                key={attachment}
                onClick={() => {
                  setAttachments(
                    feedback.attachments.map(attachment => ({
                      id: attachment,
                      src: getFeedbackAttachmentUrl(feedback.id, attachment),
                    }))
                  );
                  setIndex(i);
                }}
              >
                <img
                  alt=""
                  src={getFeedbackAttachmentUrl(feedback.id, attachment)}
                />
              </button>
            ))}
          </div>
        </div>
      )}
      {feedback.replies.map(reply => (
        <div
          key={reply.id}
          className="feedback-reply"
        >
          <div className="d-flex justify-content-between p-2">
            <span>{reply.creator.fullName}</span>
            <span className="text-muted">{dayjs(reply.created).format('lll')}</span>
          </div>
          <div
            className="py-2 px-3 feedback-content"
            dangerouslySetInnerHTML={{ __html: reply.reply }}
          />
          {!!reply.attachments.length && (
            <div className="feedback-attachments mt-2 mb-1 px-3">
              <div>{translate('FEEDBACK_FEEDBACK_ATTACHMENTS')}:</div>
              <div
                className="d-flex flex-wrap align-items-center mt-1"
                style={{ gap: '.25rem' }}
              >
                {reply.attachments.map((attachment, i) => (
                  <button
                    className="preview p-0"
                    key={attachment}
                    onClick={() => {
                      setAttachments(
                        reply.attachments.map(attachment => ({
                          id: attachment,
                          src: getFeedbackAttachmentUrl(feedback.id, attachment),
                        }))
                      );
                      setIndex(i);
                    }}
                  >
                    <img
                      alt=""
                      src={getFeedbackAttachmentUrl(feedback.id, attachment)}
                    />
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      ))}
      <Lightbox
        open={index >= 0}
        index={index}
        close={() => setIndex(-1)}
        slides={attachments}
        animation={{ swipe: 300 }}
        carousel={{ finite: true, padding: '50px' }}
      />
      <div className="mt-3 px-3">
        {!feedback.closed && (
          <TextareaAutosize
            className="form-control"
            value={reply}
            onChange={e => setReply(e.target.value)}
            readOnly={submitting}
            placeholder={translate('FEEDBACK_REPLY')}
            spellCheck={true}
          />
        )}
        {reply && !feedback.closed ? (
          <div
            key="reply"
            className="my-3 d-flex justify-content-end"
          >
            <Button
              className="me-2"
              onClick={() => setReply('')}
              color="primary"
              outline
            >
              {translate('FEEDBACK_CANCEL_REPLY')}
            </Button>
            <Button
              color="primary"
              onClick={doReply}
              disabled={submitting}
            >
              {translate('FEEDBACK_SUBMIT_REPLY')}
            </Button>
          </div>
        ) : (
          <div
            key="transition"
            className="my-3 d-flex justify-content-end"
          >
            {feedback.status != null && (
              <Button
                onClick={doReopen}
                color="warning"
                outline
              >
                {translate('FEEDBACK_REOPEN')}
              </Button>
            )}
            {!feedback.closed && (
              <Button
                className="ms-2"
                color="success"
                onClick={doClose}
                disabled={submitting}
              >
                {translate('FEEDBACK_MARK_AS_DONE')}
              </Button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default FeedbackItem;
