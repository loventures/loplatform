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

import Course from '../../../bootstrap/course.ts';
import { loConfig } from '../../../bootstrap/loConfig.ts';
import ERContentTitle from '../../../commonPages/contentPlayer/ERContentTitle.tsx';
import TooltippedGradeBadge from '../../../components/TooltippedGradeBadge.tsx';
import { useAssetInfo } from '../../../resources/AssetInfo.ts';
import { useCourseSelector } from '../../../loRedux';
import { ActivityProps } from '../ActivityProps.ts';
import ActivityCompetencies from '../../parts/ActivityCompetencies.tsx';
import ContentBlockInstructions from '../../parts/ContentBlockInstructions';
import { reportProgressActionCreator } from '../../../courseActivityModule/actions/activityActions.ts';
import { ContentWithRelationships } from '../../../courseContentModule/selectors/assembleContentView.ts';
import { useTranslation } from '../../../i18n/translationContext.tsx';
import { CONTENT_TYPE_COURSE_LINK } from '../../../utilities/contentTypes.ts';
import { UserWithRoleInfo, selectCurrentUser } from '../../../utilities/rootSelectors.ts';
import UrlBuilder from '../../../utilities/UrlBuilder.ts';
import React, { useCallback } from 'react';
import { MdOutlineOpenInNew } from 'react-icons/md';
import { useDispatch } from 'react-redux';

export const getLaunchUrl = (content: ContentWithRelationships, user: UserWithRoleInfo) => {
  const previewRole =
    user.user_type !== 'Preview' ? undefined : user.isInstructor ? 'Instructor' : 'Learner';
  return new UrlBuilder(loConfig.courseLink.launch, {
    context: Course.id,
    path: content.id,
    role: previewRole,
  }).toString();
};

export const INTERNAL_RETURN_URL = 'internal:returnUrl';
export const INTERNAL_RETURN_NAME = 'internal:returnName';

const ResourceActivityCourseLink: React.FC<ActivityProps<CONTENT_TYPE_COURSE_LINK>> = ({
  content,
  printView,
}) => {
  const dispatch = useDispatch();
  const translate = useTranslation();
  const assetInfo = useAssetInfo(content);
  const user = useCourseSelector(selectCurrentUser);
  const recordProgress = useCallback(() => {
    // TODO: emit progress on click to navigate away from the current page is sketch and
    // unreliable; this should be done on the server.
    if (!content.hasGradebookEntry) dispatch(reportProgressActionCreator(content, true));
    if (!assetInfo.newWindow) {
      sessionStorage.setItem(`${INTERNAL_RETURN_URL}:${assetInfo.branch}`, window.location.href);
      sessionStorage.setItem(`${INTERNAL_RETURN_NAME}:${assetInfo.branch}`, Course.name);
    }
  }, [dispatch, content.id, printView]);
  return (
    <div className="card internal-course-activity er-content-wrapper">
      <div className="card-body">
        <ERContentTitle
          content={content}
          printView={printView}
        />
        <TooltippedGradeBadge />
        <ActivityCompetencies content={content} />
        <div className="er-expandable-activity">
          {assetInfo.instructions.renderedHtml ? (
            <ContentBlockInstructions
              instructions={assetInfo.instructions}
              className="mb-4"
            />
          ) : null}
          {!printView && (
            <div className="flex-center-center d-print-none mb-3">
              <a
                className="btn btn-success btn-lg d-flex align-items-center"
                href={getLaunchUrl(content, user)}
                onClick={recordProgress}
                target={assetInfo.newWindow ? '_blank' : undefined}
              >
                {translate('LAUNCH_COURSE_LINK', { content })}
                {assetInfo.newWindow && <MdOutlineOpenInNew className="ms-2" />}
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ResourceActivityCourseLink;
