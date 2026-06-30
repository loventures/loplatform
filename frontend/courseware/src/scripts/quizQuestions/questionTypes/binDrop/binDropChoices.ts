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

import { filter, find, flatMap, isEmpty, keyBy, map, range, some } from 'lodash';

import { SELECTION_TYPE_BIN_DROP } from '../../../utilities/questionTypes.js';

// The bin-drop data + interaction transforms from the AngularJS binDropQuestion
// controller, extracted as pure functions (B2-quiz) so the React views and their
// tests share exactly the same logic.

export const buildId = (question: any, index: number, prefix: string) =>
  `${question.id}-${prefix}-${index}`;

export const getChoices = (question: any): any[] =>
  question.choices
    ? question.choices
    : map(question.binOptions, (binOption: any, optionIndex: number) => ({
        ...binOption,
        id: buildId(question, optionIndex, 'choice'),
      }));

export const getBinMapping = (question: any): Record<string, string[]> => {
  if (question.binMapping) return question.binMapping;
  const binMapping: Record<string, string[]> = {};
  (question.bins ?? []).forEach((bin: any, binIndex: number) => {
    binMapping[buildId(question, binIndex, 'bin')] = map(bin.correctOptionIndices, (choiceIndex: number) =>
      buildId(question, choiceIndex, 'choice')
    );
  });
  return binMapping;
};

export interface PlaySelection {
  selected: Record<number, number[]>; // binIndex → choiceIndexes
  unselected: number[];
}

/** The interactive selection (which choices are in which bin, and the pool). */
export const responseToSelection = (question: any, response: any): PlaySelection => {
  const choiceCount = getChoices(question).length;
  const byBin = response?.selection?.elementIndexesByGroupIndex;
  if (!byBin) {
    return { selected: {}, unselected: range(0, choiceCount) };
  }
  return {
    selected: { ...byBin },
    unselected: filter(range(0, choiceCount), choiceIndex =>
      Object.values(byBin).every((selectedForBin: any) => (selectedForBin ?? []).indexOf(choiceIndex) === -1)
    ),
  };
};

/** Remove a choice from every bin, then drop it into `binIndex` (-1 = back to the pool). */
export const moveChoiceSelected = (
  selected: Record<number, number[]>,
  choiceIndex: number,
  binIndex: number
): Record<number, number[]> => {
  const next: Record<number, number[]> = {};
  Object.keys(selected).forEach(k => {
    next[+k] = (selected[+k] ?? []).filter(ci => ci !== choiceIndex);
  });
  if (binIndex !== -1) {
    next[binIndex] = [...(next[binIndex] ?? []), choiceIndex];
  }
  return next;
};

/** Fold the selection back into a response (a null selection when nothing is binned). */
export const selectionToResponse = (response: any, selected: Record<number, number[]>) => {
  const resp = response || {};
  const hasSelected = some(selected, bin => !isEmpty(bin));
  const selection = hasSelected
    ? { ...(resp.selection || {}), responseType: SELECTION_TYPE_BIN_DROP, elementIndexesByGroupIndex: selected }
    : null;
  return { ...resp, selection };
};

export interface DisplayBin {
  id: string;
  text: string;
  selected: any[];
  missing: any[];
}

/** The review view: each bin's placed choices (flagged correct/incorrect) + missing ones. */
export const computeDisplayBins = (question: any, response: any) => {
  const hasCorrectness = question.displayDetail?.correctAnswer;
  const hasScore = response ? !isEmpty(response.score) : false;
  const binMapping = getBinMapping(question);
  const choices = getChoices(question);

  const displayBins: DisplayBin[] = map(question.bins, (bin: any, binIndex: number) => {
    const binId = bin.id || buildId(question, binIndex, 'bin');
    const correctChoices = hasCorrectness
      ? map(binMapping[binId], (choiceId: string) => find(choices, { id: choiceId }))
      : [];
    const correctMap = keyBy(correctChoices, 'id');

    const selectedChoices =
      !response || !response.selection
        ? []
        : map(response.selection.elementIndexesByGroupIndex[binIndex], (choiceIndex: number) => {
            const choice = choices[choiceIndex];
            return {
              ...choice,
              correct: hasScore && !!correctMap[choice.id],
              incorrect: hasScore && !correctMap[choice.id],
            };
          });
    const selectedMap = keyBy(selectedChoices, 'id');

    return {
      id: binId,
      text: bin.text,
      selected: selectedChoices,
      missing: hasCorrectness ? filter(correctChoices, (choice: any) => !selectedMap[choice.id]) : [],
    };
  });

  const choicesWithBin = flatMap(binMapping);
  const choicesWithoutBin =
    response || hasCorrectness
      ? filter(choices, (choice: any) => choicesWithBin.indexOf(choice.id) === -1)
      : choices;

  return { hasCorrectness, hasScore, displayBins, choicesWithoutBin };
};
