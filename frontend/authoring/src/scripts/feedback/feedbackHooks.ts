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

import { useDcmSelector, useNumericRouterPathVariable } from '../hooks';
import { useProjectAccess } from '../story/hooks';
import { NodeName } from '../types/asset';
import { FeedbackDto } from './FeedbackApi';
import { FeedbackState } from './feedbackReducer';

export const useFeedbackSelector = <A>(selector: (state: FeedbackState) => A) =>
  useDcmSelector(state => selector(state.feedback));

export const useCurrentFeedbackId = (): number | undefined =>
  useNumericRouterPathVariable('feedback');

export const useCurrentFeedback = (): FeedbackDto | undefined => {
  const feedbackId = useCurrentFeedbackId();
  return useFeedbackSelector(state => state.feedbacks[feedbackId]);
};

export const useFeedbackOpen = () => useFeedbackSelector(state => state.open);

export const useFeedbackOn = () => useFeedbackSelector(state => state.on);

export const useFeedbackMode = () => useFeedbackSelector(state => state.mode);

export const useAddFeedback = () => useFeedbackSelector(state => state.addFeedback);

export const useAddingFeedback = () => useFeedbackSelector(state => !!state.addFeedback);

export const useHtmlFeedback = () => useFeedbackSelector(state => state.htmlFeedback);

export const useFeedbackOffset = () => useFeedbackSelector(state => state.offset);

export const useFeedbackFilters = () => useFeedbackSelector(state => state.filters);

// narrative authors are restricted to their own feedback
export const useFeedbackPersonFilter = () => {
  const projectAccess = useProjectAccess();
  return useDcmSelector(state =>
    !projectAccess.ViewAllFeedback ? state.user.profile.id : undefined
  );
};

export const useFeedbackAssignees = () => useFeedbackSelector(state => state.assignees);

export const useFeedbackCounts = () => useFeedbackSelector(state => state.counts);

export const useFeedbackCount = (name: NodeName | undefined, total?: boolean): number =>
  useFeedbackSelector(state => (total ? state.totals : state.counts)[name] ?? 0);
