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

import { UserInfo } from '../../../../loPlatform';
import Course from '../../../bootstrap/course';
import { activeValueSetActionCreatorMaker } from '../../../utilities/activeValueReduxUtils';
import {
  loadingActionCreatorMaker,
  loadingResetActionCreatorMaker,
} from '../../../utilities/loadingStateUtils';
import { statusFlagToggleActionCreatorMaker } from '../../../utilities/statusFlagReducer';
import { AnyAction, Dispatch } from 'redux';
import { BatchAction, batchActions } from 'redux-batched-actions';
import { lojector } from '../../../loject';

const modalActionConfig = {
  sliceName: 'learnerTableModal',
};

type ModalAction = AnyAction;

const modalStatusActionCreator = statusFlagToggleActionCreatorMaker(modalActionConfig);

const activeLearnerActionCreator = activeValueSetActionCreatorMaker(modalActionConfig);

export const openModalActionCreator =
  (learner: UserInfo) =>
  (dispatch: Dispatch<any>): BatchAction =>
    dispatch(batchActions([modalStatusActionCreator(true), activeLearnerActionCreator(learner)]));

export const toggleModalActionCreator = (): ModalAction => modalStatusActionCreator(undefined);

export const resetMessageStateActionCreator = loadingResetActionCreatorMaker(modalActionConfig);

const sendMessage = (learner: UserInfo, content: string): Promise<void> => {
  const SimpleMessage = lojector.get('SimpleMessage') as any;
  const message = new SimpleMessage();
  message.title = (lojector.get('$translate') as any).instant('STUDENT_AT_RISK_MESSAGE_TITLE', {
    course: Course.title,
  });
  message.recipients = [learner];
  message.content = content;
  return message.send();
};

export const sendMessageActionCreator: (
  learner: UserInfo,
  content: string
) => (dispatch: Dispatch<any>) => void = loadingActionCreatorMaker(modalActionConfig, sendMessage, [
  resetMessageStateActionCreator,
]);
