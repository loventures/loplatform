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

import { formatFullDate } from '../dateUtil';
import { useUserProfile } from '../user/userActions';
import {
  FeedbackActivityDto,
  FeedbackProfileDto,
  InitialStatus,
  deleteFeedbackReply,
  editFeedbackReply,
} from './FeedbackApi';
import FeedbackAttachments from './FeedbackAttachments';
import FeedbackProfile from './FeedbackProfile';
import FeedbackText from './FeedbackText';

const isProfile = (value: null | string | FeedbackProfileDto): value is FeedbackProfileDto =>
  value != null && typeof value === 'object';

const FeedbackActivities: React.FC<{
  id: number;
  activities: FeedbackActivityDto[];
}> = ({ id, activities }) => {
  const yourself = useUserProfile();
  return (
    <>
      {activities.map(activity => {
        const creator = activity.creator.id === yourself?.id ? 'You' : activity.creator.fullName;
        return (
          <div
            key={activity.id}
            className={classNames('feedback-activity', activity.event.toLowerCase())}
          >
            {activity.event === 'Status' ? (
              <div className="px-3">
                <span className="text-muted me-2">{formatFullDate(activity.created)}</span>
                {`${creator} changed status to ${activity.value ?? InitialStatus}.`}
              </div>
            ) : activity.event === 'Assign' ? (
              <div className="px-3">
                <span className="text-muted me-2">{formatFullDate(activity.created)}</span>
                {isProfile(activity.value)
                  ? `${creator} assigned to ${
                      activity.value.id === yourself?.id
                        ? activity.creator.id === yourself?.id
                          ? 'yourself'
                          : 'you'
                        : activity.value.id === activity.creator.id
                          ? 'themself'
                          : activity.value.fullName
                    }.`
                  : `${creator} unassigned.`}
              </div>
            ) : (
              <>
                <div className="d-flex align-items-center px-3">
                  <FeedbackProfile
                    className="me-3"
                    profile={activity.creator}
                  />
                  <div className="d-flex flex-column flex-grow-1">
                    <div className="fw-bold">{activity.creator.fullName}</div>
                    <div className="text-muted">
                      {formatFullDate(activity.created)}
                      {activity.edited != null && <span className="small ms-1">(edited)</span>}
                    </div>
                  </div>
                </div>
                <FeedbackText
                  id={activity.id}
                  html={activity.value as string}
                  editable={activity.creator.id === yourself.id}
                  onEdit={value => editFeedbackReply(id, activity.id, { value })}
                  onDelete={() => deleteFeedbackReply(id, activity.id)}
                />
                <FeedbackAttachments
                  id={id}
                  attachments={activity.attachments}
                />
              </>
            )}
          </div>
        );
      })}
    </>
  );
};

export default FeedbackActivities;
