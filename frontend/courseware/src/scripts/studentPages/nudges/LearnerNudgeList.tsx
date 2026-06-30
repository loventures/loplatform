/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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
import React, { useRef } from 'react';
import { CSSTransition, TransitionGroup } from 'react-transition-group';

const LearnerNudgeList: React.FC = () => {
  const alerts = useUnviewedAlerts();
  const { isPreviewing } = useCourseSelector(selectCurrentUser);

  // React 19 removed findDOMNode, so each CSSTransition needs an explicit
  // nodeRef. The nudge body is one of several component types, so wrap it in a
  // ref'd div (the .nudge-list-* classes animate this element). Keep one stable
  // ref per alert id across renders.
  const nodeRefs = useRef(new Map<number, React.RefObject<HTMLDivElement>>());
  const nodeRefFor = (id: number) => {
    let ref = nodeRefs.current.get(id);
    if (!ref) {
      ref = React.createRef<HTMLDivElement>();
      nodeRefs.current.set(id, ref);
    }
    return ref;
  };

  return !isPreviewing ? (
    <TransitionGroup>
      {alerts.map(alert => {
        const nodeRef = nodeRefFor(alert.id);
        return (
          <CSSTransition
            timeout={500}
            classNames="nudge-list"
            key={alert.id}
            nodeRef={nodeRef}
          >
            <div ref={nodeRef}>
              {isPostNotification(alert) ? (
                <DiscussionNudge alert={alert} />
              ) : isFeedbackNotification(alert) ? (
                <FeedbackNudge alert={alert} />
              ) : isQnaNotification(alert) ? (
                <QnaNudge alert={alert} />
              ) : isInstructorMessageSentNotification(alert) ? (
                <InstructorMessageNudge alert={alert} />
              ) : null}
            </div>
          </CSSTransition>
        );
      })}
    </TransitionGroup>
  ) : null;
};

export default LearnerNudgeList;
