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

import Previewer from '../../components/fileViews/Previewer';
import { useTranslation } from '../../i18n/translationContext';
import React, { useState } from 'react';

interface PreviewFileRowProps {
  name: string;
  viewUrl?: string;
  downloadUrl: string;
  removeFile?: () => void;
  initiallyExpanded?: boolean;
  getSignedUrl?: string;
}

const PreviewFileRow: React.FC<PreviewFileRowProps> = ({
  name,
  downloadUrl,
  removeFile,
  initiallyExpanded,
  viewUrl,
  getSignedUrl,
}) => {
  const [expanded, setExpanded] = useState(!!initiallyExpanded);
  const translate = useTranslation();

  const reloadPreview = () => {
    setExpanded(false);
    setTimeout(() => setExpanded(true), 250);
  };

  return (
    <div className="project-file-container list-group-item">
      <div className="project-file">
        <div className="flex-row-content">
          <button
            className="icon-btn icon-btn-primary expansion-toggle"
            onClick={() => setExpanded(!expanded)}
            aria-label={translate('TOGGLE_FILE_PREVIEW')}
            aria-expanded={expanded}
          >
            {expanded ? (
              <span className="icon-chevron-down"></span>
            ) : (
              <span className="icon-chevron-right"></span>
            )}
          </button>

          <div className="flex-col-fluid file-name">{name}</div>

          <button
            className="icon-btn icon-btn-primary reload-preview"
            onClick={reloadPreview}
            title={translate('UPLOADS_RELOAD_PREVIEW')}
            aria-label={translate('UPLOADS_RELOAD_PREVIEW')}
            disabled={!expanded}
          >
            <span className="icon icon-reload"></span>
          </button>

          <a
            className="icon-btn icon-btn-primary download-file"
            href={downloadUrl}
            title={translate('UPLOADS_DOWNLOAD_FILE')}
            aria-label={translate('UPLOADS_DOWNLOAD_FILE')}
            target="_blank"
          >
            <span className="icon icon-download"></span>
          </a>

          {removeFile && (
            <button
              className="icon-btn icon-btn-danger delete-file"
              onClick={removeFile}
              title={translate('UPLOADS_REMOVE_FILE')}
              aria-label={translate('UPLOADS_REMOVE_FILE')}
            >
              <span className="icon icon-trash"></span>
            </button>
          )}
        </div>
      </div>

      {expanded && (
        <div className="my-2 pt-1 file-preview">
          <Previewer
            name={name}
            viewUrl={viewUrl}
            getSignedUrl={getSignedUrl}
          />
        </div>
      )}
    </div>
  );
};

export default PreviewFileRow;
