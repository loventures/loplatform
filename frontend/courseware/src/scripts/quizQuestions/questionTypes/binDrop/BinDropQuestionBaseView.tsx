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

import React, { useState } from 'react';

import { useTranslation } from '../../../i18n/translationContext.tsx';
import { BasicQuestionTemplate } from '../../questionTemplates/BasicQuestionTemplate.tsx';
import {
  computeDisplayBins,
  getChoices,
  moveChoiceSelected,
  responseToSelection,
  selectionToResponse,
} from './binDropChoices.ts';

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
 * The interactive bin-drop view: choices live in a pool (`unselected-bin`) and the
 * bins, and are placed two ways — click-to-place (click a choice to activate it,
 * then click a bin) and native HTML5 drag. Both funnel through `moveChoice`, which
 * rebuilds `selection.elementIndexesByGroupIndex` and round-trips via `changeAnswer`,
 * so the layout is fully controlled. (The Angular version used jQuery-UI draggable/
 * droppable; native drag avoids a second HTML5 backend. NB: the Selenide `associate`
 * helper drives a *mouse* drag, which native HTML5 DnD ignores — but that suite is
 * `@ignore`d; the click-to-place path is the robust one.)
 */
const BinDropPlay: React.FC<Omit<Props, 'assessment' | 'questionCount' | 'canEditAnswer'>> = ({
  index,
  focusOnRender,
  question,
  response,
  changeAnswer,
}) => {
  const translate = useTranslation();
  const choices = getChoices(question);
  const selection = responseToSelection(question, response);
  const [activeChoiceIndex, setActiveChoiceIndex] = useState<number | null>(null);
  const [dragChoice, setDragChoice] = useState<number | null>(null);

  const moveChoice = (choiceIndex: number, binIndex: number) => {
    const next = moveChoiceSelected(selection.selected, choiceIndex, binIndex);
    changeAnswer(index, selectionToResponse(response, next));
    setActiveChoiceIndex(null);
  };
  const activateChoice = (choiceIndex: number) =>
    setActiveChoiceIndex(prev => (prev === choiceIndex ? null : choiceIndex));
  const dropToBin = (binIndex: number) => {
    if (activeChoiceIndex != null) moveChoice(activeChoiceIndex, binIndex);
  };
  const onDropBin = (binIndex: number) => {
    if (dragChoice != null) moveChoice(dragChoice, binIndex);
    setDragChoice(null);
  };

  const renderChoice = (choiceIndex: number) => (
    <li key={choiceIndex}>
      <div
        className={activeChoiceIndex === choiceIndex ? 'bin-drop-choice play active' : 'bin-drop-choice play'}
        choice-index={String(choiceIndex)}
        draggable
        autoFocus={!!focusOnRender && choiceIndex === 0}
        onDragStart={() => setDragChoice(choiceIndex)}
        onClick={e => {
          e.stopPropagation();
          activateChoice(choiceIndex);
        }}
      >
        {choices[choiceIndex].text}
      </div>
    </li>
  );

  return (
    <>
      <ul className="bin-list bin-list-play">
        {(question.bins ?? []).map((bin: any, binIndex: number) => (
          <li
            key={binIndex}
            className="bin-drop-bin play-bin"
            bin-index={String(binIndex)}
            title={translate('BIN_DROP_BIN_INSTRUCTIONS')}
            onClick={() => dropToBin(binIndex)}
            onDragOver={e => e.preventDefault()}
            onDrop={() => onDropBin(binIndex)}
          >
            <div className="bin-header">{bin.text}</div>
            <ul className="bin-choice-list list-unstyled">
              {(selection.selected[binIndex] ?? []).map(renderChoice)}
            </ul>
          </li>
        ))}
      </ul>

      <div
        className="bin-drop-bin unselected-bin"
        bin-index="-1"
        onClick={() => dropToBin(-1)}
        onDragOver={e => e.preventDefault()}
        onDrop={() => onDropBin(-1)}
      >
        <div className="bin-label sr-only">{translate('BIN_DROP_CHOICES_INSTRUCTIONS')}</div>
        <ul className="bin-choice-list list-unstyled">{selection.unselected.map(renderChoice)}</ul>
      </div>
    </>
  );
};

/** The read-only bin-drop review: placed choices (correct/incorrect) + missing. */
const BinDropResults: React.FC<{ question: any; response?: any }> = ({ question, response }) => {
  const translate = useTranslation();
  const { displayBins, choicesWithoutBin } = computeDisplayBins(question, response);

  return (
    <>
      <ul className="bin-list bin-list-results">
        {displayBins.map(bin => (
          <li
            key={bin.id}
            className="bin-drop-bin results-bin"
          >
            <div className="bin-header">{bin.text}</div>
            <ul className="bin-choice-list list-unstyled">
              {bin.selected.map((choice: any) => (
                <li key={choice.id}>
                  <div
                    className={
                      'bin-drop-choice' +
                      (choice.correct ? ' correct' : '') +
                      (choice.incorrect ? ' incorrect' : '')
                    }
                  >
                    {choice.correct && (
                      <span
                        className="bindrop-choice-correctness icon icon-checkmark bg-success"
                        title={translate('BINDROP_CHOICE_CORRECT')}
                      />
                    )}
                    {choice.incorrect && (
                      <span
                        className="bindrop-choice-correctness icon icon-cross bg-danger"
                        title={translate('BINDROP_CHOICE_INCORRECT')}
                      />
                    )}
                    <span>{choice.text}</span>
                  </div>
                </li>
              ))}
            </ul>

            {!!bin.missing.length && !response && (
              <div className="bin-label">{translate('BIN_DROP_RESULT_MISSING')}</div>
            )}
            {!!bin.missing.length && (
              <ul className="list-unstyled">
                {bin.missing.map((choice: any) => (
                  <li key={choice.id}>
                    <div className="bin-drop-choice missing">
                      <span>{choice.text}</span>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>

      {!!choicesWithoutBin.length && (
        <div className="bin-drop-bin binless-bin">
          <div className="bin-label">{translate('BIN_DROP_CHOICES_WITHOUT_BIN')}</div>
          <ul className="bin-choice-list list-unstyled">
            {choicesWithoutBin.map((choice: any) => (
              <li key={choice.id}>
                <div className="bin-drop-choice missing">{choice.text}</div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  );
};

/**
 * React port of `binDropQuestionBaseView` (B2-quiz): the hub with the interactive
 * play view when editable, or the read-only results view otherwise. DOM preserved
 * from the templates (`.bin-list`, `.bin-drop-bin`, `.unselected-bin`,
 * `.bin-choice-list`, `.bin-drop-choice`, `.bin-header`). Print view stays Angular.
 */
export const BinDropQuestionBaseView: React.FC<Props> = ({
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
    className="question bindrop-question"
    index={index}
    assessment={assessment}
    questionCount={questionCount}
    question={question}
    response={response}
  >
    {canEditAnswer ? (
      <BinDropPlay
        index={index}
        focusOnRender={focusOnRender}
        question={question}
        response={response}
        changeAnswer={changeAnswer}
      />
    ) : (
      <BinDropResults
        question={question}
        response={response}
      />
    )}
  </BasicQuestionTemplate>
);
