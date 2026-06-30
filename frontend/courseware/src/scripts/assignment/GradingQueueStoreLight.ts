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

import { map } from 'lodash';

import { LocalResourceStore } from '../srs/pure/localResourceStore.ts';
import { getContentDisplayInfo } from '../utilities/contentDisplayInfo.js';
import { selectPageContentLoaderComponent } from '../loaders/PageContentLoader.js';
import { selectContentItems } from '../selectors/contentItemSelectors.js';
import { selectCurrentUser } from '../utilities/rootSelectors.js';
import { loadContentPlayerActionCreator } from '../courseContentModule/actions/contentPageLoadActions.js';
import { getAssignmentType } from '../assignmentGrader/getAssignmentType.js';
import { getGradingQueue } from '../api/gradingApi.js';
import { courseReduxStore } from '../loRedux';

/**
 * Pure TS port of the AngularJS `GradingQueueStoreLight` factory: the instructor
 * dashboard "Assignments to Grade" store, driving the React SRS list stack. No
 * longer an Angular service — the React widget constructs it directly.
 *
 * `$q.defer()` → native `Promise` (the content-loaded resolve + `courseReduxStore`
 * subscribe logic is preserved exactly); `$q.all({...})` → `Promise.all([...])`.
 * The pure base is a constructor *function*, so the `extends` clause is cast to a
 * constructor type; runtime is unchanged from the original `.js` subclass.
 */
export class GradingQueueStoreLight extends (LocalResourceStore as unknown as {
  new (...args: any[]): any;
}) {
  contentPromise: Promise<any>;

  constructor() {
    super();

    this.sortByProps = {};
    this.searchByProps = {};

    this.setPageSize(5);

    this.contentPromise = this.getContentPromise();
  }

  getContentPlayerLoadingState() {
    return selectPageContentLoaderComponent(courseReduxStore.getState()).loadingState;
  }

  getContentItems() {
    return selectContentItems(courseReduxStore.getState());
  }

  getContentPromise() {
    return new Promise<any>(resolve => {
      const loadingState = this.getContentPlayerLoadingState();
      if (loadingState.loaded) {
        resolve(this.getContentItems());
      } else {
        if (!loadingState.loading) {
          const currentUser = selectCurrentUser(courseReduxStore.getState());
          courseReduxStore.dispatch(loadContentPlayerActionCreator(currentUser.id));
        }
        const subs = courseReduxStore.subscribe(() => {
          const loadingState = this.getContentPlayerLoadingState();
          if (loadingState.loaded) {
            subs();
            resolve(this.getContentItems());
          }
        });
      }
    });
  }

  doRemoteLoad() {
    return Promise.all([getGradingQueue(), this.contentPromise]).then(
      ([actionables, contents]) => {
        return map(actionables, (queueItem: any) => {
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
      }
    );
  }
}
