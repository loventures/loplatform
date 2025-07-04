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

import ERContentTitle from '../../../commonPages/contentPlayer/ERContentTitle.tsx';
import { get } from 'lodash';
import { ActivityProps } from '../ActivityProps.ts';
import { UnknownResource } from './ResourceActivity.tsx';
import ResourceActivityAudio from './ResourceActivityAudio.tsx';
import ResourceActivityEmbed from './ResourceActivityEmbed.tsx';
import ResourceActivityInstructions from './ResourceActivityInstructions.tsx';
import ResourceActivityLink from './ResourceActivityLink.tsx';
import ResourceActivityVideo from './ResourceActivityVideo.tsx';
import ActivityCompetencies from '../../parts/ActivityCompetencies.tsx';
import { ContentWithRelationships } from '../../../courseContentModule/selectors/assembleContentView.ts';
import { CONTENT_TYPE_RESOURCE } from '../../../utilities/contentTypes.ts';
import React, { Suspense } from 'react';
import LoadingEllipsis from '../../../landmarks/chat/LoadingEllipsis.tsx';

const getResource1Component = (content: ContentWithRelationships): React.FC<ActivityProps<any>> => {
  // only subtype used is readingInstructions and LtiLink
  const subType = get(content, 'activity.resourceType', content.subType);
  switch (subType) {
    case 'audioEmbed':
    case 'videoEmbed':
    case 'readingMaterial':
      return ResourceActivityEmbed;

    case 'audioUpload':
      return ResourceActivityAudio;

    case 'videoUpload':
      return ResourceActivityVideo;

    case 'externalLink':
      return ResourceActivityLink;

    case 'readingInstructions':
      return ResourceActivityInstructions;

    default:
      return UnknownResource;
  }
};

const Resource1Activity: React.FC<ActivityProps<CONTENT_TYPE_RESOURCE>> = ({
  content,
  printView,
}) => {
  const ActivityComponent = getResource1Component(content);

  return (
    <div className="card resource-1">
      <div className="card-body">
        <ERContentTitle
          content={content}
          printView={printView}
        />
        <div className="er-expandable-activity">
          <Suspense fallback={<LoadingEllipsis />}>
            <ActivityComponent
              key={content.id}
              content={content}
              printView={printView}
            />
          </Suspense>
        </div>
        <ActivityCompetencies content={content} />
      </div>
    </div>
  );
};

export default Resource1Activity;
