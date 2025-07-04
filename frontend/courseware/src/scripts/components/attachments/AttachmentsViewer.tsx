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

import { AttachmentInfo } from '../../api/quizApi';
import FileAttachment from '../../components/fileViews/FileAttachment';
import { map } from 'lodash';
import React, { HTMLAttributes } from 'react';

const AttachmentsViewer: React.FC<
  {
    attachments: AttachmentInfo[];
  } & HTMLAttributes<any>
> = ({ className = '', attachments }) => {
  return (
    <div className={`attachments-viewer ${className}`}>
      <ul className="list-group">
        {map(attachments, att => (
          <li
            className="list-group-item"
            key={att.id}
          >
            <FileAttachment file={att} />
          </li>
        ))}
      </ul>
    </div>
  );
};

export default AttachmentsViewer;
