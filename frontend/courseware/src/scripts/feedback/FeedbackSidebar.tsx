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
import { SelectionStatus, useFeedbackDtos } from '../feedback/feedback';
import { AssigneeDto } from '../feedback/feedbackApi';
import FeedbackForm from '../feedback/FeedbackForm';
import FeedbackItem from '../feedback/FeedbackItem';
import { useFeedbackOpen } from '../feedback/FeedbackStateService';
import { useCourseSelector } from '../loRedux';
import { selectContent } from '../courseContentModule/selectors/contentEntrySelectors';
import { useTranslation } from '../i18n/translationContext';
import React, { useEffect, useRef } from 'react';
import { TfiClose, TfiPlus } from 'react-icons/tfi';
import { useScroll } from 'react-use';
import { Button } from 'reactstrap';

const FeedbackSidebar: React.FC<{
  status: SelectionStatus;
  opened: boolean;
  assignees: AssigneeDto[];
  close: () => void;
  reset: () => void;
}> = ({ status, opened, assignees, close, reset }) => {
  const translate = useTranslation();
  const [, feedbackAdd, toggleFeedbackOpen] = useFeedbackOpen();
  const ref = useRef<HTMLDivElement>(null);
  const { y } = useScroll(ref);
  useEffect(() => {
    if (feedbackAdd) ref.current?.scrollTo(0, 0);
  }, [ref, feedbackAdd]);
  const content = useCourseSelector(selectContent);
  const feedback = useFeedbackDtos(content.id);

  return (
    <div
      id="FeedbackSidebar"
      className={classNames('feedback-sidebar', { scrolled: y > 0, opened })}
    >
      <div className="feedback-container">
        <div className="feedback-sidebar-header d-flex p-3 align-items-center flex-grow-0">
          <Button
            color="medium"
            outline
            className="p-1 border-0 flex-grow-0 text-muted"
            style={{ lineHeight: 1 }}
            onClick={close}
            title={translate('FEEDBACK_SIDEBAR_CLOSE')}
            aria-label={translate('FEEDBACK_SIDEBAR_CLOSE')}
          >
            <TfiClose
              aria-hidden={true}
              size="1rem"
              style={{ strokeWidth: 0.75 }}
            />
          </Button>
          <h3 className="ms-2 mb-0 text-center text-truncate flex-grow-1">
            {translate('FEEDBACK_SIDEBAR_TITLE')}
          </h3>
          {!feedbackAdd && (
            <Button
              color="medium"
              outline
              className="p-1 border-0 flex-grow-0 text-primary"
              style={{ lineHeight: 1 }}
              onClick={() => toggleFeedbackOpen(true, true)}
              title={translate('FEEDBACK_ADD_FEEDBACK')}
              aria-label={translate('FEEDBACK_ADD_FEEDBACK')}
            >
              <TfiPlus
                aria-hidden={true}
                size="1rem"
                style={{ strokeWidth: 0.5 }}
              />
            </Button>
          )}
        </div>
        <div
          className="flex-grow-1 overflow-auto"
          ref={ref}
        >
          {feedbackAdd && (
            <FeedbackForm
              status={status}
              assignees={assignees}
              reset={reset}
            />
          )}
          {feedback.map(feedback => (
            <FeedbackItem
              key={feedback.id}
              content={content}
              feedback={feedback}
            />
          ))}
          {!feedbackAdd && !feedback.length && (
            <ul className="text-dark me-3 mt-3 small">
              <li className="mb-3">
                Select text in the page content and click the Add Feedback gadget that will appear
                to provide feedback on the selected text.
              </li>
              <li className="mb-3">
                Add feedback on specific images by double-clicking on them and clicking the Add
                Feedback gadget that will appear.
              </li>
              <li className="mb-3">
                You can also provide feedback on the current page by clicking the Plus button above.
              </li>
              <li>
                If you close this sidebar you can reopen it by clicking the feedback icon in the
                content header.
              </li>
            </ul>
          )}
        </div>
      </div>
    </div>
  );
};

export default FeedbackSidebar;
