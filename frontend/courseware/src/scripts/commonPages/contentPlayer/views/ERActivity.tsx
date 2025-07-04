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
import { UserInfo } from '../../../../loPlatform';
import ERContentTitle from '../../../commonPages/contentPlayer/ERContentTitle';
import ERLockedActivity from '../../../commonPages/contentPlayer/views/ERLockedActivity';
import TooltippedGradeBadge from '../../../components/TooltippedGradeBadge';
import { useContentGatingInfoResource } from '../../../resources/GatingInformationResource';
import { ActivityProps } from '../../../contentPlayerComponents/activityViews/ActivityProps';
import DiscussionActivity from '../../../contentPlayerComponents/activityViews/discussion/DiscussionActivity';
import QuizActivityInstructor from '../../../contentPlayerComponents/activityViews/quiz/QuizActivityInstructor';
import QuizActivityLearner from '../../../contentPlayerComponents/activityViews/quiz/QuizActivityLearner';
import ResourceActivity, {
  ResourceTypes,
} from '../../../contentPlayerComponents/activityViews/resource/ResourceActivity';
import SubmissionActivityInstructor from '../../../contentPlayerComponents/activityViews/submission/SubmissionActivityInstructor';
import SubmissionActivityLearnerLoader from '../../../contentPlayerComponents/activityViews/submission/SubmissionActivityLearnerLoader';
import { ErrorState } from '../../../contentPlayerComponents/contentViews/ContentIndex/PlainIndex';
import { ContentWithRelationships } from '../../../courseContentModule/selectors/assembleContentView';
import { ViewingAs } from '../../../courseContentModule/selectors/contentEntry';
import {
  CONTENT_TYPE_ASSESSMENT,
  CONTENT_TYPE_ASSIGNMENT,
  CONTENT_TYPE_CHECKPOINT,
  CONTENT_TYPE_DIAGNOSTIC,
  CONTENT_TYPE_DISCUSSION,
  CONTENT_TYPE_OBSERVATION_ASSESSMENT,
  CONTENT_TYPE_POOLED_ASSESSMENT,
} from '../../../utilities/contentTypes';
import React from 'react';
import { Alert } from 'reactstrap';
import { ErrorBoundary } from 'react-error-boundary';

export type ERActivityProps = ActivityProps<any> & {
  actualUser: UserInfo;
  viewingAs: ViewingAs;
};

const getActivityComponent = (content: ContentWithRelationships, viewingAs: ViewingAs) => {
  switch (content.typeId) {
    case CONTENT_TYPE_ASSESSMENT:
    case CONTENT_TYPE_CHECKPOINT:
    case CONTENT_TYPE_POOLED_ASSESSMENT:
    case CONTENT_TYPE_DIAGNOSTIC:
      return viewingAs.isInstructor ? QuizActivityInstructor : QuizActivityLearner;

    case CONTENT_TYPE_ASSIGNMENT:
    case CONTENT_TYPE_OBSERVATION_ASSESSMENT:
      return viewingAs.isInstructor
        ? SubmissionActivityInstructor
        : SubmissionActivityLearnerLoader;

    case CONTENT_TYPE_DISCUSSION:
      return DiscussionActivity;

    default:
      return ErrorState;
  }
};

const ERActivity: React.FC<ERActivityProps> = props => {
  const { content, viewingAs, printView } = props;
  const gatingInfo = useContentGatingInfoResource(content.id, viewingAs.id);

  if (gatingInfo.isLocked) {
    return <ERLockedActivity {...props} />;
  } else if (ResourceTypes.has(content.typeId)) {
    return <ResourceActivity {...props} />;
  } else {
    const ActivityComponent = getActivityComponent(content, viewingAs);
    const feedbackContext = content.typeId !== CONTENT_TYPE_DISCUSSION;
    return (
      <div className="card er-content-wrapper">
        <div className="card-body">
          <ERContentTitle
            content={content}
            printView={printView}
          />
          <TooltippedGradeBadge />
          <div
            className={classNames('er-expandable-activity', feedbackContext && 'feedback-context')}
          >
            <ErrorBoundary
              fallback={
                <div className="p-4">
                  <Alert color="danger">Something went wrong.</Alert>
                </div>
              }
            >
              <ActivityComponent
                key={content.id}
                {...props}
              />
            </ErrorBoundary>
          </div>
        </div>
      </div>
    );
  }
};

export default ERActivity;
