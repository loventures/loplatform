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
import React, { useMemo } from 'react';
import { Progress } from 'reactstrap';

import Previewer from '../../components/fileViews/Previewer.tsx';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { FileContainer } from '../pure/fileContainer.ts';

interface FeedbackFile {
  info?: { playType?: string; downloadUrl?: string; displayName?: string; viewUrl?: string };
  progress?: { percent?: number; status?: string };
}
interface FeedbackManager {
  toggleRemovalStaging: (file: FeedbackFile) => void;
}

export interface FeedbackFileListProps {
  files?: FeedbackFile[];
  rawFiles?: any[];
  feedbackManager?: FeedbackManager;
  // thumbnailSize / removeAction were directive bindings unused by this template.
  [key: string]: any;
}

/**
 * React port of the `feedbackFileList` directive — the list of feedback/submission
 * attachments (download link, optional remove button, file preview, upload
 * progress). The file previewer (already React) and reactstrap `Progress` replace
 * the Angular `<previewer>` / `<uib-progressbar>`. Imported natively by its consumers
 * (essay, grading, discussion, viewCompositeGrade). DOM preserved (`ul.list-group…`,
 * `.file-preview`).
 */
export const FeedbackFileList: React.FC<FeedbackFileListProps> = ({ files, rawFiles, feedbackManager }) => {
  const translate = useTranslation();

  // `rawFiles` (plain file infos) are wrapped in the Angular FileContainer model.
  const resolved: FeedbackFile[] = useMemo(() => {
    if (rawFiles) {
      return map(rawFiles, file => new FileContainer(file));
    }
    return files ?? [];
  }, [rawFiles, files]);

  if (!resolved.length) return null;

  return (
    <ul className="list-group list-group-flush list-group-striped">
      {resolved.map((file, i) => (
        <li
          className="list-group-item py-1"
          key={i}
        >
          <div className="file-preview">
            {!file.progress && (
              <div className="d-flex flex-column">
                <div className="d-flex justify-content-between">
                  {file.info?.playType === 'file' && (
                    <a
                      className="flex-col-fluid"
                      href={file.info.downloadUrl}
                      target="_blank"
                      rel="noreferrer"
                    >
                      {file.info.displayName}
                    </a>
                  )}
                  {feedbackManager && (
                    <button
                      className="icon icon-cross"
                      aria-label={translate('UPLOADS_REMOVE_FILE')}
                      onClick={() => feedbackManager.toggleRemovalStaging(file)}
                    />
                  )}
                </div>
                <Previewer
                  name={file.info?.displayName ?? ''}
                  viewUrl={file.info?.viewUrl}
                />
              </div>
            )}

            {file.progress && (
              <div className="upload-progress-bar-container">
                <Progress
                  className="progress-striped active"
                  value={file.progress.percent}
                  color="info"
                >
                  {file.progress.status}
                </Progress>
              </div>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
};
