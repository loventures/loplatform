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
import { Link } from 'react-router-dom';

import { trackFeedbackPageView } from '../analytics/AnalyticsEvents';
import { useBranchId, useHomeNodeName } from '../hooks';
import { useProjectNodes } from '../structurePanel/projectGraphActions';
import { Asset } from '../types/asset';
import { useCurrentFeedback } from './feedbackHooks';

const truncateTitle = (s?: string) => s?.replace(/:.*/, '');

const FeedbackContentHeader: React.FC = () => {
  const feedback = useCurrentFeedback();

  const branchId = useBranchId();
  const homeNodeName = useHomeNodeName();
  const nodes = useProjectNodes();
  const unitAsset =
    feedback?.unitName == feedback?.assetName ? undefined : nodes[feedback?.unitName];
  const moduleAsset =
    feedback?.moduleName == feedback?.assetName ? undefined : nodes[feedback?.moduleName];
  const lessonAsset =
    feedback?.lessonName == feedback?.assetName ? undefined : nodes[feedback?.lessonName];
  const contentAsset =
    feedback?.contentName == feedback?.assetName ? undefined : nodes[feedback?.contentName];

  const assetUrl = (
    asset: Asset<any> | undefined,
    path: Array<string | null | undefined | false>
  ): string =>
    `/branch/${branchId}/story/${asset?.name}?contextPath=${path.filter(p => !!p).join('.')}`;
  const unitPath = assetUrl(unitAsset, [homeNodeName]);
  const modulePath = assetUrl(moduleAsset, [homeNodeName, unitAsset?.name]);
  const lessonPath = assetUrl(lessonAsset, [homeNodeName, unitAsset?.name, moduleAsset?.name]);
  const contentPath = assetUrl(contentAsset, [
    homeNodeName,
    unitAsset?.name,
    moduleAsset?.name,
    lessonAsset?.name,
  ]);

  return (
    <div className="d-flex align-items-center justify-content-center minw-0 feedback-context">
      {unitAsset && (
        <>
          <Link
            to={unitPath}
            onClick={() => trackFeedbackPageView('Unit')}
            className="unhover-muted text-truncate"
          >
            {truncateTitle(unitAsset.data.title)}
          </Link>
        </>
      )}
      {moduleAsset && (
        <>
          {unitAsset && <span className="text-muted mx-2">/</span>}
          <Link
            to={modulePath}
            onClick={() => trackFeedbackPageView('Module')}
            className="unhover-muted text-truncate"
          >
            {truncateTitle(moduleAsset.data.title)}
          </Link>
        </>
      )}
      {lessonAsset && (
        <>
          {moduleAsset && <span className="text-muted mx-2">/</span>}
          <Link
            to={lessonPath}
            onClick={() => trackFeedbackPageView('Lesson')}
            className="unhover-muted text-truncate"
          >
            {truncateTitle(lessonAsset.data.title)}
          </Link>
        </>
      )}
      {contentAsset && (
        <>
          {(moduleAsset || lessonAsset) && <span className="text-muted mx-2">/</span>}
          <Link
            to={contentPath}
            onClick={() => trackFeedbackPageView('Content')}
            className="unhover-muted text-truncate"
            data-id="title"
          >
            {contentAsset.data.title}
          </Link>
        </>
      )}
      {!moduleAsset && !lessonAsset && !contentAsset && <div>&nbsp;</div>}
    </div>
  );
};

export default FeedbackContentHeader;
