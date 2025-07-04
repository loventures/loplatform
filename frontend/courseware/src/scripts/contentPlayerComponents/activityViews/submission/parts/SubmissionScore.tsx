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

import { BasicScore, InstructorFeedback, Rubric, RubricScore } from '../../../../api/quizApi.ts';
import { filter, flatMap, map } from 'lodash';
import ViewRubricScore from '../../../../directives/ViewRubricScore';
import { BASIC_FEEDBACK, RUBRIC_FEEDBACK } from '../../../../utilities/scoreTypes.js';
import React from 'react';

const SubmissionScore: React.FC<{
  score?: RubricScore | BasicScore | null;
  rubric?: Rubric | null;
  feedback?: InstructorFeedback[];
}> = ({ score, rubric, feedback }) => {
  const basicFeedback = filter(feedback, f => f.feedbackType === BASIC_FEEDBACK);
  const rubricFeedback = filter(feedback, f => f.feedbackType === RUBRIC_FEEDBACK);
  const feedbackComments = map(basicFeedback, 'comment').join('\n');
  const scoreAttachments = flatMap(basicFeedback, 'attachments');
  return (
    <ViewRubricScore
      score={score}
      scoreAttachments={scoreAttachments}
      feedback={feedbackComments}
      rubric={rubric}
      rubricFeedback={rubricFeedback}
    />
  );
};

export default SubmissionScore;
