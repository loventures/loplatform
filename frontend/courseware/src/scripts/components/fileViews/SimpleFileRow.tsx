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

import IconButton from '../../components/IconButton';
import { Translate, withTranslation } from '../../i18n/translationContext';
import React from 'react';

const getExtension = (name: string) => {
  return name.slice(name.lastIndexOf('.') + 1).toLowerCase();
};

const fileIconMap: Record<string, string> = {
  zip: 'icon-file-zip',
  xml: 'icon-file-xml',
  csv: 'icon-file-spreadsheet',
  txt: 'icon-file-text',
  doc: 'icon-file-text',
  docx: 'icon-file-text',
};

interface SimpleFilePreviewProps {
  translate: Translate;
  playType?: string;
  name: string;
  icon?: string;
  thumbnailUrl?: string;
  downloadUrl?: string;
  audioSrc?: string;
  removeFile?: () => void;
}

const SimpleFilePreview: React.FC<SimpleFilePreviewProps> = ({
  translate,
  playType = 'file',
  name,
  icon = fileIconMap[getExtension(name)] || fileIconMap.txt,
  thumbnailUrl = null,
  downloadUrl = null,
  audioSrc,
  removeFile,
}) => {
  return (
    <div className="file-preview">
      <div className="flex-row-content">
        {thumbnailUrl ? (
          <img
            className="file-preview-image inline-image"
            src={thumbnailUrl}
            alt={translate('THUMBNAIL_ALT')}
          ></img>
        ) : (
          <span
            className={icon}
            role="presentation"
          ></span>
        )}

        {playType === 'audio' ? (
          <audio //eslint-disable-line jsx-a11y/media-has-caption
            className="flex-col-fluid"
            controls={true}
            src={audioSrc}
          />
        ) : downloadUrl ? (
          <a
            className="flex-col-fluid text-truncate"
            href={downloadUrl}
            target="_blank"
          >
            {name}
          </a>
        ) : (
          <span className="flex-col-fluid text-truncate">{name}</span>
        )}

        {removeFile && (
          <IconButton
            className="p-1"
            color="danger"
            icon="icon-cross"
            srOnly={translate('UPLOADS_REMOVE_FILE')}
            onClick={removeFile}
          />
        )}
      </div>
    </div>
  );
};

export default withTranslation(SimpleFilePreview);
