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
import React, { useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Alert } from 'reactstrap';

import Loading from '../authoringUi/Loading';
import { getAllEditedOutEdges, getEditedAsset, useEditedAsset, useGraphEdits } from '../graphEdit';
import useHomeNodeName from '../hooks/useHomeNodeName';
import { NarrativeAsset } from '../story/NarrativeAsset';
import { useIsStoryEditMode } from '../story/storyHooks';
import useEditableIFrame from '../story/useEditableIFrame';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { openToast } from '../toast/actions';
import { isContentType } from './AddFeedback';
import { feedbackLoaded, toggleFeedbackOpen } from './feedbackActions';
import { FeedbackDto, FeedbackRelocate, loadFeedback, relocateFeedback } from './FeedbackApi';
import { useFeedbackFilters } from './feedbackHooks';

const FeedbackDetail: React.FC<{ id: number }> = ({ id }) => {
  const dispatch = useDispatch();
  const editMode = useIsStoryEditMode();

  const [fetching, setFetching] = useState<boolean>(true);
  const [feedback, setFeedback] = useState<FeedbackDto | null>();
  const { refresh } = useFeedbackFilters();

  const homeNodeName = useHomeNodeName();
  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  const asset = useEditedAsset(feedback?.assetName);
  const context = [
    homeNodeName,
    feedback?.unitName,
    feedback?.moduleName,
    feedback?.lessonName,
    feedback?.contentName,
  ].filter(name => name && name !== asset?.name);
  const contextPath = context.join('.');

  // Feedback memorializes the module/lesson/content location of the asset
  // upon which feedback was rendered. If the asset has since moved, that
  // memorial will be in error so relocate it.
  const relocate = useMemo<FeedbackRelocate | undefined>(() => {
    if (asset) {
      let bad = false;
      for (let i = 0; i < context.length; ++i) {
        // if any element in the path is not a child of its parent
        bad ||= !getAllEditedOutEdges(context[i], projectGraph, graphEdits).some(
          edge => edge.targetName === (context[i + 1] ?? asset.name)
        );
      }
      const realPath = graphEdits.contentTree.contextPaths[asset.name];
      if (bad && realPath) {
        const pathEls = realPath
          .split('.')
          .map(el => getEditedAsset(el, projectGraph, graphEdits))
          .concat(asset);
        const unitName = pathEls.find(el => el?.typeId === 'unit.1')?.name;
        const moduleName = pathEls.find(el => el?.typeId === 'module.1')?.name;
        const lessonName = pathEls.find(el => el?.typeId === 'lesson.1')?.name;
        const contentName = pathEls.find(a => isContentType(a?.typeId))?.name;
        return { unitName, moduleName, lessonName, contentName };
      }
    }
    return undefined;
  }, [feedback, asset, context, projectGraph, graphEdits]);

  useEffect(() => {
    if (relocate) {
      relocateFeedback(id, relocate)
        .then(feedback => {
          setFeedback(feedback);
          dispatch(feedbackLoaded([feedback]));
          dispatch(openToast('Feedback was relocated.', 'success'));
        })
        .catch(e => {
          console.log(e);
        });
    }
  }, [relocate]);

  useEffect(() => {
    dispatch(toggleFeedbackOpen(true)); // vile but the css needs the class
  }, []);

  useEditableIFrame();

  // this may already have been loaded by the index, but shrug...
  useEffect(() => {
    loadFeedback(id)
      .then(feedback => {
        setFeedback(feedback);
        dispatch(feedbackLoaded([feedback]));
      })
      .catch(e => {
        console.log(e);
        setFeedback(e.status === 404 ? null : undefined);
      })
      .finally(() => setFetching(false));
  }, [id, refresh]);

  return (
    <div className={classNames('feedback-detail', 'feedback-open', editMode && 'flash-edit')}>
      {fetching ? null : feedback ? (
        <div className="d-flex py-0 py-lg-4">
          <div className="flex-grow-1 narrative-editor">
            {!asset || relocate ? (
              <Loading />
            ) : (
              <NarrativeAsset
                key={asset.name}
                name={asset.name}
                contextPath={contextPath}
                mode="feedback"
              />
            )}
          </div>
        </div>
      ) : (
        <Alert
          color="danger"
          className="m-3"
        >
          {feedback === null ? 'Feedback deleted.' : 'An error occurred.'}
        </Alert>
      )}
    </div>
  );
};

export default FeedbackDetail;
