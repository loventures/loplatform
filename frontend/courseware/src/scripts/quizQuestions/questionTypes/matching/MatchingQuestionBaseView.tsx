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

import { isEmpty } from 'lodash';
import React, { useState } from 'react';

import { useTranslation } from '../../../i18n/translationContext.tsx';
import { BasicQuestionTemplate } from '../../questionTemplates/BasicQuestionTemplate.tsx';
import {
  buildDisplay,
  buildMatchesFromRows,
  createCorrectAnswerRows,
  createResponseRows,
  Def,
  selectionToResponse,
  Term,
  toggleMatch,
} from './matchingChoices.ts';

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

const correctnessCheck = (
  <svg aria-hidden="true" stroke="currentColor" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">
    <path fill="none" strokeLinecap="round" strokeLinejoin="round" strokeWidth="32" d="M416 128L192 384l-96-96" />
  </svg>
);
const correctnessCross = (
  <svg aria-hidden="true" stroke="currentColor" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">
    <path fill="none" strokeLinecap="round" strokeLinejoin="round" strokeWidth="32" d="M368 368L144 144m224 0L144 368" />
  </svg>
);

/**
 * The interactive matching view: terms (left) are matched to definitions (right)
 * by clicking a term and a definition. The match map lives in
 * `selection.elementIndexesByGroupIndex` and round-trips via `changeAnswer`, so it
 * is fully controlled. The Angular version laid out two absolutely-positioned
 * columns (jQuery height math for the slide animation); React renders the computed
 * rows (term + matched def) as flex rows for the same aligned result.
 */
const MatchingPlay: React.FC<Omit<Props, 'assessment' | 'questionCount' | 'canEditAnswer'>> = ({
  index,
  question,
  response,
  changeAnswer,
}) => {
  const translate = useTranslation();
  const { rows } = buildDisplay(question, response);
  const [activeTerm, setActiveTerm] = useState<Term | null>(null);
  const [activeDef, setActiveDef] = useState<Def | null>(null);

  const doMatch = (term: Term, def: Def) => {
    const next = toggleMatch(buildMatchesFromRows(rows), term, def);
    changeAnswer(index, selectionToResponse(response, next));
    setActiveTerm(null);
    setActiveDef(null);
  };
  const onTerm = (term: Term) => {
    if (activeTerm && activeTerm.termIndex === term.termIndex) return setActiveTerm(null);
    if (activeDef) doMatch(term, activeDef);
    else setActiveTerm(term);
  };
  const onDef = (def: Def) => {
    if (activeDef && activeDef.defIndex === def.defIndex) return setActiveDef(null);
    if (activeTerm) doMatch(activeTerm, def);
    else setActiveDef(def);
  };

  const itemClass = (base: string, active: boolean, matched: boolean) =>
    `match-item ${base}${active ? ' active' : ''}${matched ? ' matched' : ''}`;

  return (
    <div className="matching-question-play">
      {rows.map(row => (
        <div
          className="match-play-row"
          key={row.rowIndex}
        >
          <div
            className={itemClass('match-term', activeTerm?.termIndex === row.term?.termIndex, !!row.term?.matchedDef)}
            id={row.term ? `match-term-${row.term.id}` : undefined}
            role={row.term ? 'button' : undefined}
            tabIndex={row.term ? 0 : undefined}
            aria-label={
              row.term
                ? translate('MATCHING_QUESTION_TERM_LABEL', {
                    text: row.term.text,
                    isMatched: !!row.term.matchedDef,
                    matchedToText: row.term.matchedDef ? row.term.matchedDef.text : '',
                  })
                : undefined
            }
            onClick={() => row.term && onTerm(row.term)}
          >
            {row.term && <div className="match-item-content btn">{row.term.text}</div>}
          </div>

          <div className={row.isMatched ? 'matching-line matched' : 'matching-line'} />

          <div
            className={itemClass('match-definition', activeDef?.defIndex === row.def?.defIndex, !!row.def?.matchedTerm)}
            id={row.def ? `match-def-${row.def.id}` : undefined}
            role={row.def ? 'button' : undefined}
            tabIndex={row.def ? 0 : undefined}
            aria-label={
              row.def
                ? translate('MATCHING_QUESTION_DEF_LABEL', {
                    text: row.def.text,
                    isMatched: !!row.def.matchedTerm,
                    matchedToText: row.def.matchedTerm ? row.def.matchedTerm.text : '',
                  })
                : undefined
            }
            onClick={() => row.def && onDef(row.def)}
          >
            {row.def && <div className="match-item-content btn">{row.def.text}</div>}
          </div>
        </div>
      ))}
    </div>
  );
};

/** The read-only matching review: the learner's (or correct) rows with marks. */
const MatchingResults: React.FC<{ question: any; response?: any }> = ({ question, response }) => {
  const translate = useTranslation();
  const { rows, defs } = buildDisplay(question, response);
  const responseRows = createResponseRows(question, rows);
  const correctRows = createCorrectAnswerRows(question, rows, defs);
  const hasScore = response ? !isEmpty(response.score) : false;
  const displayRows = response ? responseRows : correctRows;

  return (
    <ul className="matching-results-list list-unstyled">
      {displayRows.map(row => (
        <li key={row.rowIndex}>
          <div
            className={
              'match-results-line' +
              (row.isMatched ? ' matched' : '') +
              (hasScore && row.isCorrect ? ' correct' : '') +
              (hasScore && !row.isCorrect ? ' incorrect' : '')
            }
          >
            {hasScore && (
              <span
                className="correctness"
                aria-label={translate(row.isCorrect ? 'MATCH_CORRECT' : 'MATCH_INCORRECT')}
              >
                {row.isCorrect ? correctnessCheck : correctnessCross}
              </span>
            )}
            <div className="match-item">
              {row.term && <div className="match-item-content">{row.term.text}</div>}
            </div>
            <div
              className="matching-line"
              aria-hidden="true"
            />
            <div className="match-item">
              {row.def && <div className="match-item-content">{row.def.text}</div>}
            </div>
          </div>

          {hasScore && !row.isCorrect && question.displayDetail?.correctAnswer && (
            <div className="match-results-line answer">
              <div className="flex-grow-1" />
              <div className="me-2 alert alert-info arrow-right mb-0 py-2">{translate('MATCH_CORRECT_ANSWER')}</div>
              <div className="match-item">
                {row.def && correctRows[row.rowIndex]?.def && (
                  <div className="match-item-content">{correctRows[row.rowIndex].def!.text}</div>
                )}
              </div>
            </div>
          )}
        </li>
      ))}
    </ul>
  );
};

/**
 * React port of `matchingQuestionBaseView` (B2-quiz): the hub with the interactive
 * play view when editable, or the read-only results view otherwise. Print view
 * stays Angular.
 */
export const MatchingQuestionBaseView: React.FC<Props> = ({
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
    className="question matching-question"
    index={index}
    assessment={assessment}
    questionCount={questionCount}
    question={question}
    response={response}
  >
    {canEditAnswer ? (
      <MatchingPlay
        index={index}
        focusOnRender={focusOnRender}
        question={question}
        response={response}
        changeAnswer={changeAnswer}
      />
    ) : (
      <MatchingResults
        question={question}
        response={response}
      />
    )}
  </BasicQuestionTemplate>
);
