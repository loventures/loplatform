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
import { useCallback, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { trackFeedbackPanelReassign } from '../analytics/AnalyticsEvents';
import { openToast } from '../toast/actions';
import { useUserProfile } from '../user/userActions';
import { refreshFeedback } from './feedbackActions';
import { FeedbackDto, assignFeedback } from './FeedbackApi';
import { useFeedbackAssignees, useFeedbackFilters } from './feedbackHooks';

const FeedbackAssigneeDropdown: React.FC<{
  feedback: FeedbackDto;
  collapseAndRefresh?: () => void;
  disabled?: boolean;
}> = ({ feedback, collapseAndRefresh, disabled }) => {
  const dispatch = useDispatch();
  const { assignee: filterAssignee } = useFeedbackFilters();
  const yourself = useUserProfile();

  const assignees = useFeedbackAssignees();
  const assigneesAndCreator = useMemo(() => {
    const result = assignees.some(a => a.id === feedback.creator.id)
      ? [...assignees]
      : [...assignees, feedback.creator];
    result.sort((a, b) => a.fullName.localeCompare(b.fullName));
    return result;
  }, [feedback, assignees]);
  const setAssignee = useCallback(
    (assignee: { id: number; fullName: string } | null) => {
      if (assignee?.id === feedback.assignee?.id) return;
      const self = assignee?.id === yourself?.id;
      trackFeedbackPanelReassign(assignee == null ? 'Unassign' : self ? 'Self' : 'Other');
      assignFeedback(feedback.id, assignee?.id).then(() => {
        dispatch(
          openToast(
            assignee
              ? `Feedback assigned to ${self ? 'yourself' : assignee.fullName}.`
              : 'Feedback unassigned.',
            'success'
          )
        );
        if (collapseAndRefresh && filterAssignee !== undefined && assignee?.id !== filterAssignee) {
          collapseAndRefresh();
        } else {
          dispatch(refreshFeedback());
        }
      });
    },
    [feedback, filterAssignee, yourself, dispatch, collapseAndRefresh]
  );

  return (
    <div className="ms-auto d-flex gap-1 align-items-center assignee-dropdown">
      <span className="vis-bigly text-muted">Assignee:</span>
      <UncontrolledDropdown>
        <DropdownToggle
          className="p-0 ps-1 assignee-dropdown"
          color="transparent"
          size="sm"
          caret
          disabled={disabled}
        >
          {feedback.assignee == null
            ? 'Unassigned'
            : feedback.assignee.id === yourself.id
              ? 'Assigned to you'
              : feedback.assignee.fullName}
        </DropdownToggle>
        <DropdownMenu end>
          <DropdownItem
            onClick={() => setAssignee(null)}
            disabled={feedback.assignee == null}
          >
            Unassign
          </DropdownItem>
          <DropdownItem
            onClick={() => setAssignee(yourself!)}
            disabled={feedback.assignee?.id === yourself?.id}
          >
            Assign to yourself
          </DropdownItem>
          {assigneesAndCreator
            .filter(a => a.id !== yourself?.id)
            .map(assignee => (
              <DropdownItem
                key={assignee.id}
                onClick={() => setAssignee(assignee)}
                disabled={feedback.assignee?.id === assignee.id}
              >
                {assignee.fullName}
              </DropdownItem>
            ))}
        </DropdownMenu>
      </UncontrolledDropdown>
    </div>
  );
};

export default FeedbackAssigneeDropdown;
