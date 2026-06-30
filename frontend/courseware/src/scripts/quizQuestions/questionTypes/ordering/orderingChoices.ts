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

import { find, get, isEmpty, isObject, map, orderBy, range } from 'lodash';

// The order-math from the AngularJS orderingQuestion controller, extracted as
// pure functions (B2-quiz) so the React ordering views — and their tests — share
// exactly the same transforms.

export interface OrderingChoice {
  text?: any;
  [k: string]: any;
}
export interface DisplayChoice extends OrderingChoice {
  text: string;
  originalIndex: number;
  displayIndex: number;
  selected?: boolean;
}
export interface ResultChoice extends OrderingChoice {
  text: string;
  correctIndex?: number;
  answerIndex?: number;
  showAsCorrect: boolean;
}

export const getChoices = (question: any): OrderingChoice[] =>
  question.renderOrderChoices ? question.renderOrderChoices : question.choices;

export const getChoiceText = (choice: OrderingChoice): string =>
  isObject(choice.text) ? (choice.text as any).html : choice.text;

/** The correct order (review "show answer"). */
export const getDisplayAnswer = (question: any): ResultChoice[] => {
  if (isEmpty(question.correctOrder)) return [];
  const choices = getChoices(question);
  const transformed = map(choices, (choice, originalIndex) => ({
    ...choice,
    text: getChoiceText(choice),
    correctIndex: question.correctOrder[originalIndex],
    showAsCorrect: true,
  }));
  return orderBy(transformed, 'correctIndex') as ResultChoice[];
};

/** The order array (display index → original index), defaulting to identity. */
export const getDisplayOrder = (question: any, response: any): number[] => {
  const order = get(response, 'selection.order', []);
  const choices = getChoices(question);
  if (isEmpty(order)) return range(0, choices.length);
  return response.selection.order;
};

/** The interactive (play) choices, each tagged with its current displayIndex. */
export const responseToSelection = (
  question: any,
  response: any,
  displayedChoices?: DisplayChoice[]
): DisplayChoice[] => {
  const order = getDisplayOrder(question, response);
  const choices = getChoices(question);
  return map(choices, (choice, originalIndex) => {
    const displayIndex = order.indexOf(originalIndex);
    const displayedChoice = find(displayedChoices, { originalIndex });
    return {
      ...choice,
      text: getChoiceText(choice),
      selected: get(displayedChoice, 'selected', false),
      originalIndex,
      displayIndex,
    };
  }) as DisplayChoice[];
};

/** The learner's submitted order (review "show response"), flagged correct/incorrect. */
export const getDisplayResponse = (question: any, response: any): ResultChoice[] => {
  if (isEmpty(question.correctOrder)) return [];
  const choices = getChoices(question);
  const transformed = map(choices, (choice, originalIndex) => {
    // selection.order holds, per original index, the display slot the choice landed in.
    const answerIndex = response?.selection?.order.indexOf(originalIndex) ?? originalIndex;
    const correctIndex = question.correctOrder[originalIndex];
    return {
      ...choice,
      text: getChoiceText(choice),
      correctIndex,
      answerIndex,
      showAsCorrect: correctIndex === answerIndex,
    };
  });
  return orderBy(transformed, 'answerIndex') as ResultChoice[];
};

/** Move the choice at display slot `from` to slot `to`, returning the new order array. */
export const reorder = (displayChoices: DisplayChoice[], from: number, to: number): number[] => {
  const order = map(orderBy(displayChoices, 'displayIndex'), 'originalIndex') as number[];
  const [choiceIndex] = order.splice(from, 1);
  order.splice(to, 0, choiceIndex);
  return order;
};
