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

import React, { useMemo } from 'react';

import { HtmlWithMathJax } from '../../components/HtmlWithMathjax';
import { grade as gradeFormat, makeGradeDisplayMethods } from '../../filters/pure/grade.ts';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { FeedbackFileList } from '../../assignmentFeedback/directives/feedbackFileList.tsx';
import { RubricGridView } from './rubricGrid/rubricGridView.tsx';

export interface ViewCompositeGradeProps {
  grade: {
    rubric?: { sections: any[] };
    feedback?: string;
    isBlankGrade?: boolean;
    feedbackManager?: { files?: any[] };
    [key: string]: any;
  };
}

/**
 * React port of the `viewCompositeGrade` directive — the post-grading display for an
 * essay/submission: the rubric grid, the overall grade (with the `coloredGradient`
 * `colored-grade done-N` class folded in, computed from the pure grade function),
 * and the instructor feedback card with its attachments. Renders the now-React
 * `RubricGridView` + `FeedbackFileList`; bridged via react2angular so essay's
 * `<view-composite-grade>` renders unchanged. DOM preserved (`.view-composite-grade`,
 * `.colored-grade.done-N`, `.instructor-feedback`).
 */
export const ViewCompositeGrade: React.FC<ViewCompositeGradeProps> = ({ grade }) => {
  const translate = useTranslation();
  const gradeMethods = useMemo(() => makeGradeDisplayMethods(translate), [translate]);

  const colorPercent = Math.min(100, (gradeFormat(gradeMethods, grade, 'color') as number) || 0);
  const gradeText = gradeFormat(gradeMethods, grade, 'percentThenPoints') as React.ReactNode;
  const formattedFeedback = (grade.feedback || '').replace(/\n/gi, '<br/>');
  const hasFeedback = !!grade.feedback || !!grade.feedbackManager?.files?.length;

  return (
    <div className="view-composite-grade">
      {grade.rubric && <RubricGridView rubric={grade.rubric} />}

      {!grade.isBlankGrade && (
        <div
          className="h2 my-3 alert alert-light flex-center-center"
          title={translate('ASSIGNMENT_GRADE_DESCRIPTION')}
        >
          <span className={`colored-grade done-${colorPercent}`}>{gradeText}</span>
        </div>
      )}

      {hasFeedback && (
        <div className="card my-3">
          <div className="card-header">{translate('ASSIGNMENT_INSTRUCTOR_FEEDBACK')}</div>
          <div className="card-body">
            <span className="instructor-feedback">
              <HtmlWithMathJax html={formattedFeedback} />
            </span>
          </div>
          <FeedbackFileList files={grade.feedbackManager?.files} />
        </div>
      )}
    </div>
  );
};
