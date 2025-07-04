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

import { EmbeddedContent } from '../../../richContent/embeddedContent.ts';
import ERContentTitle from '../../../commonPages/contentPlayer/ERContentTitle.tsx';
import { useFileBundleDisplayFilesResource } from '../../../resources/FileBundleDisplayFilesResource.ts';
import { map } from 'lodash';
import { ActivityProps } from '../ActivityProps.ts';
import ActivityCompetencies from '../../parts/ActivityCompetencies.tsx';
import { getFileBundleUrl } from '../../../utilities/assetRendering';
import { DisplayFile } from '../../../utilities/assetTypes.ts';
import { CONTENT_TYPE_FILE_BUNDLE } from '../../../utilities/contentTypes.ts';
import React, { useEffect } from 'react';

const SingleFileBundle: React.FC<
  ActivityProps<CONTENT_TYPE_FILE_BUNDLE> & { displayFile: DisplayFile }
> = ({ content, displayFile, onLoaded }) => (
  <EmbeddedContent
    url={getFileBundleUrl(displayFile, content)}
    title={content.name}
    onLoaded={onLoaded}
  />
);

const MultiFileBundle: React.FC<
  ActivityProps<CONTENT_TYPE_FILE_BUNDLE> & { displayFiles: DisplayFile[] }
> = ({ content, displayFiles, onLoaded }) => {
  useEffect(() => onLoaded?.(), [onLoaded]);
  return (
    <ul className="list-group list-group-flush list-unstyled er-expandable-activity">
      {map(displayFiles, displayFile => (
        <li key={displayFile.path}>
          <a
            className="list-group-item"
            href={getFileBundleUrl(displayFile, content)}
            target="_blank"
            rel="noopener"
          >
            {displayFile.displayName}
          </a>
        </li>
      ))}
    </ul>
  );
};

const ResourceActivityFileBundle: React.FC<ActivityProps<CONTENT_TYPE_FILE_BUNDLE>> = ({
  content,
  onLoaded,
  printView,
}) => {
  const displayFilesList = useFileBundleDisplayFilesResource(content.node_name);

  return (
    <div className="card">
      <div className="card-body">
        <ERContentTitle
          content={content}
          printView={printView}
        />

        {displayFilesList.length === 1 ? (
          <SingleFileBundle
            content={content}
            displayFile={displayFilesList[0]}
            onLoaded={onLoaded}
          />
        ) : (
          <MultiFileBundle
            content={content}
            displayFiles={displayFilesList}
            onLoaded={onLoaded}
          />
        )}
        <ActivityCompetencies content={content} />
      </div>
    </div>
  );
};

export default ResourceActivityFileBundle;
