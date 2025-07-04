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

import axios from 'axios';
import Course from '../bootstrap/course';
import { useCourseSelector } from '../loRedux';
import { selectCurrentUserOverallProgress } from '../selectors/progressSelectors';
import { selectCurrentUser } from '../utilities/rootSelectors';
import React from 'react';
import { Button } from 'reactstrap';

export const ResetLearnerDataWidget: React.FC = () => {
  const viewingAs = useCourseSelector(selectCurrentUser);
  const progress = useCourseSelector(selectCurrentUserOverallProgress);

  const resetLearnerData = () => {
    if (confirm('Reset your test student grades, progress and attempts?')) {
      axios.post(`/api/v2/lwc/${Course.id}/resetLearner`, {}).then(() => window.location.reload());
    }
  };

  // Preview student anywhere, or regular student in test section
  const canReset =
    (Course.groupType === 'TestSection' || viewingAs.user_type === 'Preview') &&
    !viewingAs.isPreviewing &&
    viewingAs.isStudent &&
    !!progress.completions;

  return !canReset ? null : (
    <Button
      color="danger"
      className="mt-4 align-self-center"
      onClick={resetLearnerData}
    >
      Reset Your Test Student Data
    </Button>
  );
};
