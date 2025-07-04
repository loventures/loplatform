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

import { filter, some } from 'lodash';

import LocalResourceStore from '../srs/LocalResourceStore.js';
import DiscussionBoardAPI from '../services/DiscussionBoardAPI.js';
import { identiferToId } from '../utilities/contentIdentifier.js';
import Course from '../bootstrap/course.js';

export default angular
  .module('lo.assignment.ActiveDiscussionsStoreLight', [
    LocalResourceStore.name,
    DiscussionBoardAPI.name,
  ])
  .factory('ActiveDiscussionsStoreLight', [
    'LocalResourceStore',
    'DiscussionBoardAPI',
    function (LocalResourceStore, DiscussionBoardAPI) {
      class ActiveDiscussionsStoreLight extends LocalResourceStore {
        constructor() {
          super();
          this.requiredSummaryCounts = ['unreadPostCount', 'unrespondedThreadCount'];
          this.sortByProps = {};
          this.searchByProps = {};
          this.setPageSize(5);
          this.hasDiscussions = Course.hasDiscussions;
        }

        doRemoteLoad() {
          return DiscussionBoardAPI.loadDiscussionListRaw().then(discussions => {
            this.hasDiscussions = discussions.length > 0;
            return filter(discussions, disc => {
              return (
                disc.summary && some(this.requiredSummaryCounts, countKey => disc.summary[countKey])
              );
            });
          });
        }

        deserialize(discussion) {
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

      return ActiveDiscussionsStoreLight;
    },
  ]);
