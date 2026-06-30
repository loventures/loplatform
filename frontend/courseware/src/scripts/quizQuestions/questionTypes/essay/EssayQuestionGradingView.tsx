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

import { FeedbackFileList } from '../../../assignmentFeedback/directives/feedbackFileList.tsx';
import { HtmlWithMathJax } from '../../../components/HtmlWithMathjax';
import { useTranslation } from '../../../i18n/translationContext.tsx';
import { FeedbackManager } from '../../../assignmentFeedback/FeedbackManager.js';
import { GradingQuestionTemplate } from '../../questionTemplates/GradingQuestionTemplate.tsx';

export interface EssayQuestionGradingViewProps {
  index: number;
  question: any;
  response?: { selection?: { response?: string }; attachments?: any[]; uploads?: any[] };
}

/**
 * React port of the instructor `essayQuestionGradingView` (B2-quiz grading) — the read-only essay
 * response shown in the grader (the React `GradingQuestionTemplate` chrome around the response text +
 * attachments, or the empty GRADING_ESSAY_NO_RESPONSE alert). The FeedbackManager model stays Angular
 * (lojector). DOM preserved from essayQuestionGradingView.html (`.question.essay-question`, `.card`,
 * `.card-body.essay-response-text`, `.alert-danger`). Rendered via the grader's `<question-loader
 * grading>` → `<essay-question-grading-view>` kebab (replacing the former Angular component).
 */
export const EssayQuestionGradingView: React.FC<EssayQuestionGradingViewProps> = ({
  index,
  question,
  response,
}) => {
  const translate = useTranslation();
  const textSelection = response?.selection?.response ?? '';
  const feedbackManager = useMemo(
    () =>
      new FeedbackManager([
        ...(response?.attachments ?? []),
        ...(response?.uploads ?? []),
      ]),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    []
  );

  return (
    <GradingQuestionTemplate
      index={index}
      question={question}
    >
      {response && (
        <div>
          <hr />
          <div className="card">
            {!!textSelection && (
              <div className="card-body essay-response-text">
                <HtmlWithMathJax html={textSelection} />
              </div>
            )}

            {!!feedbackManager.files.length && <FeedbackFileList files={feedbackManager.files} />}

            {!textSelection && !feedbackManager.files.length && (
              <div className="card-body essay-response-text alert-danger">
                {translate('GRADING_ESSAY_NO_RESPONSE')}
              </div>
            )}
          </div>
        </div>
      )}
    </GradingQuestionTemplate>
  );
};
