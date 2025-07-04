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

import React, { useMemo } from 'react';
import { Link } from 'react-router-dom';

import { trackFeedbackPageNav } from '../analytics/AnalyticsEvents';
import { useBranchId, useDcmSelector, useDocumentTitle } from '../hooks';
import Presence from '../presence/Presence';
import { EditModeSwitch } from '../story/EditModeSwitch';
import { QuillMenu } from '../story/NarrativeActionBar/QuillMenu';
import { isQuestion } from '../story/questionUtil';
import { useProjectNode } from '../structurePanel/projectGraphActions';
import { useCurrentFeedback } from './feedbackHooks';

const FeedbackDetailActionBar: React.FC = () => {
  const presenceEnabled = useDcmSelector(s => s.configuration.presenceEnabled);
  const branchId = useBranchId();
  const feedback = useCurrentFeedback();
  const asset = useProjectNode(feedback?.contentName ?? feedback?.assetName);

  const title = isQuestion(asset?.typeId) ? 'Question' : asset?.data.title;
  const crumbs = useMemo(() => ['Feedback', title], [title]);
  useDocumentTitle(crumbs);

  return (
    <div className="d-flex align-items-center h-100 px-3 narrative-action-bar">
      <h6 className="m-0 flex-grow-1 d-flex align-items-center">
        <QuillMenu />
        <Link
          to={`/branch/${branchId}/feedback`}
          onClick={() => trackFeedbackPageNav('Parent')}
        >
          Feedback
        </Link>
        <span className="text-muted mx-2">/</span>
        <span className="text-muted">{title ?? '...'}</span>
      </h6>
      <div className="d-flex align-items-center">
        {presenceEnabled && <Presence compact />}
        <EditModeSwitch />
      </div>
    </div>
  );
};

export default FeedbackDetailActionBar;
