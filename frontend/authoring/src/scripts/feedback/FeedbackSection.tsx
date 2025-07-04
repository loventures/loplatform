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
import { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

import { useAllEditedOutEdges, useEditedAsset } from '../graphEdit';
import { useBranchId, useDcmSelector, usePolyglot } from '../hooks';
import { ApiQueryResults } from '../srs/apiQuery';
import { useProjectAccess } from '../story/hooks';
import { isQuestion } from '../story/questionUtil';
import { setNarrativeAssetState } from '../story/storyActions';
import { FeedbackDto, loadFeedbacks } from './FeedbackApi';
import { FeedbackComponent } from './FeedbackComponent';
import { useFeedbackCount, useFeedbackFilters, useFeedbackSelector } from './feedbackHooks';

export const FeedbackSection: React.FC<{
  name: string;
  narrative?: boolean;
  expanded?: boolean;
}> = ({ name, narrative, expanded }) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const branchId = useBranchId();
  const { branch, status, assignee, refresh } = useFeedbackFilters();
  const [feedbacks, setFeedbacks] = useState<ApiQueryResults<FeedbackDto>>({
    count: -1,
    objects: [],
  });
  const asset = useEditedAsset(name);
  const edges = useAllEditedOutEdges(name);
  const rubric = edges.find(edge => edge.group === 'cblRubric')?.targetName;
  const count = useFeedbackCount(name, false) + useFeedbackCount(rubric, false);
  const total = useFeedbackCount(name, true);
  const { profile } = useDcmSelector(s => s.user);
  const projectAccess = useProjectAccess();
  const person = !projectAccess.ViewAllFeedback ? profile.id : undefined;
  const feedbackBranch = branch ?? branchId;
  const justAdded = useFeedbackSelector(state => state.justAdded);

  // this is horrid, need better state management around feedback loading. widgets should
  // express interest in feedback on IDs and everything else happens separately. In inline
  // mode, adding one feedback reloads the activities of all feedbacks.. And other things..

  const doReload = () =>
    count
      ? loadFeedbacks(feedbackBranch, {
          name: rubric ? [name, rubric] : name,
          status,
          assignee,
          person,
          remotes: branch ? branchId : undefined,
          order: 'asc',
        }).then(setFeedbacks)
      : (setFeedbacks({ count: 0, objects: [] }), Promise.resolve());

  useEffect(() => {
    setFeedbacks({ objects: [], count: 0 });
  }, [feedbackBranch, name]);

  useEffect(() => {
    doReload().then();
  }, [feedbackBranch, name, rubric, status, assignee, person, refresh, count]);

  const remainder = feedbacks.count < 0 ? 0 : total - feedbacks.count;

  return (
    <div className="feedback-section">
      {!narrative && feedbacks.count < 0 ? (
        <div className="p-4 text-muted text-center">Loading...</div>
      ) : !narrative && !feedbacks.count ? (
        <div className="p-4 text-muted text-center">
          {feedbacks.totalCount ? 'No feedback matching these filters' : 'No feedback'}
        </div>
      ) : narrative && (feedbacks.count > 0 || (remainder > 0 && !expanded)) ? (
        <div className="section-asset-name">
          <div className="dashes" />
          <div className="title text-truncate">
            {isQuestion(asset?.typeId) ? polyglot.t(asset.typeId) : asset?.data.title}
          </div>
          <div className="dashes" />
        </div>
      ) : null}
      {feedbacks.objects.map((feedback, index) => (
        <FeedbackComponent
          key={feedback.id}
          feedback={feedback}
          className={index ? undefined : 'first'}
          justAdded={justAdded === feedback.id}
        />
      ))}
      {narrative && remainder > 0 && !expanded && (
        <Button
          color="primary"
          block
          outline
          className="feedback-item p-3 text-center unhover-muted"
          onClick={() => dispatch(setNarrativeAssetState(name, { expanded: true }))}
        >
          {remainder} {remainder > 1 ? 'pieces' : 'piece'} of feedback within this content.
        </Button>
      )}
    </div>
  );
};
