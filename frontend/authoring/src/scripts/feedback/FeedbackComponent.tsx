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
import * as React from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useCollapse } from 'react-collapsed';
import { IoAttachOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { useMedia } from 'react-use';
import { Badge, Button, Form, Input } from 'reactstrap';

import { trackFeedbackPageReply, trackFeedbackPanelReply } from '../analytics/AnalyticsEvents';
import CodeEditor from '../code/CodeEditor';
import { formatFullDate } from '../dateUtil';
import { dropTrailingParagraphs, isBlankHtml } from '../story/editorUtils';
import { useUserProfile } from '../user/userActions';
import { refreshFeedback } from './feedbackActions';
import FeedbackActivities from './FeedbackActivities';
import {
  editFeedback,
  FeedbackActivityDto,
  FeedbackDto,
  loadFeedbackActivity,
  MaxFeedbackLength,
  postFeedbackReply,
} from './FeedbackApi';
import FeedbackAssigneeDropdown from './FeedbackAssigneeDropdown';
import FeedbackAttachments from './FeedbackAttachments';
import {
  FeedbackAttachment,
  FeedbackAttachmentUpload,
  isStagedFile,
  onDropFeedbackAttachments,
  onPasteFeedbackAttachments,
} from './FeedbackAttachmentUpload';
import { useFeedbackFilters } from './feedbackHooks';
import FeedbackImage from './FeedbackImage';
import { FeedbackLocator } from './FeedbackLocator';
import FeedbackProfile, { profileColor } from './FeedbackProfile';
import FeedbackStatusDropdown from './FeedbackStatusDropdown';
import FeedbackText from './FeedbackText';

export const FeedbackComponent: React.FC<{
  feedback: FeedbackDto;
  feedbackPage?: boolean;
  className?: string;
  justAdded?: boolean;
}> = ({ feedback, feedbackPage, className, justAdded }) => {
  const dispatch = useDispatch();
  const yourself = useUserProfile();
  const bigly = useMedia('(min-width: 94em)');

  const [going, setGoing] = useState(false);
  const { getCollapseProps: goneCollapseProps } = useCollapse({
    defaultExpanded: true,
    isExpanded: !going,
    onTransitionStateChange: state => {
      if (state === 'collapseEnd') dispatch(refreshFeedback());
    },
  });
  const collapseAndRefresh = useCallback(() => {
    if (feedbackPage) {
      dispatch(refreshFeedback());
    } else {
      setGoing(true);
    }
  }, [setGoing]);

  const [shrunk, setShrunk] = useState(false);
  const { getCollapseProps: shrinkCollapseProps } = useCollapse({
    defaultExpanded: true,
    isExpanded: !shrunk,
  });

  const [showAttachments, setShowAttachments] = useState(false);

  const [attachments, setAttachments] = useState(new Array<FeedbackAttachment>());
  const isUploading = attachments.some(f => !isStagedFile(f));

  const [activities, setActivities] = useState(new Array<FeedbackActivityDto>());
  const { refresh } = useFeedbackFilters();
  useEffect(() => {
    loadFeedbackActivity(feedback.id).then(results => setActivities(results.objects));
  }, [feedback.id, refresh]);

  const [replyFocus, setReplyFocus] = useState(false);

  const onFocus = useCallback(() => setReplyFocus(true), [setReplyFocus]);
  //stays focused so that you can add attachments
  const onBlur = useCallback(() => setReplyFocus(true), [setReplyFocus]);
  const onDrop = useCallback(onDropFeedbackAttachments(setAttachments), [setAttachments]);

  useEffect(
    () => (replyFocus ? onPasteFeedbackAttachments(setAttachments) : undefined),
    [replyFocus]
  );

  const [reply, setReply] = useState('');
  const [replying, setReplying] = useState(false);
  const invalid = reply.length > MaxFeedbackLength;
  const isBlankReply = useMemo(() => isBlankHtml(reply), [reply]);

  const imageContent = !!feedback.quote?.match(/^http.*\.(?:jpg|jpeg|gif|png|webp|svg)$/i);

  const cannotReply = replying || (isBlankReply && !attachments.length) || isUploading || invalid;
  const onReply = () => {
    if (cannotReply) return;
    (feedbackPage ? trackFeedbackPageReply : trackFeedbackPanelReply)();
    const value = dropTrailingParagraphs(reply);
    setReplying(true);
    setShowAttachments(false);
    postFeedbackReply(feedback.id, {
      value,
      attachments: attachments.map(a => a.guid),
    })
      .then(() => {
        dispatch(refreshFeedback());
        setReplyFocus(false);
        setReply('');
        setAttachments([]);
      })
      .finally(() => setReplying(false));
  };

  const scrollNew = useCallback(
    (e: HTMLDivElement | null) => void (justAdded && e?.scrollIntoView()),
    [justAdded]
  );

  return (
    <div {...goneCollapseProps()}>
      <div
        ref={scrollNew}
        className={classNames(
          'd-flex flex-column pb-3 feedback-item',
          className,
          justAdded && 'created'
        )}
      >
        <div className="d-flex align-items-center px-3 mt-3 feedback-item-header">
          <Button
            color="transparent"
            className="p-0 border-0 me-3 position-relative"
            style={{ borderRadius: '50%' }}
            onClick={() => setShrunk(!shrunk)}
          >
            <FeedbackProfile profile={feedback.creator} />
            <span
              style={{
                position: 'absolute',
                left: '50%',
                top: '50%',
                height: 0,
                transform: `translate(-4px, 0) rotate(${
                  shrunk ? -90 : 0
                }deg) translate(0, 7px) scale(${bigly ? 1.4 : 1}`,
                color: profileColor(feedback.creator),
                transition: 'transform ease-in .3s',
              }}
            >
              <svg
                stroke="currentColor"
                fill="currentColor"
                strokeWidth="0"
                viewBox="0 0 16 16"
                height="8px"
                width="8px"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  fillRule="evenodd"
                  d="m 7.022,14.432584 a 1.13,1.13 0 0 0 1.96,0 L 15.839,2.7655838 c 0.457,-0.778 -0.092,-1.76699997 -0.98,-1.76699997 H 1.144 c -0.889,0 -1.437,0.98999997 -0.98,1.76699997 z"
                />
              </svg>
            </span>
          </Button>
          <div className="d-flex flex-column flex-grow-1 header-cluster">
            <div className="d-flex align-items-baseline status-flex">
              <div className="text-truncate fw-bold">{feedback.creator.fullName}</div>
              <Badge className="mx-2">{feedback.role}</Badge>
              <FeedbackStatusDropdown
                feedback={feedback}
                collapseAndRefresh={collapseAndRefresh}
              />
            </div>
            <div className="d-flex align-items-baseline assignee-flex">
              <div className="text-muted text-truncate me-1">
                {formatFullDate(feedback.created)}
              </div>
              <FeedbackLocator feedback={feedback} />
              <FeedbackAssigneeDropdown
                feedback={feedback}
                collapseAndRefresh={collapseAndRefresh}
              />
            </div>
          </div>
        </div>
        <div {...shrinkCollapseProps()}>
          {imageContent ? (
            <div className="px-3 mt-3">
              <FeedbackImage src={feedback.quote} />
              <div className="text-muted small mt-1">{feedback.quote}</div>
            </div>
          ) : feedback.quote ? (
            <div className="px-3 mt-3">
              <span className="feedback-txt">{feedback.quote}</span>
            </div>
          ) : null}
          <FeedbackText
            id={feedback.id}
            html={feedback.feedback}
            editable={feedback.creator.id === yourself.id}
            onEdit={html => editFeedback(feedback.id, html)}
          />
          <FeedbackAttachments
            id={feedback.id}
            attachments={feedback.attachments}
          />
          <FeedbackActivities
            id={feedback.id}
            activities={activities}
          />
          <Form
            className="d-flex flex-column mt-3 px-3 add-reply"
            onSubmit={e => {
              e.preventDefault();
            }}
          >
            {!replyFocus && isBlankReply ? (
              <Input
                type="text"
                className="flex-grow-1"
                placeholder="Reply to this feedback."
                onFocus={onFocus}
              />
            ) : (
              <>
                <CodeEditor
                  id={`reply-${feedback.id}`}
                  mode="htmlmixed"
                  size="inline"
                  value={reply}
                  onChange={setReply}
                  onBlur={onBlur}
                  onImageUpload={onDrop}
                  lineWrapping
                  placeholder="Reply to this feedback."
                  toolbar="none"
                  focus={true}
                  doneEditing={onReply}
                />
                {showAttachments && (
                  <FeedbackAttachmentUpload
                    attachments={attachments}
                    setAttachments={setAttachments}
                  />
                )}
              </>
            )}
            {invalid && (
              <div className="text-danger small mt-2">This reply is too large to submit.</div>
            )}
            {(replyFocus || !isBlankReply) && (
              <div className="d-flex justify-content-stretch mt-2">
                {!showAttachments && (
                  <Button
                    color="transparent"
                    size="sm"
                    title="Attachments"
                    className="me-auto d-flex align-items-center p-1 text-muted show-attachments"
                    onClick={() => {
                      setShowAttachments(true);
                    }}
                  >
                    <IoAttachOutline size="1.5rem" />
                  </Button>
                )}
                <Button
                  color="primary"
                  size="sm"
                  outline
                  className="me-2 ms-auto"
                  onClick={() => {
                    setReplying(false);
                    setReplyFocus(false);
                    setReply('');
                    setShowAttachments(false);
                    setAttachments([]);
                  }}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  color="primary"
                  size="sm"
                  disabled={cannotReply}
                  onClick={onReply}
                >
                  Save
                </Button>
              </div>
            )}
          </Form>
        </div>
      </div>
    </div>
  );
};
