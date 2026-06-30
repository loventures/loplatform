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

import { filter, some } from 'lodash';

import { LocalResourceStore } from '../srs/pure/localResourceStore.ts';
import { identiferToId } from '../utilities/contentIdentifier.js';
import Course from '../bootstrap/course.js';
import { discussionBoardAPI } from '../services/discussionBoardAPI.ts';

/**
 * Pure TS port of the AngularJS `ActiveDiscussionsStoreLight` factory: the
 * instructor dashboard "Active Discussions" store, driving the React SRS list
 * stack. No longer an Angular service — the React widget constructs it directly.
 *
 * `discussionBoardAPI` is the pure (axios) singleton; `loadDiscussionListRaw`
 * rides the native `request` (carrying the global X-UserId default). The pure
 * base is a constructor *function*, so the `extends` clause is cast to a
 * constructor type; runtime is unchanged.
 */
export class ActiveDiscussionsStoreLight extends (LocalResourceStore as unknown as {
  new (...args: any[]): any;
}) {
  requiredSummaryCounts: string[];
  hasDiscussions: boolean;

  constructor() {
    super();
    this.requiredSummaryCounts = ['unreadPostCount', 'unrespondedThreadCount'];
    this.sortByProps = {};
    this.searchByProps = {};
    this.setPageSize(5);
    this.hasDiscussions = (Course as any).hasDiscussions;
  }

  doRemoteLoad() {
    return discussionBoardAPI
      .loadDiscussionListRaw()
      .then((discussions: any[]) => {
        this.hasDiscussions = discussions.length > 0;
        return filter(discussions, (disc: any) => {
          return (
            disc.summary && some(this.requiredSummaryCounts, countKey => disc.summary[countKey])
          );
        });
      });
  }

  deserialize(discussion: any) {
    const summary = discussion.summary || {};
    const activeCount = summary.unreadPostCount;
    const unrespondedCount = summary.unrespondedThreadCount;
    return {
      id: identiferToId(discussion.id),
      name: discussion.title,
      activityType: 'discussion',
      activeCount,
      unrespondedCount,
    };
  }
}
