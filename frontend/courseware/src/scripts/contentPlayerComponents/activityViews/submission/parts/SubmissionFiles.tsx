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
import FileAttachment from '../../../../components/fileViews/FileAttachment.tsx';
import FilePendingStaging from '../../../../components/fileViews/FilePendingStaging';
import FileStaged from '../../../../components/fileViews/FileStaged';
import { map } from 'lodash';
import { NGSubmissionActivityAPI } from '../../../../services/SubmissionActivityAPI';
import { FileStagingState } from '../../../../utilities/fileStagingUtils.ts';
import React from 'react';
import { lojector } from '../../../../loject.ts';

interface SubmissionFilesProps {
  attemptId?: number;
  attachments: AttachmentInfo[];
  removeAttachment?: (file: AttachmentInfo) => void;
  uploads?: StagedFile[];
  removeUpload?: (file: StagedFile) => void;
  filesPendingStaging?: FileStagingState;
  previewFirst?: boolean;
}

const SubmissionFiles: React.FC<SubmissionFilesProps> = ({
  attemptId,
  attachments = [],
  removeAttachment,
  uploads = [],
  removeUpload,
  filesPendingStaging,
  previewFirst,
}) => {
  const SubmissionActivityAPI: NGSubmissionActivityAPI = lojector.get('SubmissionActivityAPI');
  return attachments.length || uploads.length || filesPendingStaging?.unstagedFiles.length ? (
    <ul className="list-group list-unstyled mt-2 mt-first-0">
      {map(attachments, (file, index) => (
        <li
          className="mt-2"
          key={file.id}
        >
          <FileAttachment
            file={file}
            removeFile={removeAttachment && (() => removeAttachment(file))}
            preview
            initiallyExpanded={previewFirst && !index}
            getSignedUrl={
              attemptId == null
                ? undefined
                : SubmissionActivityAPI.createAttachmentRedirectUrl(attemptId, file.id)
            }
          />
        </li>
      ))}

      {map(uploads, file => (
        <li
          className="mt-2"
          key={file.guid}
        >
          <FileStaged
            file={file}
            removeFile={() => {
              removeUpload && removeUpload(file);
            }}
            preview
          />
        </li>
      ))}

      {filesPendingStaging &&
        map(filesPendingStaging.unstagedFiles, (rawFile, index) => (
          <li
            className="mt-2"
            key={rawFile.name}
          >
            <FilePendingStaging
              file={rawFile}
              progress={filesPendingStaging.progresses[index]}
              preview
            />
            <span className="text-danger">{filesPendingStaging.errors[index]}</span>
          </li>
        ))}
    </ul>
  ) : null;
};

export default SubmissionFiles;
