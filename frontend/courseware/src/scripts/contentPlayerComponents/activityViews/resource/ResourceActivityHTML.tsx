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
import Course from '../../../bootstrap/course.ts';
import ERContentTitle from '../../../commonPages/contentPlayer/ERContentTitle.tsx';
import { ActivityProps } from '../ActivityProps.ts';
import ActivityCompetencies from '../../parts/ActivityCompetencies.tsx';
import { ContentWithRelationships } from '../../../courseContentModule/selectors/assembleContentView.ts';
import { getAssetRenderUrl, getCourseAssetRenderUrl } from '../../../utilities/assetRendering';
import { CONTENT_TYPE_RESOURCE } from '../../../utilities/contentTypes.ts';
import { enableInlineLTI } from '../../../utilities/preferences.ts';
import React from 'react';

// always enable inline LTI in test/preview sections
const getUrl = (content: ContentWithRelationships<CONTENT_TYPE_RESOURCE>) =>
  enableInlineLTI || (!Course.offering_id && content.id)
    ? getCourseAssetRenderUrl(content.id)
    : getAssetRenderUrl(content.node_name);

const ResourceActivityHTML: React.FC<ActivityProps<CONTENT_TYPE_RESOURCE>> = ({
  content,
  onLoaded,
  printView,
}) => {
  return (
    <div className="card">
      <div className="card-body">
        <ERContentTitle
          content={content}
          printView={printView}
        />
        <EmbeddedContent
          url={getUrl(content)}
          title={content.name}
          onLoaded={onLoaded}
          printView={printView}
        />
        <ActivityCompetencies
          className="post-html"
          content={content}
        />
      </div>
    </div>
  );
};

export default ResourceActivityHTML;
