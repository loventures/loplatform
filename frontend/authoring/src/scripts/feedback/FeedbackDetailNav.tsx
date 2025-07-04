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

import classNames from 'classnames';
import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

import { trackFeedbackPageNav } from '../analytics/AnalyticsEvents';
import { useBranchId } from '../hooks';
import { loadFeedbackIds } from './FeedbackApi';
import { useFeedbackFilters } from './feedbackHooks';

const FeedbackDetailNav: React.FC<{ feedback: number }> = ({ feedback }) => {
  const branchId = useBranchId();
  const { branch, status, assignee, module, unit } = useFeedbackFilters();
  const feedbackBranch = branch ?? branchId;

  // We load these once on mount so that the index remains stable, even
  // as you update tickets and page through them. Otherwise every time you
  // update a ticket its modified time will be updated and so you'll move
  // to the start of the order again.
  const [ids, setIds] = useState<number[]>([]);
  useEffect(() => {
    loadFeedbackIds(feedbackBranch, {
      status,
      assignee,
      module,
      unit,
      remotes: branch ? branchId : undefined,
    }).then(setIds);
  }, [feedbackBranch, status, assignee, module, unit, setIds]);

  const [index, lag, lead] = useMemo(() => {
    const index = ids.indexOf(feedback);
    return index < 0 ? [undefined, undefined, undefined] : [index, ids[index - 1], ids[index + 1]];
  }, [ids, feedback]);

  return (
    <div className="d-flex align-items-center">
      <div className="me-1">{index != null ? `${index + 1} of ${ids.length}` : ''}</div>
      <Link
        className={classNames(
          'material-icons md-18 p-1 ms-2 btn btn-transparent mini-button',
          !lag && 'disabled'
        )}
        id="previous-feedback-button"
        title="Previous feedback"
        to={`/branch/${branchId}/feedback/${lag}`}
        onClick={e => {
          if (!lag) e.preventDefault();
          else trackFeedbackPageNav('Previous');
        }}
      >
        chevron_left
      </Link>
      <Link
        className={classNames(
          'material-icons md-18 p-1 ms-2 btn btn-transparent mini-button',
          !lead && 'disabled'
        )}
        id="next-feedback-button"
        title="Next feedback"
        to={`/branch/${branchId}/feedback/${lead}`}
        onClick={e => {
          if (!lead) e.preventDefault();
          else trackFeedbackPageNav('Next');
        }}
      >
        chevron_right
      </Link>
    </div>
  );
};

export default FeedbackDetailNav;
