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

import React from 'react';

import { useTranslation } from '../../../i18n/translationContext.tsx';
import { PrintQuestionTemplate } from '../../questionTemplates/PrintQuestionTemplate.tsx';
import { computeDisplayBins } from './binDropChoices.ts';

export interface BinDropQuestionPrintViewProps {
  index: number;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: any;
  response?: any;
  canEditAnswer?: boolean;
}

/**
 * React port of the learner `binDropQuestionPrintView` (+ `binDropQuestionPrintViewResults`, folded in)
 * — B2-quiz print. Read-only: each bin lists its selected choices (with checkmark/cross once correctness
 * is shown) then its missing correct choices, plus any choices left unbinned. Shares the review
 * computation (`computeDisplayBins`) with the base view. DOM preserved from the print templates
 * (`.print-bin-drop`, `.bin`/`.bin-header`, `.choice-correctness.icon-checkmark/.icon-cross`,
 * `.choices-without-bin`). No Selenide print coverage for binDrop — verified via vitest + build.
 */
export const BinDropQuestionPrintView: React.FC<BinDropQuestionPrintViewProps> = ({
  index,
  assessment,
  questionCount,
  question,
  response,
  canEditAnswer,
}) => {
  const translate = useTranslation();
  const { displayBins, choicesWithoutBin } = computeDisplayBins(question, response);

  return (
    <PrintQuestionTemplate
      className="question bindrop-question"
      index={index}
      assessment={assessment}
      questionCount={questionCount}
      question={question}
      response={response}
    >
      <div className="print-bin-drop">
        {displayBins.map((bin: any) => (
          <div
            className="bin"
            key={bin.id}
          >
            <div className="bin-header">{bin.text}</div>
            {bin.selected.map((choice: any) => (
              <div
                className="p-2 mb-2 text-center word-break-all border border-dark rounded position-relative"
                key={choice.id}
              >
                {!canEditAnswer && choice.correct && <span className="choice-correctness icon icon-checkmark" />}
                {!canEditAnswer && choice.incorrect && <span className="choice-correctness icon icon-cross" />}
                <span>{choice.text}</span>
              </div>
            ))}
            {bin.missing.map((choice: any) => (
              <div
                className="p-2 mb-2 text-center word-break-all border rounded position-relative"
                key={choice.id}
              >
                <span>{choice.text}</span>
              </div>
            ))}
          </div>
        ))}

        {choicesWithoutBin.length > 0 && (
          <div className="bin choices-without-bin">
            {!canEditAnswer && <div className="bin-header">{translate('BIN_DROP_CHOICES_WITHOUT_BIN')}</div>}
            {choicesWithoutBin.map((choice: any) => (
              <div
                className="d-inline-block p-2 mb-2 me-1 text-center word-break-all border border-dark rounded position-relative"
                key={choice.id}
              >
                <span>{choice.text}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </PrintQuestionTemplate>
  );
};
