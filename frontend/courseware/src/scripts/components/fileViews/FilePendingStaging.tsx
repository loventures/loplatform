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
import { Progress } from 'reactstrap';
import SimpleFileRow from './SimpleFileRow';
import PreviewFileRow from './PreviewFileRow';

interface FilePendingStagingProps {
  file: any;
  progress: { percent?: number };
  preview?: boolean;
  FileRow?: ComponentType<any>;
}

const FilePendingStaging = ({
  file,
  progress,
  preview = false,
  FileRow = preview ? PreviewFileRow : SimpleFileRow,
}: FilePendingStagingProps) => (
  <div>
    <FileRow
      name={file.name}
      icon={file.icon}
      thumbnailUrl={file.preview}
      viewUrl={file.preview}
      downloadUrl={file.preview}
      audioSrc={file.preview}
    />
    <Progress value={progress.percent}>{progress.percent}%</Progress>
  </div>
);

export default FilePendingStaging;
