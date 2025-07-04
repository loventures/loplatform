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
  isFeedbackNotification,
  isInstructorMessageSentNotification,
  isPostNotification,
  isQnaNotification,
  useUnviewedAlerts,
} from '../../resources/AlertsResource';
import { useCourseSelector } from '../../loRedux';
import DiscussionNudge from '../../studentPages/nudges/DiscussionNudge';
import FeedbackNudge from '../../studentPages/nudges/FeedbackNudge';
import InstructorMessageNudge from '../../studentPages/nudges/InstructorMessageNudge';
import QnaNudge from '../../studentPages/nudges/QnaNudge';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React from 'react';
import { CSSTransition, TransitionGroup } from 'react-transition-group';

const LearnerNudgeList: React.FC = () => {
  const alerts = useUnviewedAlerts();
  const { isPreviewing } = useCourseSelector(selectCurrentUser);

  return !isPreviewing ? (
    <TransitionGroup>
      {alerts.map(alert => (
        <CSSTransition
          timeout={500}
          classNames="nudge-list"
          key={alert.id}
        >
          {isPostNotification(alert) ? (
            <DiscussionNudge alert={alert} />
          ) : isFeedbackNotification(alert) ? (
            <FeedbackNudge alert={alert} />
          ) : isQnaNotification(alert) ? (
            <QnaNudge alert={alert} />
          ) : isInstructorMessageSentNotification(alert) ? (
            <InstructorMessageNudge alert={alert} />
          ) : null}
        </CSSTransition>
      ))}
    </TransitionGroup>
  ) : null;
};

export default LearnerNudgeList;
