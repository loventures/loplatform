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

import { StagedFile } from '../../../../api/fileUploadApi.ts';
import { AttachmentInfo } from '../../../../api/quizApi.ts';
import dayjs from 'dayjs';
import { isEqual } from 'lodash';
import {
  FILE_STAGING_ERROR,
  FILE_STAGING_PROGRESS,
  FILE_STAGING_SELECTED,
  FILE_STAGING_SUCCESS,
  FileStagingState,
  fileStagingInitialState,
  fileStagingReducer,
} from '../../../../utilities/fileStagingUtils.ts';
import { AnyAction, Reducer } from 'redux';

const withoutFile = <T>(list: T[], file: T) => list.filter(item => !isEqual(item, file));

export interface SubmissionInEditState {
  lastUpdated?: number;
  essay?: string;
  attachments: AttachmentInfo[];
  emptyAttachments: boolean;
  uploads: StagedFile[];
  staging: FileStagingState;
}

/** optimistic idea of what fields can be in this action. */
export interface SubmissionInEditAction extends AnyAction {
  data: {
    essay: string;
    attachmentId: number;
    existingAttachments: AttachmentInfo[];
    upload: StagedFile;
  };
}

const blankState: SubmissionInEditState = {
  lastUpdated: undefined,
  essay: undefined,
  attachments: [],
  emptyAttachments: false,
  uploads: [],
  staging: fileStagingInitialState,
};

/**
 * The InEdit Reducer keeps temporary objects that have not yet been saved to an attempt.
 * Autosave, save, or submission clear this out.
 * */
const submissionInEditReducer: Reducer<SubmissionInEditState, SubmissionInEditAction> = (
  state = blankState,
  action
) => {
  switch (action.type) {
    case 'QUIZ_ACTIVITY_ATTEMPT_SUBMITTED':
      return blankState;
    case 'QUIZ_ACTIVITY_ATTEMPT_SAVED':
      // in the event an attempt was saved during staging, we don't want to clear it.
      if (state.staging.unstagedFiles.length) {
        return {
          ...blankState,
          staging: state.staging,
        };
      } else {
        return blankState;
      }
    case 'SUBMISSION_ATTEMPT_RESPONSE_TEXT_CHANGED':
      return {
        ...state,
        lastUpdated: dayjs().valueOf(),
        essay: action.data.essay,
      };
    case 'SUBMISSION_ATTEMPT_ATTACHMENT_REMOVED': {
      const attachments = action.data.existingAttachments.filter(
        att => att.id !== action.data.attachmentId
      );
      return {
        ...state,
        lastUpdated: dayjs().valueOf(),
        attachments,
        emptyAttachments: attachments.length === 0,
      };
    }
    case 'SUBMISSION_ATTEMPT_UPLOAD_REMOVED':
      return {
        ...state,
        lastUpdated: dayjs().valueOf(),
        uploads: withoutFile<StagedFile>(state.uploads, action.data.upload),
      };
    case 'SUBMISSION_ATTEMPT_UPLOAD_ADDED':
      return {
        ...state,
        lastUpdated: dayjs().valueOf(),
        uploads: state.uploads.concat(action.data.upload),
      };
    case FILE_STAGING_SELECTED:
    case FILE_STAGING_PROGRESS:
    case FILE_STAGING_SUCCESS:
    case FILE_STAGING_ERROR:
      return {
        ...state,
        staging: fileStagingReducer(state.staging, action),
      };
    default:
      return state;
  }
};

export default submissionInEditReducer;
