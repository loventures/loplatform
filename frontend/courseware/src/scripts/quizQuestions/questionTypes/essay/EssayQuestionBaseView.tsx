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

import React, { useEffect, useMemo, useReducer, useRef, useState } from 'react';

import { FeedbackFileList } from '../../../assignmentFeedback/directives/feedbackFileList.tsx';
import { FeedbackTools } from '../../../assignmentFeedback/directives/feedbackTools.tsx';
import { RubricGridView } from '../../../assignmentGrade/directives/rubricGrid/rubricGridView.tsx';
import { ViewCompositeGrade } from '../../../assignmentGrade/directives/viewCompositeGrade.tsx';
import { HtmlWithMathJax } from '../../../components/HtmlWithMathjax';
import { RichTextEditor } from '../../../contentEditor/directives/richTextEditor.tsx';
import { useTranslation } from '../../../i18n/translationContext.tsx';
import { FeedbackManager } from '../../../assignmentFeedback/FeedbackManager.js';
import { ViewQuestionSubmissionAttemptGrade } from '../../../assignmentGrade/models/pure/questionSubmissionAttemptGrade.ts';
import { ViewRubric } from '../../../assignmentGrade/models/pure/rubric.ts';
import { RESPONSE_SUBMITTED } from '../../../utilities/attemptStates.js';
import { SELECTION_TYPE_ESSAY } from '../../../utilities/questionTypes.js';
import { BasicQuestionTemplate } from '../../questionTemplates/BasicQuestionTemplate.tsx';

interface EssayResponse {
  state?: string;
  selection?: { responseType?: string; response?: string };
  attachments?: any[];
  uploads?: any[];
}

export interface EssayQuestionBaseViewProps {
  index: number;
  focusOnRender?: boolean;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: any;
  response?: EssayResponse;
  score?: any;
  changeAnswer: (index: number, response: any) => void;
  canEditAnswer?: boolean;
}

const FILE_POLL_MS = 250;

/**
 * React port of the learner `essayQuestionBaseView` (+ `essayQuestionBaseViewPlay`) — the essay
 * answering UI (editable: rich-text editor + feedback tools) and the read-only results view (response
 * text + attachments), wrapped in the React BasicQuestionTemplate hub, plus the pending-grade alert,
 * rubric preview, and composite grade card. Renders via the React `QuestionView` dispatcher (and the
 * Angular `<essay-question-base-view>` kebab in essayQuestion.html). The essay **grading** view
 * (instructor) and **print** view stay Angular (essayQuestion.js / questionLoader), as do the
 * `FeedbackManager` data model (still Angular, passed to the already-React deps); the pure
 * `ViewRubric` / `ViewQuestionSubmissionAttemptGrade` models are imported directly.
 *
 * DOM preserved for EssayGradingTest (QuizQuestions page object): `.question.essay-question`,
 * `.alert-primary` + `#assignment-grade-pending`, `.essay-question-play`, `.essay-response-text`,
 * `.card`/`.card-body`, `.alert-danger`.
 */
export const EssayQuestionBaseView: React.FC<EssayQuestionBaseViewProps> = ({
  index,
  focusOnRender,
  assessment,
  questionCount,
  question,
  response,
  changeAnswer,
  canEditAnswer,
}) => {
  const translate = useTranslation();

  // The essay HTML — seeded from the response, re-synced when the response prop changes. The
  // RichTextEditor (CKEditor) ignores content syncs while focused, so the changeAnswer round-trip
  // does not clobber the live editor.
  const [textSelection, setTextSelection] = useState<string>(response?.selection?.response ?? '');
  useEffect(() => setTextSelection(response?.selection?.response ?? ''), [response]);

  // Angular data models stay Angular (resolved from the injector), mutated in place / read by the deps.
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

  // Mirror the Angular ctrl: create the grade model once, on the first truthy response, never recreate.
  const gradeRef = useRef<any>(null);
  if (response && !gradeRef.current) {
    gradeRef.current = new (ViewQuestionSubmissionAttemptGrade as any)({ question, response });
  }
  const grade = gradeRef.current;

  const isPendingGrade = response?.state === RESPONSE_SUBMITTED;

  const selectionToResponse = (
    html: string = textSelection ?? '',
    attachments: any[] = feedbackManager.getAttachedFiles(),
    uploads: any[] = feedbackManager.getFileInfoInStaging()
  ) => {
    const selection = response?.selection ?? { responseType: SELECTION_TYPE_ESSAY };
    return { ...response, attachments, uploads, selection: { ...selection, response: html } };
  };

  const updateResponseText = (html: string) => changeAnswer(index, selectionToResponse(html));
  const updateUploads = () => changeAnswer(index, selectionToResponse());

  // The Angular ctrl's `$scope.$watch` on `feedbackManager.files.length`: FeedbackTools (React) stages
  // files into the mutable feedbackManager outside React's knowledge, and uploads settle async. Poll
  // the settled file count and push a response update when it changes (skipping the initial reading and
  // any in-progress state) — the digest-emulation pattern used elsewhere for mutable Angular models.
  const lastFileSig = useRef<string | null>(null);
  useEffect(() => {
    if (!canEditAnswer) return;
    const id = window.setInterval(() => {
      const sig = feedbackManager.hasInProgressFiles() ? '-1' : String(feedbackManager.files.length);
      if (lastFileSig.current === null) {
        lastFileSig.current = sig; // initial reading — "filters out the first change"
        return;
      }
      if (sig !== lastFileSig.current) {
        lastFileSig.current = sig;
        if (sig !== '-1') updateUploads();
      }
    }, FILE_POLL_MS);
    return () => window.clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [feedbackManager, canEditAnswer]);

  // Commit staged file removals on unmount (the Angular `$onDestroy`).
  useEffect(
    () => () => feedbackManager?.commitRemovingFiles?.(),
    [feedbackManager]
  );

  // FeedbackFileList reads the mutable feedbackManager.files; re-render when read-only attachments change.
  const [, forceRender] = useReducer((c: number) => c + 1, 0);
  useEffect(() => {
    if (canEditAnswer) return;
    const len = feedbackManager.files.length;
    const id = window.setInterval(() => {
      if (feedbackManager.files.length !== len) forceRender();
    }, FILE_POLL_MS);
    return () => window.clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [feedbackManager, canEditAnswer]);

  return (
    <BasicQuestionTemplate
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
        <>
          {canEditAnswer ? (
            <div className="essay-question-play mt-4">
              <RichTextEditor
                content={textSelection}
                onChange={updateResponseText}
                label="ESSAY_QUESTION_LABEL"
                focusOnRender={focusOnRender}
              />
              <FeedbackTools feedbackManager={feedbackManager} />
            </div>
          ) : (
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
        </>
      )}
    </BasicQuestionTemplate>
  );
};
