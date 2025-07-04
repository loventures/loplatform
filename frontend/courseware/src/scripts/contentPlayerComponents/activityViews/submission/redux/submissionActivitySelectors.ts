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

import { AttachmentInfo, isBasicFeedback } from '../../../../api/quizApi.ts';
import { SubmissionAssessment, SubmissionAttempt } from '../../../../api/submissionApi.ts';
import { CourseState } from '../../../../loRedux';
import { filter, find, isEmpty, last, map, orderBy, some } from 'lodash';
import { SubmissionInEditState } from './submissionInEditReducer.ts';
import { DisplaySubmissionAttempt, SubmissionActivity } from '../submissionActivity';
import { ContentWithRelationships } from '../../../../courseContentModule/selectors/assembleContentView.ts';
import { ViewingAs } from '../../../../courseContentModule/selectors/contentEntry';
import {
  selectPageContent,
  selectPageContentId,
} from '../../../../courseContentModule/selectors/contentEntrySelectors.ts';
import { DRIVER_LEARNER } from '../../../../utilities/assessmentSettings';
import {
  ATTEMPT_FINALIZED,
  ATTEMPT_OPEN,
  ATTEMPT_SUBMITTED,
} from '../../../../utilities/attemptStates';
import { LoadingState } from '../../../../utilities/loadingStateUtils.ts';
import { createInstanceSelector } from '../../../../utilities/reduxify.ts';
import { selectCurrentUser, selectCurrentUserId } from '../../../../utilities/rootSelectors.ts';
import { Selector } from 'react-redux';
import { createSelector, createStructuredSelector } from 'reselect';

const selectSubmissionAssessmentByContent = (state: CourseState) =>
  state.api.submissionAssessmentByContent;
const selectSubmissionAttemptsByContentByUser = (state: CourseState) =>
  state.api.submissionAttemptsByContentByUser;
export const selectContentPageSubmissionAssessment = createInstanceSelector<SubmissionAssessment>(
  selectSubmissionAssessmentByContent,
  selectPageContentId
);
const selectCurrentUserSubmissionAttemptsByContent = createInstanceSelector(
  selectSubmissionAttemptsByContentByUser,
  selectCurrentUserId
);
export const selectCurrentUserCurrentPageSubmissionAttempts = createInstanceSelector<
  SubmissionAttempt[]
>(selectCurrentUserSubmissionAttemptsByContent, selectPageContentId);
export const selectCurrentPageSubmissionOpenAttemptLoadingState = createInstanceSelector(
  (state: CourseState) => state.ui.submissionOpenAttemptLoadingStateByContent,
  selectPageContentId,
  {}
);
export const selectSubmissionActivity = createSelector(
  selectContentPageSubmissionAssessment,
  selectCurrentUserCurrentPageSubmissionAttempts,
  (assessment, attempts) => {
    const settings = assessment.settings || {};
    const learnerCanDrive = settings.driver === DRIVER_LEARNER;

    let validAttempts = filter(attempts, att => att.valid);
    if (!learnerCanDrive) {
      validAttempts = filter(validAttempts, attempt => attempt.state !== ATTEMPT_OPEN);
    }

    const displayAttempts: DisplaySubmissionAttempt[] = map(validAttempts, attempt => {
      return {
        ...attempt,
        isOpen: attempt.state === ATTEMPT_OPEN,
        attachments: mapAttachments(attempt.attachments, attempt.attachmentInfos),
        feedback: map(attempt.feedback, feedback => {
          if (isBasicFeedback(feedback)) {
            return {
              ...feedback,
              attachments: mapAttachments(feedback.attachments, attempt.attachmentInfos),
            };
          } else {
            return { ...feedback };
          }
        }),
      } as DisplaySubmissionAttempt;
    });

    const allOrderedAttempts = orderBy(displayAttempts, 'submitTime');

    const latestAttempt = last(allOrderedAttempts);

    const isLatestAttemptOpen = latestAttempt && latestAttempt.isOpen;

    const orderedAttempts = isLatestAttemptOpen
      ? allOrderedAttempts.slice(0, -1)
      : allOrderedAttempts;

    const latestSubmittedAttempt = last(orderedAttempts);

    const hasSubmittedAttempts = some(
      displayAttempts,
      a => a.state === ATTEMPT_FINALIZED || a.state === ATTEMPT_SUBMITTED
    );

    const unlimitedAttempts = settings.maxAttempts === null;
    const numSubmittedAttempts = filter(validAttempts, a => a.state !== ATTEMPT_OPEN).length;
    // TODO: subtracting from zero seems wrong if maxAttempts is null.
    const attemptsRemaining = (settings.maxAttempts || 0) - numSubmittedAttempts;
    const openAttempt = isLatestAttemptOpen ? latestAttempt : null;

    const canPlayAttempt =
      !assessment.pastDeadline && (!!openAttempt || unlimitedAttempts || attemptsRemaining > 0);

    return {
      assessment,
      orderedAttempts,
      orderedAttemptsLength: orderedAttempts.length,
      latestAttempt,
      isLatestAttemptOpen,
      openAttempt,
      hasSubmittedAttempts,
      attemptsRemaining,
      attemptNumber: 1 + numSubmittedAttempts,
      unlimitedAttempts,
      canPlayAttempt,
      learnerCanDrive,
      latestSubmittedAttempt,
    };
  }
);

