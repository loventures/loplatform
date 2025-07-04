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

import { DiscussionBoard } from '../../../discussion/index.js';
import Tutorial from '../../../tutorial/Tutorial.js';
import { get } from 'lodash';
import { RubricGrid } from '../../../assignmentGrade/directives/rubricGrid/rubricGrid.jsx';
import { GraderJumpButton } from '../../../assignmentGrader/jumper/graderJumpButtonLight.js';
import { loadDiscussionActivityActionCreator } from '../../../courseActivityModule/actions/discussionActivityActions.js';
import {
  contentDiscussionActivitySelector,
  selectContentActivityLoaderComponent,
} from '../../../courseActivityModule/selectors/discussionActivitySelectors.js';
import { withTranslation } from '../../../i18n/translationContext.js';
import { createLoaderComponent } from '../../../utilities/withLoader.js';
import React from 'react';
import { connect } from 'react-redux';

import InstructorNotifications from '../../../components/InstructorNotifications.js';
import ActivityInfo from '../../parts/ActivityInfo.js';
import ContentBlockInstructions from '../../parts/ContentBlockInstructions.jsx';

const getHasBlockInstructions = content => {
  return 'block' === get(content, 'activity.discussion.instructions.instructionType');
};

class DiscussionActivity extends React.Component {
  componentDidMount() {
    this.props.onLoaded?.();
  }

  render() {
    const { translate, content, discussion, rubricScore, viewingAs, actualUser, printView } =
      this.props;
    //yes rubric.rubric
    const rubric = discussion.rubric ? discussion.rubric.rubric : null;
    const rubricResponse = rubricScore ? rubricScore.rubricResponse : null;

    const hasBlockInstructions = getHasBlockInstructions(content);
    const isClosed =
      get(content, 'availability.isClosed') || get(content, 'activity.discussion.closed');
    const isTrialLearner = viewingAs.isUnderTrialAccess;
    const isRestricted = isClosed || isTrialLearner;

    return (
      <div className="discussion-activity">
        {isRestricted && (
          <div className="access-info access-closed">
            {isClosed && (
              <span className="access-info-message access-closed">
                {translate('DISCUSSION_CLOSED_MESSAGE')}
              </span>
            )}
            {!isClosed && isTrialLearner && (
              <span className="access-info-message access-trial">
                {translate('DISCUSSION_TRIAL_ACCESS_MESSAGE')}
              </span>
            )}
          </div>
        )}
        <ActivityInfo
          content={content}
          hasAttempt={!!rubricScore}
        />

        {viewingAs.isStudent ? <InstructorNotifications contentId={content.id} /> : null}

        {viewingAs.id !== actualUser.id && content.hasGradebookEntry && (
          <div className="flex-center-center p-3">
            <GraderJumpButton
              content={content}
              type="discussion"
            />
          </div>
        )}

        {hasBlockInstructions && (
          <ContentBlockInstructions instructions={content.activity.discussion.instructions.block} />
        )}

        {rubric && (
          <RubricGrid
            rubric={rubric}
            rubricResponse={rubricResponse}
          />
        )}

        {!printView && (
          <DiscussionBoard
            discussionId={content.id}
            contentItemId={content.id}
            isOpen={!isClosed}
            isClosed={isClosed}
            gatingPolicies={content.gatingPolicies}
            printView={printView}
          />
        )}

        {!isRestricted && viewingAs.isStudent && !printView && (
          <Tutorial name="discussion-introduction" />
        )}
      </div>
    );
  }
}

const ConnectedDiscussionActivity = connect(
  contentDiscussionActivitySelector,
  {}
)(withTranslation(DiscussionActivity));

const DiscussionActivityLoader = createLoaderComponent(
  selectContentActivityLoaderComponent,
  ({ content, viewingAs }) => loadDiscussionActivityActionCreator(content, viewingAs),
  'DiscussionActivity'
);

const DiscussionActivityContainer = props => (
  <DiscussionActivityLoader {...props}>
    <ConnectedDiscussionActivity {...props} />
  </DiscussionActivityLoader>
);

export default DiscussionActivityContainer;
