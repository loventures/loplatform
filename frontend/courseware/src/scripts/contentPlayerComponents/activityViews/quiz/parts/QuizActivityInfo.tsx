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

import { QuizAssessment, QuizAttempt } from '../../../../api/quizApi.ts';
import { ContentWithNebulousDetails } from '../../../../courseContentModule/selectors/contentEntry';
import React from 'react';

import ActivityInfo from '../../../parts/ActivityInfo.tsx';

// see selectQuizActivityData
export type QuizActivity = {
  assessment: QuizAssessment;
  orderedAttempts: QuizAttempt[];
  latestAttempt?: QuizAttempt;
  isLatestAttemptOpen: boolean;
  latestAttemptCompetencyBreakdown: any;
  openAttempt?: QuizAttempt;
  hasSubmittedAttempts: boolean;
  attemptsRemaining: number;
  attemptNumber: number;
  unlimitedAttempts: boolean;
  canPlayAttempt: boolean;
  latestSubmittedAttempt?: QuizAttempt;
  isLatestSubmittedAttemptFinalized: boolean;
};

const QuizActivityInfo: React.FC<{
  content: ContentWithNebulousDetails;
  quiz: QuizActivity;
  noAttemptNumber?: boolean;
}> = ({ content, quiz, noAttemptNumber = false }) => (
  <ActivityInfo
    content={content}
    gradeCalculationType={quiz.assessment.settings.gradingPolicy}
    maximumAttempts={quiz.assessment.settings.maxAttempts}
    attemptNumber={noAttemptNumber ? undefined : quiz.attemptNumber}
    maxMinutes={quiz.assessment.settings.maxMinutes}
    testsOut={quiz.assessment.settings.testsOut}
  />
);

export default QuizActivityInfo;
