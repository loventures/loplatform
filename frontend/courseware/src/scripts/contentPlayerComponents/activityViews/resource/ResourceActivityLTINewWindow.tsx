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
import TooltippedGradeBadge from '../../../components/TooltippedGradeBadge.tsx';
import { useAssetInfo } from '../../../resources/AssetInfo.ts';
import { useCourseSelector } from '../../../loRedux';
import { ActivityProps } from '../ActivityProps.ts';
import ActivityCompetencies from '../../parts/ActivityCompetencies.tsx';
import ContentBlockInstructions from '../../parts/ContentBlockInstructions';
import { reportProgressActionCreator } from '../../../courseActivityModule/actions/activityActions.ts';
import { useTranslation } from '../../../i18n/translationContext.tsx';
import LTINewWindowLauncher from '../../../lti/components/LTINewWindowLauncher.tsx';
import { getLTIConfigUrl } from '../../../lti/configUrl';
import { CONTENT_TYPE_LTI } from '../../../utilities/contentTypes.ts';
import { selectCurrentUser } from '../../../utilities/rootSelectors.ts';
import React, { useCallback } from 'react';
import { MdOutlineOpenInNew } from 'react-icons/md';
import { useDispatch } from 'react-redux';

const ResourceActivityLTI: React.FC<ActivityProps<CONTENT_TYPE_LTI>> = ({ content, printView }) => {
  const dispatch = useDispatch();
  const recordProgress = useCallback(() => {
    if (!printView && !content.hasGradebookEntry)
      dispatch(reportProgressActionCreator(content, true));
  }, [dispatch, content.id, printView]);
  const translate = useTranslation();
  const assetInfo = useAssetInfo(content);
  const user = useCourseSelector(selectCurrentUser);
  return (
    <div className="card lti-activity er-content-wrapper">
      <div className="card-body">
        <ERContentTitle
          content={content}
          printView={printView}
        />
        <TooltippedGradeBadge />
        <div className="er-expandable-activity">
          {assetInfo.instructions.renderedHtml ? (
            <ContentBlockInstructions
              instructions={assetInfo.instructions}
              className="mb-4"
            />
          ) : null}
          {!printView && (
            <div className="flex-center-center d-print-none mb-3">
              <LTINewWindowLauncher
                launchPath={getLTIConfigUrl(content, user)}
                className="btn btn-success btn-lg d-flex align-items-center"
                onClick={recordProgress}
              >
                {translate('LTI_LAUNCH_NEW_WINDOW')}
                <MdOutlineOpenInNew className="ms-2" />
              </LTINewWindowLauncher>
            </div>
          )}
        </div>
        <ActivityCompetencies content={content} />
      </div>
    </div>
  );
};

export default ResourceActivityLTI;
