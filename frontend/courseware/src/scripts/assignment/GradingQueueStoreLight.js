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

import { map } from 'lodash';
import LocalResourceStore from '../srs/LocalResourceStore.js';

import { getContentDisplayInfo } from '../utilities/contentDisplayInfo.js';

import { selectPageContentLoaderComponent } from '../loaders/PageContentLoader.js';

import { selectContentItems } from '../selectors/contentItemSelectors.js';

import { selectCurrentUser } from '../utilities/rootSelectors.js';

import { loadContentPlayerActionCreator } from '../courseContentModule/actions/contentPageLoadActions.js';

import { getAssignmentType } from '../assignmentGrader/getAssignmentType.js';
import { getGradingQueue } from '../api/gradingApi.js';

export default angular
  .module('lo.content.GradingQueueStoreLight', [LocalResourceStore.name])
  .factory('GradingQueueStoreLight', [
    'LocalResourceStore',
    '$ngRedux',
    '$q',
    function (LocalResourceStore, $ngRedux, $q) {
      class GradingQueueStoreLight extends LocalResourceStore {
        constructor() {
          super();

          this.sortByProps = {};
          this.searchByProps = {};

          this.setPageSize(5);

          this.contentPromise = this.getContentPromise();
        }

        getContentPlayerLoadingState() {
          return selectPageContentLoaderComponent($ngRedux.getState()).loadingState;
        }

        getContentItems() {
          return selectContentItems($ngRedux.getState());
        }

        getContentPromise() {
          const d = $q.defer();
          const loadingState = this.getContentPlayerLoadingState();
          if (loadingState.loaded) {
            d.resolve(this.getContentItems());
          } else {
            if (!loadingState.loading) {
              const currentUser = selectCurrentUser($ngRedux.getState());
              $ngRedux.dispatch(loadContentPlayerActionCreator(currentUser.id));
            }
            const subs = $ngRedux.subscribe(() => {
              const loadingState = this.getContentPlayerLoadingState();
              if (loadingState.loaded) {
                subs();
                d.resolve(this.getContentItems());
              }
            });
          }
          return d.promise;
        }

        doRemoteLoad() {
          return $q
            .all({
              actionables: getGradingQueue(),
              contents: this.contentPromise,
            })
            .then(({ actionables, contents }) => {
              return map(actionables, queueItem => {
                const content = contents[queueItem.edgePath];
                const assignmentType = getAssignmentType(content);
                return {
                  id: content.id,
                  assignmentId: content.contentId,
                  title: content.name,
                  activeCount: queueItem.overview.actionItemCount,
                  assignmentType,
                  activityType: assignmentType,
                  ...getContentDisplayInfo(content),
                };
              });
            });
        }
      }

      return GradingQueueStoreLight;
    },
  ]);
