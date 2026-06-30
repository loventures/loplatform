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

import { createDataListUpdateMergeAction } from '../../../utilities/apiDataActions';

import {
  loadingActionCreatorMaker,
  loadingResetActionCreatorMaker,
} from '../../../utilities/loadingStateUtils';
import { gradebookAPI } from '../../../services/gradebookAPI.ts';
import { quizOverviewAPI } from '../../../services/quizOverviewAPI.ts';

export const toggleSendMessageModalActionCreator = (activeOverview?: any) => ({
  type: 'OVERVIEW_MODAL_TOGGLE',
  data: { activeOverview },
});

export const openSendMessageModalActionCreator = (activeOverview: any) => ({
  type: 'OVERVIEW_MODAL_OPEN',
  data: { activeOverview },
});

const closeSendMessageModalActionCreator = () => ({
  type: 'OVERVIEW_MODAL_CLOSE',
});

const resetSendMessageLoadingStateActionCreator = loadingResetActionCreatorMaker({
  sliceName: 'activityOverviewSendMessageLoadingState',
});

const overviewSendMessageSuccessAC = ({ contentId, userId, grade }: any) => {
  const acs: any[] = [];

  if (grade) {
    acs.push(
      createDataListUpdateMergeAction('activityOverviewByUserByContent', {
        [contentId]: {
          [userId]: {
            grade,
          },
        },
      })
    );
  }

  return acs;
};

const sendGrade = (contentId, overviewItem, grade) => {
  return new Promise((resolve, reject) => {
    gradebookAPI
      .setScore(overviewItem.learner.id, contentId, grade / 100)
      .then(gradebookGrade => {
        const {
          // : LegacyGradebookWebController.GradeComponentDto ffs
          grade, // : Option[Double]
          max, // : Double
        } = gradebookGrade;
        resolve({
          pointsAwarded: grade,
          pointsPossible: max,
        });
      }, reject);
  });
};

const sendMessage = (contentId, overviewItem, message) => {
  return new Promise((resolve, reject) => {
    quizOverviewAPI.sendMessage(contentId, overviewItem.learner.id, message).then(resolve, reject);
  });
};

const sendMessageAndGrade = (contentId, overviewItem, grade, message) => {
  return Promise.all([
    grade && sendGrade(contentId, overviewItem, grade),
    message && sendMessage(contentId, overviewItem, message),
  ]).then(([grade, message]) => {
    return {
      grade,
      message,
      contentId,
      userId: overviewItem.learner.id,
    };
  });
};

export const sendMessageActionCreator = loadingActionCreatorMaker(
  { sliceName: 'activityOverviewSendMessageLoadingState' },
  sendMessageAndGrade,
  [
    overviewSendMessageSuccessAC,
    closeSendMessageModalActionCreator,
    resetSendMessageLoadingStateActionCreator,
  ]
);
