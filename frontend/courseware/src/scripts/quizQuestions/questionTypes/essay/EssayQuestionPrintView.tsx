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

import React, { useMemo, useRef } from 'react';

import { FeedbackFileList } from '../../../assignmentFeedback/directives/feedbackFileList.tsx';
import { RubricGridView } from '../../../assignmentGrade/directives/rubricGrid/rubricGridView.tsx';
import { ViewCompositeGrade } from '../../../assignmentGrade/directives/viewCompositeGrade.tsx';
import { HtmlWithMathJax } from '../../../components/HtmlWithMathjax';
import { useTranslation } from '../../../i18n/translationContext.tsx';
import { FeedbackManager } from '../../../assignmentFeedback/FeedbackManager.js';
import { ViewQuestionSubmissionAttemptGrade } from '../../../assignmentGrade/models/pure/questionSubmissionAttemptGrade.ts';
import { ViewRubric } from '../../../assignmentGrade/models/pure/rubric.ts';
import { RESPONSE_SUBMITTED } from '../../../utilities/attemptStates.js';
import { PrintQuestionTemplate } from '../../questionTemplates/PrintQuestionTemplate.tsx';

export interface EssayQuestionPrintViewProps {
  index: number;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: any;
  response?: { state?: string; selection?: { response?: string }; attachments?: any[]; uploads?: any[] };
}

/**
 * React port of the learner `essayQuestionPrintView` (B2-quiz print) — read-only essay: the pending-grade
 * alert, rubric preview / composite grade, then the response text and attachments (or the empty
 * ESSAY_NO_RESPONSE alert), wrapped in the React PrintQuestionTemplate hub. Mirrors the read-only branch
 * of essayQuestionBaseViewPlay (print is never editable, so no editor / feedback tools / file watching).
 * The FeedbackManager stays Angular; the pure ViewRubric / ViewQuestionSubmissionAttemptGrade grade
 * models are imported directly. DOM preserved for the print
 * frame (`.question.essay-question`, `.alert-primary` + `#assignment-grade-pending`, `.essay-response-text`).
 */
export const EssayQuestionPrintView: React.FC<EssayQuestionPrintViewProps> = ({
  index,
  assessment,
  questionCount,
  question,
  response,
}) => {
  const translate = useTranslation();

  const textSelection = response?.selection?.response ?? '';
  const rubric = useMemo(
    () => (question.rubric ? new (ViewRubric as any)(question.rubric) : null),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    []
  );
  const feedbackManager = useMemo(
    () =>
      new FeedbackManager([
        ...(response?.attachments ?? []),
        ...(response?.uploads ?? []),
      ]),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    []
  );
  const gradeRef = useRef<any>(null);
  if (response && !gradeRef.current) {
    gradeRef.current = new (ViewQuestionSubmissionAttemptGrade as any)({ question, response });
  }
  const grade = gradeRef.current;
  const isPendingGrade = response?.state === RESPONSE_SUBMITTED;

  return (
    <PrintQuestionTemplate
      className="question essay-question"
      index={index}
      assessment={assessment}
      questionCount={questionCount}
      question={question}
      response={response as any}
    >
      {isPendingGrade && (
        <div className="alert alert-primary d-flex align-items-center mt-3">
          <i
            role="presentation"
            className="material-icons"
          >
            pending_actions
          </i>
          <span id="assignment-grade-pending">{translate('ASSIGNMENT_GRADE_PENDING')}</span>
        </div>
      )}

      {rubric && !grade && !isPendingGrade && <RubricGridView rubric={rubric} />}

      {grade && <ViewCompositeGrade grade={grade} />}

      {response && (
        <div className="card mt-4">
          {!!textSelection && (
            <div className="card-body essay-response-text">
              <HtmlWithMathJax html={textSelection} />
            </div>
          )}

          {!!feedbackManager.files.length && <FeedbackFileList files={feedbackManager.files} />}

          {!textSelection && !feedbackManager.files.length && (
            <div className="card-body essay-response-text alert-danger">{translate('ESSAY_NO_RESPONSE')}</div>
          )}
        </div>
      )}
    </PrintQuestionTemplate>
  );
};
