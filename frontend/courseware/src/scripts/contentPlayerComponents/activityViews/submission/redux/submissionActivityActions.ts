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

import { SubmissionAssessment, SubmissionAttempt } from '../../../../api/submissionApi.ts';
import { InstructorGraderPageLink } from '../../../../utils/pageLinks.ts';
import { compact } from 'lodash';
import {
  ContentWithNebulousDetails,
  ViewingAs,
} from '../../../../courseContentModule/selectors/contentEntry';
import { NGSubmissionActivityAPI } from '../../../../services/SubmissionActivityAPI';
import { createDataListUpdateMergeAction } from '../../../../utilities/apiDataActions';
import { loadingActionCreatorMaker } from '../../../../utilities/loadingStateUtils.ts';
import { gotoLinkActionCreator } from '../../../../utilities/routingUtils';
import { lojector } from '../../../../loject.ts';

const withContentAndUser =
  <T>(contentId: string, userId: number) =>
  (data: T) => ({
    contentId,
    userId,
    ...data,
  });

const submissionActivityACs = ({
  contentId,
  userId,
  assessment,
  attempts,
}: {
  contentId: string;
  userId: number;
  assessment: SubmissionAssessment;
  attempts: SubmissionAttempt[];
}) => {
  return compact([
    assessment &&
      createDataListUpdateMergeAction('submissionAssessmentByContent', {
        [contentId]: assessment,
      }),
    attempts &&
      createDataListUpdateMergeAction('submissionAttemptsByContentByUser', {
        [userId]: {
          [contentId]: attempts,
        },
      }),
  ]);
};

export const loadSubmissionActivityActionCreator = loadingActionCreatorMaker(
  { sliceName: 'contentActivityLoadingState' },
  (content: ContentWithNebulousDetails, viewingAs: ViewingAs) => {
    const SubmissionActivityAPI: NGSubmissionActivityAPI = lojector.get('SubmissionActivityAPI');
    const submission = SubmissionActivityAPI.loadSubmissionActivity(
      content.contentId,
      viewingAs.id
    );

    return submission.then(submission => {
      return {
        contentId: content.id,
        userId: viewingAs.id,
        ...submission,
      };
    }) as Promise<any>;
  },
  [submissionActivityACs],
  content => ({ id: content.id })
);

export const createSubmissionAttemptActionCreator = loadingActionCreatorMaker(
  { sliceName: 'submissionOpenAttemptLoadingStateByContent' },
  (content, viewingAsId) => {
    const SubmissionActivityAPI: NGSubmissionActivityAPI = lojector.get('SubmissionActivityAPI');
    return SubmissionActivityAPI.createAttempt(content.contentId).then(
      withContentAndUser(content.id, viewingAsId)
    ) as Promise<any>;
  },
  [submissionActivityACs],
  content => ({ id: content.id })
);

const afterSaveAC = ({ contentId, userId }: { contentId: string; userId: number }) => ({
  type: 'QUIZ_ACTIVITY_ATTEMPT_SAVED',
  id: contentId,
  contentId,
  userId,
});

export const saveSubmissionAttemptActionCreator = loadingActionCreatorMaker(
  { sliceName: 'submissionSaveState' },
  (content, attempt, attemptResponse) => {
    const SubmissionActivityAPI: NGSubmissionActivityAPI = lojector.get('SubmissionActivityAPI');
    return SubmissionActivityAPI.saveAttempt(attempt, attemptResponse).then(
      withContentAndUser(content.id, attempt.subjectId)
    ) as Promise<any>;
  },
  [submissionActivityACs, afterSaveAC],
  content => ({ id: content.id })
);

const afterSubmitAC = ({ contentId, userId }: { contentId: string; userId: number }) => ({
  type: 'QUIZ_ACTIVITY_ATTEMPT_SUBMITTED',
  id: contentId,
  contentId,
  userId,
});

export const submitSubmissionAttemptActionCreator = loadingActionCreatorMaker(
  { sliceName: 'submissionSubmitState' },
  (content, attempt, attemptResponse) => {
    const SubmissionActivityAPI: NGSubmissionActivityAPI = lojector.get('SubmissionActivityAPI');
    return SubmissionActivityAPI.saveAndSubmitAttempt(attempt, attemptResponse).then(
      withContentAndUser(content.id, attempt.subjectId)
    ) as Promise<any>;
  },
  [submissionActivityACs, afterSubmitAC],
  content => ({ id: content.id })
);

export const gradeAttemptActionCreator = (
  content: ContentWithNebulousDetails,
  attempt: SubmissionAttempt,
  viewingAs: ViewingAs
) =>
  gotoLinkActionCreator(
    InstructorGraderPageLink.toLink({
      contentId: content.id,
      forLearnerId: viewingAs.id,
      attemptId: attempt.id,
    })
  );
