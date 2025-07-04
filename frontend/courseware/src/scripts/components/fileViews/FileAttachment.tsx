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

import React from 'react';

import PreviewFileRow from './PreviewFileRow';
import SimpleFileRow from './SimpleFileRow';

interface Props {
  file: any;
  preview?: boolean;
  initiallyExpanded?: boolean;
  removeFile?: () => void;
  getSignedUrl?: string;
}

const FileAttachment: React.FC<Props> = ({
  file,
  preview = false,
  removeFile,
  getSignedUrl,
  initiallyExpanded,
}) => {
  const Row = preview ? PreviewFileRow : SimpleFileRow;
  return (
    <Row
      playType={file.playType}
      name={file.fileName}
      icon={file.icon}
      thumbnailUrl={file.thumbnailUrl}
      viewUrl={file.viewUrl}
      downloadUrl={file.downloadUrl || file.viewUrl}
      getSignedUrl={getSignedUrl}
      audioSrc={file.viewUrl || file.base64}
      removeFile={removeFile}
      initiallyExpanded={initiallyExpanded}
    />
  );
};

export default FileAttachment;
