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

import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';

import { useGraphEditSelector } from '../graphEdit';
import { useBranchId, useDcmSelector } from '../hooks';
import { useProjectGraphSelector } from '../structurePanel/projectGraphHooks';
import { NodeName } from '../types/asset';
import {
  refreshFeedback,
  setFeedbackAssignees,
  setFeedbackCounts,
  setFeedbackStale,
  setFeedbackSuccess,
  setFeedbackTotals,
} from './feedbackActions';
import { loadAssignees, loadFeedbackSummary } from './FeedbackApi';
import {
  useFeedbackCounts,
  useFeedbackFilters,
  useFeedbackPersonFilter,
  useFeedbackSelector,
} from './feedbackHooks';
import { AssetCounts } from './feedbackReducer';

const recordFeedbackCount = (counts: AssetCounts, name: NodeName, count: number) =>
  (counts[name] = (counts[name] ?? 0) + count);

/**
 * We want this at the root of DCM so it is available for all the routes. But if we put it in a
 * hook useFeedbackLoader() that DcmApp uses then any internal changes cause the entirety of
 * DcmAOpp to re-render. So we make it a child of DcmApp so only this component rerenders.
 */
const FeedbackLoader: React.FC = () => {
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const filters = useFeedbackFilters();
  const person = useFeedbackPersonFilter();
  const rootName = useProjectGraphSelector(state => state.rootNodeName);
  const isStale = useDcmSelector(
    state =>
      state.feedback.stale &&
      state.presence.tabVisible &&
      !state.presence.idling &&
      state.configuration.realTime
  );
  const wasSuccess = useFeedbackSelector(state => state.success);
  const shouldRefresh = isStale && wasSuccess == null;

  useEffect(() => {
    if (branchId < 0) return;
    loadAssignees(branchId).then(results => dispatch(setFeedbackAssignees(results.objects)));
  }, [branchId]);

  useEffect(() => {
    if (!rootName || branchId < 0) return; // need the project graph for the access control for the person filter
    const { branch, assignee, status, unit, module } = filters;
    loadFeedbackSummary(branch ?? branchId, {
      assignee,
      person,
      status,
      unit,
      module,
      remotes: branch ? branchId : undefined,
    })
      .then(summaries => {
        const counts: AssetCounts = {};
        for (const { assetName, count } of summaries) {
          recordFeedbackCount(counts, assetName, count);
        }
        dispatch(setFeedbackCounts(counts, filters));
        dispatch(setFeedbackStale(false));
        dispatch(setFeedbackSuccess(true));
      })
      .catch(e => {
        console.log(e);
        dispatch(setFeedbackSuccess(false));
      });
  }, [rootName, branchId, person, filters]);

  const contextPaths = useGraphEditSelector(state => state.contentTree.contextPaths);
  const counts = useFeedbackCounts();

  // We have to recompute all the totals because if you move a node from place to place, the feedback
  // context path will we wrong
  useEffect(() => {
    const totals: AssetCounts = { ...counts };
    for (const [name, count] of Object.entries(counts)) {
      const contextPath = contextPaths[name];
      if (contextPath) {
        for (const ancestor of contextPath.split('.')) {
          totals[ancestor] = (totals[ancestor] ?? 0) + count;
        }
      }
    }
    dispatch(setFeedbackTotals(totals));
  }, [counts, contextPaths]);

  useEffect(() => {
    if (wasSuccess != null) {
      const timeout = setTimeout(
        () => dispatch(setFeedbackSuccess(undefined)),
        wasSuccess ? 5000 : 15000
      );
      return () => clearTimeout(timeout);
    }
  }, [wasSuccess]);

  useEffect(() => {
    if (shouldRefresh) dispatch(refreshFeedback());
  }, [shouldRefresh]);

  return null;
};

export default FeedbackLoader;
