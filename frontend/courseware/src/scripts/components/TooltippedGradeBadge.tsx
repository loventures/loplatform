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

import { CourseState, useCourseSelector } from '../loRedux';
import { uniqueId } from 'lodash';
import { selectSubmissionActivity } from '../contentPlayerComponents/activityViews/submission/redux/submissionActivitySelectors';
import DueDateBadge from '../contentPlayerComponents/parts/DueDateBadge';
import GradeBadge from '../directives/GradeBadge';
import { selectQuizActivityData } from '../courseActivityModule/selectors/quizActivitySelectors';
import {
  isContentWithRelationships,
  selectContent,
} from '../courseContentModule/selectors/contentEntrySelectors';
import selectShowContentHeaderGrade from '../courseContentModule/selectors/showContentHeaderGradeSelector';
import { Translate, useTranslation } from '../i18n/translationContext';
import { selectQuiz } from '../selectors/quizSelectors';
import { selectCurrentUserRubricScores } from '../selectors/rubricScoreSelectors';
import { isDiscussion, isQuiz, isSubmission } from '../utilities/contentTypes';
import React, { useRef } from 'react';
import { UncontrolledTooltip as Tooltip } from 'reactstrap';

const selectCompleted = (s: CourseState) => {
  const content = selectContent(s);
  if (isDiscussion(content)) {
    const rubricScores = selectCurrentUserRubricScores(s);
    return !!rubricScores[content.id];
  } else if (isSubmission(content)) {
    const submission = selectSubmissionActivity(s);
    return !!submission?.hasSubmittedAttempts;
  } else if (isQuiz(content)) {
    const quiz = selectQuizActivityData(s);
    return !!quiz?.hasSubmittedAttempts;
  } else {
    return false;
  }
};

const TooltippedGradeBadge: React.FC = () => {
  const translate = useTranslation();
  const id = useRef(uniqueId('grade-badge-with-tooltip'));
  const content = useCourseSelector(selectContent);
  const quiz = useCourseSelector(selectQuiz);
  const showGrade = useCourseSelector(selectShowContentHeaderGrade) && !quiz?.settings.isCheckpoint;
  const { grade } = content;

  const tooltipText = getTooltipText(
    quiz?.settings.maxAttempts,
    quiz?.settings.gradingPolicy,
    translate
  );

  const completed = useCourseSelector(selectCompleted);

  return isContentWithRelationships(content) && (content.dueDate || showGrade) ? (
    <div className="d-flex justify-content-between align-items-center mb-4">
      {content.dueDate ? (
        <DueDateBadge
          date={content.dueDate}
          completed={completed}
          exempt={content.dueDateExempt}
        />
      ) : (
        <div />
      )}
      {quiz?.settings.gradingPolicy ? (
        <div className="grade-badge-with-tooltip h3">
          <div id={id.current}>
            {grade ? (
              <GradeBadge
                grade={grade}
                percent="half"
                isPending={!Number.isFinite(content?.grade?.pointsAwarded)}
              />
            ) : (
              <span
                role="presentation"
                className="material-icons pending-grade"
              >
                pending_actions
              </span>
            )}
          </div>
          <Tooltip
            className="tooltip-info"
            placement="left"
            target={id.current}
          >
            <span className="grade-badge-tooltip-text">{tooltipText}</span>
          </Tooltip>
        </div>
      ) : (
        <div className="h3">
          <GradeBadge
            grade={grade}
            percent="half"
            className="grade-badge-without-tooltip"
          />
        </div>
      )}
    </div>
  ) : null;
};

const getTooltipText = (
  maxAttempts: number | null | undefined,
  gradingPolicy: string | undefined,
  translate: Translate
) => {
  return maxAttempts === 1
    ? translate('ASSIGNMENT_GRADE_CALCULATION_SINGLE_ATTEMPT')
    : translate(`ASSIGNMENT_GRADE_CALCULATION_${gradingPolicy}`);
};

export default TooltippedGradeBadge;
