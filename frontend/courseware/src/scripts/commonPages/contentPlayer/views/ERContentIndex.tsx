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

import ERCompletion from '../../../commonPages/contentPlayer/ERCompletion';
import ERNextUp from '../../../commonPages/contentPlayer/ERNextUp';
import ERReturnHome from '../../../commonPages/contentPlayer/ERReturnHome';
import ERActivity, { ERActivityProps } from '../../../commonPages/contentPlayer/views/ERActivity';
import ContentSurvey from '../../../components/survey/ContentSurvey';
import { ERLandmark } from '../../../landmarks/ERLandmarkProvider';
import { useContentGatingInfoResource } from '../../../resources/GatingInformationResource';
import { useNextUpFromContent } from '../../../resources/useNextUp';
import { useCourseSelector } from '../../../loRedux';
import { ResourceTypes } from '../../../contentPlayerComponents/activityViews/resource/ResourceActivity';
import { selectNavToPageContent } from '../../../courseContentModule/selectors/contentEntrySelectors';
import { timeoutEffect } from '../../../utilities/effectUtils';
import { showContentSurveys } from '../../../utilities/preferences';
import React, { useCallback, useEffect, useState } from 'react';

type ERContentIndexProps = Omit<ERActivityProps, 'printView' | 'onLoaded'>;

/**
 *
 * Replaces ContentIndex
 *
 */
const ERContentIndex: React.FC<ERContentIndexProps> = ({ content, viewingAs, actualUser }) => {
  const gatingInfo = useContentGatingInfoResource(content.id, viewingAs.id);
  const nextUp = useNextUpFromContent();
  const showCompletion = ResourceTypes.has(content.typeId);
  const doNav = useCourseSelector(selectNavToPageContent);

  const [loadedId, setLoadedId] = useState<string | undefined>(undefined);
  const onLoaded = useCallback(() => setLoadedId(content.id), [setLoadedId, content.id]);
  const loaded = loadedId === content.id;

  useEffect(() => setLoadedId(undefined), [content.id]);
  useEffect(timeoutEffect(onLoaded, 2000), [onLoaded]); // safeguard in case content fails to call back

  return (
    <ERLandmark
      landmark="content"
      className="container p-0"
    >
      <div className="content-plain-index mb-3 m-md-3 m-lg-4">
        <ERActivity
          content={content}
          viewingAs={viewingAs}
          actualUser={actualUser}
          onLoaded={onLoaded}
        />

        {showCompletion && !viewingAs.isInstructor && !gatingInfo.isLocked && (
          <ERCompletion
            content={content}
            loaded={loaded}
          />
        )}

        {showContentSurveys && !viewingAs.isInstructor && !gatingInfo.isLocked && (
          <ContentSurvey
            content={content}
            loaded={loaded}
          />
        )}

        <div className="content-footer-wrapper mt-4 mt-md-4 mt-lg-5">
          {doNav && nextUp ? (
            <ERNextUp
              content={content}
              nextUp={nextUp}
            />
          ) : (
            <ERReturnHome />
          )}
        </div>
      </div>
    </ERLandmark>
  );
};

export default ERContentIndex;
