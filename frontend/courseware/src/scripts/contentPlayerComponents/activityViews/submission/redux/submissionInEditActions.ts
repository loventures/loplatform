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

import { IoFile } from '../../../../api/fileUploadApi.ts';
import { AttachmentInfo } from '../../../../api/quizApi.ts';
import { fileStagingActionCreatorMaker } from '../../../../utilities/fileStagingUtils.ts';

export const submissionAttemptEditTextActionCreator = (
  contentId: string,
  userId: number,
  essay: string
) => ({
  type: 'SUBMISSION_ATTEMPT_RESPONSE_TEXT_CHANGED',
  contentId,
  userId,
  data: { essay },
});

export const submissionAttemptRemoveAttachmentsActionCreator = (
  contentId: string,
  userId: number,
  attachmentId: number,
  existingAttachments: AttachmentInfo[]
) => ({
  type: 'SUBMISSION_ATTEMPT_ATTACHMENT_REMOVED',
  contentId,
  userId,
  data: { attachmentId, existingAttachments },
});

export const submissionAttemptRemoveUploadActionCreator = (
  contentId: string,
  userId: number,
  upload: unknown
) => ({
  type: 'SUBMISSION_ATTEMPT_UPLOAD_REMOVED',
  contentId,
  userId,
  data: { upload },
});

export const submissionAttemptAddUploadActionCreator = (
  upload: IoFile,
  contentId: string,
  userId: number
) => ({
  type: 'SUBMISSION_ATTEMPT_UPLOAD_ADDED',
  contentId,
  userId,
  data: { upload },
});

export const submissionAttemptStageFileActionCreator: (
  upload: File,
  contentId: string,
  userId: number
) => void = fileStagingActionCreatorMaker(
  [submissionAttemptAddUploadActionCreator],
  (contentId, userId) => ({
    sliceName: 'submissionInEditByContentByUser',
    contentId,
    userId,
  })
);
