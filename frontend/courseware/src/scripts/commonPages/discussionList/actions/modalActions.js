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

import { createDataListUpdateMergeAction } from '../../../utilities/apiDataActions';
import { loadingActionCreatorMaker } from '../../../utilities/loadingStateUtils';
import { statusFlagToggleActionCreatorMaker } from '../../../utilities/statusFlagReducer';
import { lojector } from '../../../loject';

const actionConfig = {
  sliceName: 'manageDiscussionsModal',
};

const modalStatusActionCreator = statusFlagToggleActionCreatorMaker(actionConfig);

export const openModalActionCreator = () => modalStatusActionCreator(true);

export const closeModalActionCreator = () => modalStatusActionCreator(false);

export const toggleModalActionCreator = () => modalStatusActionCreator();

const saveUpdates = (discussions, { closed }) => {
  return lojector.get('DiscussionBoardAPI').batchClosePolicy(closed);
};

const saveUpdatesSuccessAC = () => createDataListUpdateMergeAction('contents', {});

export const saveUpdatesActionCreator = loadingActionCreatorMaker(actionConfig, saveUpdates, [
  saveUpdatesSuccessAC,
  closeModalActionCreator,
]);
