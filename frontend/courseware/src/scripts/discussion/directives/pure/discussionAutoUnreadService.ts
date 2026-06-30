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

import { keys, filter, flow, mapValues, map, groupBy } from 'lodash';

import { CallAggregatorsSet } from '../../../utilities/pure/callAggregator.ts';
import settingsSvc from '../../../utilities/settingsService.ts';
import { courseReduxStore } from '../../../loRedux';
import * as DiscussionPostActions from '../../actions/DiscussionPostActions.ts';

/**
 * Pure (framework-free) `discussionAutoUnreadService` singleton — the redux batching/aggregation layer
 * the React auto-unread sentinel (DiscussionAutoUnread in discussionItem.tsx) calls. Ported from the
 * former Angular `DiscussionAutoUnreadService`; its `CallAggregatorsSet` / `Settings` injections are now
 * the pure `CallAggregatorsSet` (native Promise/setTimeout runtime) and the `settingsService` singleton.
 */
const updateTracking = (discussionId: any, postIds: any, threadIdMap: any) => {
  // Computed per-call, not at module load: `TeachCourseRight` is a per-course right that isn't loaded
  // until you're inside a course, but this module evaluates at app bootstrap. (The old Angular `.service`
  // computed it lazily at first injection — i.e. when discussionItem first rendered inside a course.)
  const isInstructor = settingsSvc.isFeatureEnabled('TeachCourseRight');
  if (isInstructor) {
    courseReduxStore.dispatch(
      DiscussionPostActions.createAutoSetViewedAction(discussionId, postIds, threadIdMap)
    );
  } else {
    courseReduxStore.dispatch(
      DiscussionPostActions.createAutoSetNotNewAction(discussionId, postIds, threadIdMap)
    );
  }
};

const aggregators = new CallAggregatorsSet((discussionId: any) => {
  return (idMap: any) => {
    const threadIdMap = flow(
      (values: any) => filter(values, (tid: any, pid: any) => tid !== +pid),
      (values: any) => groupBy(values, (threadId: any) => threadId),
      (values: any) => mapValues(values, 'length')
    )(idMap);
    updateTracking(
      discussionId,
      map(keys(idMap), a => +a),
      threadIdMap
    );
  };
}, 1000);

export const discussionAutoUnreadService = {
  updateViewed(discussionId: any, threadId: any, postId: any) {
    const aggregator = aggregators.getOrCreate(discussionId);
    return aggregator.queueCalls({
      [postId]: threadId,
    });
  },
};
