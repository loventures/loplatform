/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import React, { useEffect, useState } from 'react';

import { TranslationProvider } from '../../../i18n/translationContext.tsx';
import { useCourseSelector } from '../../../loRedux';
import { QueryClientProvider, queryClient } from '../../../resources/queryClient';
import { quizAPI } from '../../../services/quizAPI.ts';
import { submissionActivityAPI } from '../../../services/submissionActivityAPI.ts';
import { isSubmission } from '../../../utilities/contentTypes';
import { withNgReduxProvider } from '../../../utilities/ngReduxProvider.jsx';
import { gotoLink } from '../../../utilities/routingUtils';
import { InstructorDashboardPageLink } from '../../../utils/pageLinks';
import { AssessmentGrader } from '../../../assignmentGrader/assessmentGrader/AssessmentGrader.tsx';
import { SubmissionGrader } from '../../../assignmentGrader/submissionGrader/SubmissionGrader.tsx';
import { selectPageContentItem } from '../selectors';

/**
 * The instructor grader entry — native React (B3, retiring the last grader `angular2react` bridge). Loads
 * the assignment for the routed content (`QuizAPI.loadQuiz` / `SubmissionActivityAPI.loadSubmissionAssessment`
 * via lojector), then renders the React quiz grader (`AssessmentGrader`) or submission/observation grader
 * (`SubmissionGrader`); exit returns to the instructor dashboard. Rendered by the React `ERGraderPage`.
 */
const GraderEntryInner: React.FC = () => {
  const content = useCourseSelector(selectPageContentItem) as any;
  const [assignment, setAssignment] = useState<any>(null);

  const submission = isSubmission(content);

  useEffect(() => {
    if (!content) return;
    const promise = submission
      ? submissionActivityAPI.loadSubmissionAssessment(content.contentId)
      : quizAPI.loadQuiz(content.contentId);
    promise.then((loaded: any) => {
      loaded.dueDate = content.dueDate;
      setAssignment(loaded);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [content]);

  const returnToDashboard = () => gotoLink(InstructorDashboardPageLink.toLink());

  if (!assignment) return null;

  return submission ? (
    <SubmissionGrader
      assignment={assignment}
      onExit={returnToDashboard}
    />
  ) : (
    <AssessmentGrader
      assignmentId={assignment.contentId}
      onExit={returnToDashboard}
    />
  );
};

// The grader views (and their leaves) need the redux / query / i18n providers the old react2angular
// grader bridges supplied; provide them here now that the views are rendered as plain React components.
export const GraderEntry = withNgReduxProvider(() => (
  <QueryClientProvider client={queryClient}>
    <TranslationProvider>
      <GraderEntryInner />
    </TranslationProvider>
  </QueryClientProvider>
));

export default GraderEntry;
