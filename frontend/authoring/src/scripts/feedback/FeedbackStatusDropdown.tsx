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

import * as React from 'react';
import { useCallback } from 'react';
import { useDispatch } from 'react-redux';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { trackFeedbackPanelStatus } from '../analytics/AnalyticsEvents';
import { openToast } from '../toast/actions';
import { useUserProfile } from '../user/userActions';
import { feedbackDeleted, refreshFeedback } from './feedbackActions';
import {
  FeedbackDto,
  FeedbackStatus,
  FeedbackStatuses,
  InitialStatus,
  deleteFeedback,
  feedbackColor,
  isClosedStatus,
  nullInitialStatus,
  transitionFeedback,
} from './FeedbackApi';
import { useFeedbackFilters } from './feedbackHooks';

const FeedbackStatusDropdown: React.FC<{
  feedback: FeedbackDto;
  collapseAndRefresh?: () => void;
  disabled?: boolean;
}> = ({ feedback, collapseAndRefresh, disabled }) => {
  const dispatch = useDispatch();
  const { status: filterStatus } = useFeedbackFilters();
  const user = useUserProfile();

  const onSetStatus = useCallback(
    (status: FeedbackStatus | null) => {
      if (status === feedback.status) return;
      const statusName = status || InitialStatus;
      trackFeedbackPanelStatus(statusName);
      transitionFeedback(feedback.id, status, isClosedStatus(status)).then(() => {
        dispatch(openToast(`Feedback set to ${statusName}.`, 'success'));
        // kinda crappy but if the new state will not show in my filter then
        // delay refresh until after the UI component collapses
        if (
          collapseAndRefresh &&
          (filterStatus === 'Closed'
            ? !isClosedStatus(status)
            : filterStatus === 'Open'
              ? isClosedStatus(status)
              : status !== filterStatus)
        ) {
          collapseAndRefresh();
        } else {
          dispatch(refreshFeedback());
        }
      });
    },
    [feedback, filterStatus, dispatch, collapseAndRefresh]
  );

  const onDeleteFeedback = useCallback(() => {
    if (window.confirm('Delete this feedback?')) {
      trackFeedbackPanelStatus('Delete');
      deleteFeedback(feedback.id).then(() => {
        dispatch(openToast(`Feedback deleted.`, 'success'));
        dispatch(feedbackDeleted(feedback.id));
        if (collapseAndRefresh) {
          collapseAndRefresh();
        } else {
          dispatch(refreshFeedback());
        }
      });
    }
  }, [feedback, dispatch, collapseAndRefresh]);

  return (
    <div className="ms-auto d-flex gap-2 align-items-center status-dropdown">
      <span className="vis-bigly text-muted">Status:</span>
      <UncontrolledDropdown>
        <DropdownToggle
          className="badge status-dropdown"
          caret
          size="sm"
          color={feedbackColor(feedback.status)}
          disabled={disabled}
        >
          {feedback.status || InitialStatus}
        </DropdownToggle>
        <DropdownMenu end>
          {FeedbackStatuses.map(status => (
            <DropdownItem
              key={status}
              onClick={() => onSetStatus(nullInitialStatus(status))}
              disabled={feedback.status === nullInitialStatus(status)}
            >
              {status}
            </DropdownItem>
          ))}
          {feedback.creator.id === user.id && (
            <DropdownItem
              onClick={onDeleteFeedback}
              className="text-danger"
            >
              Delete
            </DropdownItem>
          )}
        </DropdownMenu>
      </UncontrolledDropdown>
    </div>
  );
};

export default FeedbackStatusDropdown;
