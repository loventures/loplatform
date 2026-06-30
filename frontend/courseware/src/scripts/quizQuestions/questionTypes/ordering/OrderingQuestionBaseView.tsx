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

import React, { useLayoutEffect, useRef, useState } from 'react';

import { HtmlWithMathJax } from '../../../components/HtmlWithMathjax';
import { choiceOrdinal } from '../../../filters/choiceOrdinalFilter.js';
import { useTranslation } from '../../../i18n/translationContext.tsx';
import { SELECTION_TYPE_ORDERING } from '../../../utilities/questionTypes.js';
import { BasicQuestionTemplate } from '../../questionTemplates/BasicQuestionTemplate.tsx';
import { QuestionDistractorRow } from '../../questionTemplates/QuestionDistractorRow.tsx';
import { QuestionResultModeToggle } from '../../questionAddons/questionResultModeToggle.tsx';
import {
  DisplayChoice,
  getDisplayAnswer,
  getDisplayResponse,
  reorder,
  responseToSelection,
} from './orderingChoices.ts';

interface Props {
  index: number;
  focusOnRender?: boolean;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: any;
  response?: any;
  changeAnswer: (index: number, response: any) => void;
  canEditAnswer?: boolean;
}

/**
 * The interactive (editable) ordering view: a reorderable list driven by per-row
 * up/down buttons (the accessible path) and native HTML5 drag. The order lives in
 * `response.selection.order` and round-trips through `changeAnswer`, so the list
 * is fully controlled. (The Angular version used jQuery-UI `sortable` + absolute
 * positioning; react reorders in normal flow, so that plumbing is dropped — see
 * the matching SASS change. We avoid react-dnd to dodge a second HTML5 backend.)
 */
const OrderingPlay: React.FC<Omit<Props, 'assessment' | 'questionCount' | 'canEditAnswer'>> = ({
  index,
  focusOnRender,
  question,
  response,
  changeAnswer,
}) => {
  const translate = useTranslation();
  const displayChoices = responseToSelection(question, response);
  const ordered = [...displayChoices].sort((a, b) => a.displayIndex - b.displayIndex);

  const [dragFrom, setDragFrom] = useState<number | null>(null);
  const buttonRefs = useRef<Record<string, HTMLButtonElement | null>>({});
  const refocus = useRef<{ originalIndex: number; dir: 'up' | 'down' } | null>(null);
  const didInitialFocus = useRef(false);

  useLayoutEffect(() => {
    if (refocus.current) {
      buttonRefs.current[`${refocus.current.originalIndex}-${refocus.current.dir}`]?.focus();
      refocus.current = null;
    } else if (focusOnRender && !didInitialFocus.current && ordered.length) {
      buttonRefs.current[`${ordered[0].originalIndex}-down`]?.focus();
      didInitialFocus.current = true;
    }
  });

  const moveTo = (choice: DisplayChoice, to: number, dir: 'up' | 'down') => {
    const from = choice.displayIndex;
    if (to < 0 || to >= displayChoices.length) return;
    if (dir === 'up') refocus.current = { originalIndex: choice.originalIndex, dir: 'up' };
    const order = reorder(displayChoices, from, to);
    const resp = response || {};
    const selection = (response && response.selection) || { responseType: SELECTION_TYPE_ORDERING };
    changeAnswer(index, { ...resp, selection: { ...selection, order } });
  };

  const onDrop = (targetDisplayIndex: number) => {
    if (dragFrom != null && dragFrom !== targetDisplayIndex) {
      const order = reorder(displayChoices, dragFrom, targetDisplayIndex);
      const resp = response || {};
      const selection = (response && response.selection) || { responseType: SELECTION_TYPE_ORDERING };
      changeAnswer(index, { ...resp, selection: { ...selection, order } });
    }
    setDragFrom(null);
  };

  return (
    <ol className="question-distractor-list">
      {ordered.map(choice => {
        const di = choice.displayIndex;
        const last = di + 1 >= displayChoices.length;
        return (
          <li
            key={choice.originalIndex}
            className="question-distractor-row ordering-distractor-row"
            original-index={String(choice.originalIndex)}
            display-index={String(di)}
            draggable
            onDragStart={() => setDragFrom(di)}
            onDragOver={e => e.preventDefault()}
            onDrop={() => onDrop(di)}
          >
            <div className={choice.selected ? 'question-choice-content active' : 'question-choice-content'}>
              <div className="choice-ordinal">{choiceOrdinal(di) as React.ReactNode}</div>
              <div
                className="choice-text"
                id={`choice-text-${choice.originalIndex}`}
              >
                <HtmlWithMathJax html={choice.text} />
              </div>
              <div className="ordering-controls">
                <button
                  ref={el => {
                    buttonRefs.current[`${choice.originalIndex}-up`] = el;
                  }}
                  className="btn btn-sm btn-outline-primary"
                  disabled={di === 0}
                  onClick={() => moveTo(choice, di - 1, 'up')}
                  title={translate('ORDERING_QUESTION_MOVE_UP')}
                >
                  <span className="sr-only">
                    {di > 0
                      ? translate('ORDERING_QUESTION_MOVE_UP', { from: di, to: di - 1 })
                      : translate('ORDERING_QUESTION_MOVE_UP_DISABLED')}
                  </span>
                  <i className="icon-chevron-up" />
                </button>
                <button
                  ref={el => {
                    buttonRefs.current[`${choice.originalIndex}-down`] = el;
                  }}
                  className="btn btn-sm btn-outline-primary"
                  disabled={last}
                  onClick={() => moveTo(choice, di + 1, 'down')}
                  title={translate('ORDERING_QUESTION_MOVE_DOWN')}
                >
                  <span className="sr-only">
                    {!last
                      ? translate('ORDERING_QUESTION_MOVE_DOWN', { from: di, to: di + 1 })
                      : translate('ORDERING_QUESTION_MOVE_DOWN_DISABLED')}
                  </span>
                  <i className="icon-chevron-down" />
                </button>
              </div>
            </div>
          </li>
        );
      })}
    </ol>
  );
};

