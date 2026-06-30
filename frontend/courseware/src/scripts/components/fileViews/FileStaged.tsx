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

import { ComponentType } from 'react';
import SimpleFileRow from './SimpleFileRow';
import PreviewFileRow from './PreviewFileRow';

const getUrl = (guid: string) => {
  return `/api/v2/uploads/${guid}`;
};

interface FileStagedProps {
  file: any;
  preview?: boolean;
  FileRow?: ComponentType<any>;
  stagingUrl?: string;
  downloadUrl?: string;
  removeFile?: () => void;
}

const FileStaged = ({
  file,
  preview = false,
  FileRow = preview ? PreviewFileRow : SimpleFileRow,

  stagingUrl = getUrl(file.guid),
  downloadUrl = getUrl(file.guid) + '?download=true',
  removeFile,
}: FileStagedProps) => (
  <FileRow
    playType={file.playType}
    name={file.fileName}
    icon={file.icon}
    thumbnailUrl={stagingUrl}
    viewUrl={stagingUrl}
    downloadUrl={downloadUrl}
    audioSrc={stagingUrl}
    removeFile={removeFile}
  />
);

export default FileStaged;
