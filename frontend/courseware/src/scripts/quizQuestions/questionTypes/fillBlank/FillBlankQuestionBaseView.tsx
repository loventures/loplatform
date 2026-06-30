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

import { useTranslation } from '../../../i18n/translationContext.tsx';
import { SELECTION_TYPE_FILL_BLANK } from '../../../utilities/questionTypes.js';
import { BasicQuestionTemplate } from '../../questionTemplates/BasicQuestionTemplate.tsx';
import { QuestionResultModeToggle } from '../../questionAddons/questionResultModeToggle.tsx';

export interface Blank {
  startIndex?: number;
  endIndex?: number;
  offset?: number;
  answers?: string[];
}
interface Question {
  questionText: string;
  blanks?: Blank[];
  displayDetail?: { correctAnswer?: boolean };
}
interface Response {
  selection?: { entries?: string[] } | null;
}

export interface FillBlankQuestionBaseViewProps {
  index: number;
  focusOnRender?: boolean;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: Question;
  response?: Response;
  changeAnswer: (index: number, response: any) => void;
  canEditAnswer?: boolean;
}

export const responseToEntries = (response?: { selection?: { entries?: string[] } | null }): string[] => [
  ...(response?.selection?.entries ?? []),
];

export type Segment =
  | { kind: 'text'; html: string; trailing?: boolean }
  | { kind: 'blank'; idx: number; blank: Blank };

// Mirror the Angular buildQuestionText: interleave question-text slices with the
// blanks at their offsets (the trailing slice is appended unwrapped).
export const buildSegments = (question: {
  questionText: string;
  blanks?: Blank[];
}): Segment[] => {
  const segments: Segment[] = [];
  let nextIndex = 0;
  (question.blanks ?? []).forEach((blank, idx) => {
    const startIndex = blank.startIndex || blank.offset || 0;
    segments.push({ kind: 'text', html: question.questionText.slice(nextIndex, startIndex) });
    segments.push({ kind: 'blank', idx, blank });
    nextIndex = blank.endIndex! + 1 || blank.offset || 0;
  });
  segments.push({ kind: 'text', html: question.questionText.slice(nextIndex), trailing: true });
  return segments;
};

/**
 * React port of the learner `fillBlankQuestionBaseView` (B2-quiz). The question
 * text is built as an HTML string with embedded `ng-model` blank inputs and
 * rendered via the `compile` directive — the reason fill-blank was excluded from
 * B1's compile retirement. Here the text slices are plain HTML (preserved, no
 * MathJax change) and the blanks are controlled React inputs, so the `compile`
 * directive is retired for this type.
 *
 * DOM preserved from fillBlankQuestionBaseView.html: the `.question-text` prompt
 * with `.fill-blank-text` slices and `.fill-blank-blank > input.form-control`
 * (answering) / `.fill-blank-answers > .blank-candidate` (review), plus the
 * already-React `<question-result-mode-toggle>` in the content slot.
 */
export const FillBlankQuestionBaseView: React.FC<FillBlankQuestionBaseViewProps> = ({
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

  const [selection, setSelection] = useState<string[]>(() => responseToEntries(response));
  useEffect(() => {
    if (response) setSelection(responseToEntries(response));
  }, [response]);

  // $onInit: with a response show the learner's entries, otherwise show answers.
  const [showCorrect, setShowCorrect] = useState<boolean>(() => !response);

  const segments = buildSegments(question);

  const inputChanged = (idx: number, value: string) => {
    const entries = responseToEntries(response);
    entries[idx] = value;
    setSelection(entries);
    const resp: any = response || {};
    changeAnswer(index, {
      ...resp,
      selection: {
        ...(resp.selection || {}),
        responseType: SELECTION_TYPE_FILL_BLANK,
        entries,
      },
    });
  };

  const questionText = (
    <div className="question-text">
      {segments.map((seg, i) => {
        if (seg.kind === 'text') {
          return seg.trailing ? (
            <span
              key={i}
              dangerouslySetInnerHTML={{ __html: seg.html }}
            />
          ) : (
            <div
              key={i}
              className="fill-blank-text"
              dangerouslySetInnerHTML={{ __html: seg.html }}
            />
          );
        }
        const { idx, blank } = seg;
        return (
          <div
            key={i}
            className="fill-blank-blank"
          >
            {!showCorrect && (
              <input
                className="form-control"
                aria-label={translate('FILL_BLANK_BLANK_LABEL')}
                autoFocus={idx === 0 && !!focusOnRender}
                value={selection[idx] ?? ''}
                disabled={!canEditAnswer}
                onChange={e => inputChanged(idx, e.target.value)}
              />
            )}
            {!canEditAnswer && showCorrect && (
              <div className="fill-blank-answers">
                <span>{translate('FILL_BLANK_CORRECT_ANSWER_LABEL')}</span>
                {(blank.answers ?? []).map((candidate, ci) => (
                  <span
                    key={ci}
                    className="blank-candidate"
                  >
                    {candidate}
                  </span>
                ))}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );

  return (
    <BasicQuestionTemplate
      className="question fill-blank-question"
      index={index}
      assessment={assessment}
      questionCount={questionCount}
      question={question as any}
      response={response as any}
      questionTextSlot={questionText}
    >
      {!canEditAnswer && response && question.displayDetail?.correctAnswer && (
        <QuestionResultModeToggle
          showResponse={() => setShowCorrect(false)}
          showAnswer={() => setShowCorrect(true)}
          translate={translate}
        />
      )}
    </BasicQuestionTemplate>
  );
};
