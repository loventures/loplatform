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

import { createSelector } from 'reselect';
import { createLoaderComponent } from '../utilities/withLoader.js';

import { selectRouter, selectCurrentUser } from '../utilities/rootSelectors.js';
import { createDataListUpdateMergeAction } from '../utilities/apiDataActions.js';
import { loadingActionCreatorMaker } from '../utilities/loadingStateUtils.js';
import { lojector } from '../loject.js';

const sliceName = 'paramLearnerLoadingState';

const loadParamUserActionCreator = loadingActionCreatorMaker(
  { sliceName },
  userId => lojector.get('enrolledUserService').getUser(userId),
  [user => createDataListUpdateMergeAction('users', { [user.id]: user })]
);

const selector = createSelector(
  selectRouter,
  selectCurrentUser,
  state => state.api.users,
  state => state.ui[sliceName],
  (router, currentUser, users, loadingState) => {
    const userId = router.searchParams.forLearnerId;

    if (!userId || currentUser.id === userId) {
      return {
        loaderKey: userId,
        loadingState: { loaded: true },
      };
    }
    return {
      userId,
      loaderKey: userId,
      loadingState: {
        ...loadingState,
        loaded: !!users[userId],
      },
    };
  }
);

const ForLearnerLoader = createLoaderComponent(
  selector,
  ({ userId }) => loadParamUserActionCreator(userId),
  'ForLearner'
);

export default ForLearnerLoader;
