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

import { BasicScore, RubricScore } from '../../../../api/quizApi.ts';
import { SubmissionAssessment } from '../../../../api/submissionApi.ts';
import { HtmlWithMathJax } from '../../../../components/HtmlWithMathjax.tsx';
import { DisplaySubmissionAttempt } from '../submissionActivity';
import {
  ContentWithNebulousDetails,
  ViewingAs,
} from '../../../../courseContentModule/selectors/contentEntry';
import { isScoreFinalized } from '../../../../utilities/attemptStates';
import React from 'react';
import { react2angular } from 'react2angular';

import GraderJumpButton from '../parts/GraderJumpButton.tsx';
import SubmissionFiles from '../parts/SubmissionFiles.tsx';
import SubmissionPendingGrade from '../parts/SubmissionPendingGrade.tsx';
import SubmissionScore from '../parts/SubmissionScore.tsx';

interface SubmissionAttemptViewProps {
  content: ContentWithNebulousDetails;
  viewingAs: ViewingAs;
  assessment: SubmissionAssessment;
  attempt: DisplaySubmissionAttempt;
  isGrading?: boolean;
}

const SubmissionAttemptView: React.FC<SubmissionAttemptViewProps> = ({
  content,
  viewingAs,
  assessment,
  attempt,
  isGrading = false,
}) => (
  <>
    {!isGrading && viewingAs.isPreviewing && (
      <div className="flex-center-center py-3 d-print-none">
        <GraderJumpButton
          content={content}
          attempt={attempt}
          viewingAs={viewingAs}
        />
      </div>
    )}

    {!isGrading && !isScoreFinalized(attempt) && <SubmissionPendingGrade />}

    {!isGrading && (hasScore(attempt.score) || attempt.feedback) && (
      <SubmissionScore
        rubric={assessment.rubric}
        score={attempt.score}
        feedback={attempt.feedback}
      />
    )}

    {attempt.essay && (
      <div className="essay-response-text mb-3">
        <HtmlWithMathJax html={attempt.essay} />
      </div>
    )}

    <SubmissionFiles
      attemptId={attempt.id}
      attachments={attempt.attachments}
      previewFirst
    />
  </>
);

const hasScore = (score: BasicScore | RubricScore | null) =>
  score?.scoreType === 'basic' || (score?.scoreType === 'rubric' && score.nullableCriterionScores);

export default SubmissionAttemptView;

export const submissionAttemptViewComponent = angular
  .module('lo.contentPlayer.submissionAttemptView', [])
  .component(
    'submissionAttemptView',
    react2angular(SubmissionAttemptView, ['attempt', 'isGrading'], [])
  );
