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

import LoadingEllipsis from '../../../landmarks/chat/LoadingEllipsis.tsx';
import { ActivityProps } from '../ActivityProps.ts';
import Resource1Activity from './Resource1Activity.tsx';
import ResourceActivityCourseLink from './ResourceActivityCourseLink.tsx';
import { ContentWithRelationships } from '../../../courseContentModule/selectors/assembleContentView.ts';
import { LTI_TYPE_FRAMED } from '../../../utilities/contentSubTypes';
import {
  CONTENT_TYPE_COURSE_LINK,
  CONTENT_TYPE_FILE_BUNDLE,
  CONTENT_TYPE_HTML,
  CONTENT_TYPE_LTI,
  CONTENT_TYPE_RESOURCE,
  CONTENT_TYPE_SCORM,
} from '../../../utilities/contentTypes.ts';
import React, { useCallback, useEffect, useState } from 'react';

import ContentError from '../../parts/ContentError';
import ResourceActivityFileBundle from './ResourceActivityFileBundle.tsx';
import ResourceActivityHTML from './ResourceActivityHTML.tsx';
import ResourceActivityLTIFramed from './ResourceActivityLTIFramed.tsx';
import ResourceActivityLTINewWindow from './ResourceActivityLTINewWindow.tsx';
import ResourceActivityScorm from './ResourceActivityScorm.tsx';
import { Alert } from 'reactstrap';
import { ErrorBoundary } from 'react-error-boundary';
import { createUrl, loConfig } from '../../../bootstrap/loConfig.ts';
import axios from 'axios';
import Course from '../../../bootstrap/course.ts';

export const UnknownResource: React.FC<ActivityProps<any>> = ({ content }) => (
  <ContentError
    message="CONTENT_PLAYER_UNKNOWN_RESOURCE"
    content={content}
    at={'ResourceActivity'}
  />
);

const ResourceActivityLTI: React.FC<ActivityProps<any>> = props => {
  const [style, setStyle] = useState<string>();
  useEffect(() => {
    const url = createUrl(loConfig.lti.launchStyle, {
      context: Course.id,
      path: props.content.id,
    });
    axios
      .get<string>(url)
      .then(({ data }) => setStyle(data))
      .catch(() => setStyle('ERROR'));
  }, [props.content.id]);

  return style == null ? (
    <LoadingEllipsis />
  ) : style === 'ERROR' ? (
    <Alert color="danger">An error occurred.</Alert>
  ) : style === LTI_TYPE_FRAMED ? (
    <ResourceActivityLTIFramed {...props} />
  ) : (
    <ResourceActivityLTINewWindow {...props} />
  );
};

const getResourceComponent = (content: ContentWithRelationships): React.FC<ActivityProps<any>> => {
  switch (content.typeId) {
    case CONTENT_TYPE_FILE_BUNDLE:
      return ResourceActivityFileBundle;

    case CONTENT_TYPE_SCORM:
      return ResourceActivityScorm;

    case CONTENT_TYPE_HTML:
      return ResourceActivityHTML;

    case CONTENT_TYPE_LTI:
      return ResourceActivityLTI;

    case CONTENT_TYPE_COURSE_LINK:
      return ResourceActivityCourseLink;

    case CONTENT_TYPE_RESOURCE:
      return Resource1Activity;

    default:
      return UnknownResource;
  }
};

export const ResourceTypes = new Set([
  CONTENT_TYPE_RESOURCE,
  CONTENT_TYPE_LTI,
  CONTENT_TYPE_HTML,
  CONTENT_TYPE_SCORM,
  CONTENT_TYPE_FILE_BUNDLE,
  CONTENT_TYPE_COURSE_LINK,
]);

const ResourceTypesWithOnLoad = new Set<string>([CONTENT_TYPE_HTML, CONTENT_TYPE_FILE_BUNDLE]);

const ResourceActivity: React.FC<ActivityProps<any>> = ({ content, onLoaded, printView }) => {
  const [loaded, setLoaded] = useState<string | undefined>(undefined);

  useEffect(() => setLoaded(undefined), [onLoaded]);

  const resourceLoaded = useCallback(() => {
    if (loaded !== content.id) {
      setLoaded(content.id);
      onLoaded?.();
    }
  }, [onLoaded, loaded, setLoaded]);

  useEffect(
    () => (ResourceTypesWithOnLoad.has(content.typeId) ? void 0 : resourceLoaded()),
    [content.typeId, resourceLoaded]
  );

  const ActivityComponent = getResourceComponent(content);

  // the key on ActivityComponent is to avoid reusing the Angular embedded-content; it doesn't
  // respect changes to its properties so if the content changes it doesn't send any loaded
  // or resizing notifications.
  return (
    <div className="resource-activity feedback-context">
      <ErrorBoundary
        fallback={
          <div className="p-4">
            <Alert color="danger">Something went wrong.</Alert>
          </div>
        }
      >
        <ActivityComponent
          key={content.id}
          content={content}
          onLoaded={resourceLoaded}
          printView={printView}
        />
      </ErrorBoundary>
    </div>
  );
};

export default ResourceActivity;