/** The read-only (review) ordering view: the learner's order or the correct order. */
const OrderingResults: React.FC<{ question: any; response?: any }> = ({ question, response }) => {
  const translate = useTranslation();
  // $onInit: with a response show the submitted order, otherwise show the answer.
  const [showingAnswer, setShowingAnswer] = useState<boolean>(() => !response);
  const displayChoices = showingAnswer
    ? getDisplayAnswer(question)
    : getDisplayResponse(question, response);

  return (
    <>
      <ol className="question-distractor-list">
        {displayChoices.map((choice, i) => (
          <li
            key={i}
            className="question-distractor-row"
          >
            <QuestionDistractorRow
              choice={choice as any}
              hasCorrectness={question.displayDetail?.correctAnswer}
              isCorrect={choice.showAsCorrect}
              index={i}
            >
              <div className="question-choice-content disabled">
                <div className="choice-ordinal">{choiceOrdinal(i) as React.ReactNode}</div>
                <div className="choice-text">
                  <HtmlWithMathJax html={choice.text} />
                </div>
              </div>
            </QuestionDistractorRow>
          </li>
        ))}
      </ol>

      {response && question.displayDetail?.correctAnswer && (
        <QuestionResultModeToggle
          showResponse={() => setShowingAnswer(false)}
          showAnswer={() => setShowingAnswer(true)}
          translate={translate}
        />
      )}
    </>
  );
};

/**
 * React port of `orderingQuestionBaseView` (B2-quiz): renders the hub with the
 * interactive play view when editable, or the read-only results view otherwise.
 * DOM preserved from the templates so selectors hold (`.question-distractor-list`,
 * `li.question-distractor-row.ordering-distractor-row`, `.question-choice-content`,
 * `.choice-ordinal`, `.choice-text`, `.ordering-controls`). The print view stays
 * Angular.
 */
export const OrderingQuestionBaseView: React.FC<Props> = ({
  index,
  focusOnRender,
  assessment,
  questionCount,
  question,
  response,
  changeAnswer,
  canEditAnswer,
}) => (
  <BasicQuestionTemplate
    className="question choice-question ordering-question"
    index={index}
    assessment={assessment}
    questionCount={questionCount}
    question={question}
    response={response}
  >
    {canEditAnswer ? (
      <OrderingPlay
        index={index}
        focusOnRender={focusOnRender}
        question={question}
        response={response}
        changeAnswer={changeAnswer}
      />
    ) : (
      <OrderingResults
        question={question}
        response={response}
      />
    )}
  </BasicQuestionTemplate>
);