const firstNonEmpty = <T>(...args: T[][]) => find(args, a => !isEmpty(a)) || [];

const mapAttachments = (ids: number[], infos: Record<string, AttachmentInfo>) =>
  map(ids, id => infos[id]);

export const selectSubmissionActivityComponent = createStructuredSelector<
  CourseState,
  {
    content: ContentWithRelationships;
    submissionActivity: SubmissionActivity;
    viewingAs: ViewingAs;
  }
>({
  // NOTE: we have to assume if we're here that pageContent isn't the course. bad typings.
  content: selectPageContent as Selector<CourseState, ContentWithRelationships>,
  submissionActivity: selectSubmissionActivity,
  viewingAs: selectCurrentUser,
});

const selectSubmissionSubmitState: Selector<CourseState, Record<string, LoadingState>> = (
  state: CourseState
) => state.ui.submissionSubmitState;
const selectSubmissionSaveState: Selector<CourseState, Record<string, LoadingState>> = (
  state: CourseState
) => state.ui.submissionSaveState;
const selectSubmissionInEditByContentByUser = (state: CourseState) =>
  state.ui.submissionInEditByContentByUser;

export const selectCurrentUserSubmissionInEditByContent = createInstanceSelector(
  selectSubmissionInEditByContentByUser,
  selectCurrentUserId,
  {}
);

export const selectCurrentUserContentPageSubmissionInEdit =
  createInstanceSelector<SubmissionInEditState>(
    selectCurrentUserSubmissionInEditByContent,
    selectPageContentId,
    {}
  );

export const selectSubmissionActivityEditComponent = createSelector(
  selectPageContent,
  selectSubmissionActivity,
  selectSubmissionSubmitState,
  selectSubmissionSaveState,
  selectCurrentUserContentPageSubmissionInEdit,
  (content, submissionActivity, submissionSubmitState, submissionSaveState, savedInEdit) => {
    const activeAttempt = submissionActivity.openAttempt || ({} as DisplaySubmissionAttempt);

    const inEdit: SubmissionInEditState = {
      ...savedInEdit,
      essay: savedInEdit.essay ?? activeAttempt.essay ?? '',
      attachments: savedInEdit.emptyAttachments
        ? []
        : firstNonEmpty(savedInEdit.attachments, activeAttempt.attachments),
      uploads: savedInEdit.uploads || [],
    };

    const submitState = submissionSubmitState[content.id] || {};
    const saveState = submissionSaveState[content.id] || {};

    return {
      attempt: activeAttempt,
      inEdit,
      submitState,
      saveState,
    };
  }
);

export const selectSubmissionOpenAttemptLoaderComponent = createSelector(
  [selectCurrentPageSubmissionOpenAttemptLoadingState, selectSubmissionActivity],
  (loadingState, submissionActivity) => {
    return {
      loadingState: {
        ...loadingState,
        loaded: !!submissionActivity.openAttempt && !loadingState.error,
      },
    };
  }
);
