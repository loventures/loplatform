/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
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

import { CreditType } from '../../utilities/creditTypes';
import { createAction, isActionOf } from 'typesafe-actions';

export type HideChild = { id: string; childId: string; hidden: boolean };
export const hideChild = createAction('HideChild')<HideChild>();

export type Move = { id: string; childId: string; newPosition: number };
export const move = createAction('Move')<Move>();

export type Rename = { id: string; name: string };
export const rename = createAction('Rename')<Rename>();

export type Reinstruct = { id: string; instructions: string };
export const reinstruct = createAction('Reinstruct')<Reinstruct>();

export type ChangeDueDay = {
  id: string;
  year: number;
  month: number;
  date: number;
};
export const changeDueDay = createAction('ChangeDueDay')<ChangeDueDay>();
export type ChangeDueTime = { id: string; hours: number; minutes: number };
export const changeDueTime = createAction('ChangeDueTime')<ChangeDueTime>();

export type RemoveDueDate = { id: string };
export const removeDueDate = createAction('RemoveDueDate')<RemoveDueDate>();

export type SetForCredit = { id: string; type: CreditType };
export const setForCredit = createAction('SetForCredit')<SetForCredit>();

export type ChangePointsPossible = { id: string; newPointsPossible: number };
export const changePointsPossible = createAction('ChangePointsPossible')<ChangePointsPossible>();

// TODO: this is quite a bit of boilerplate to pattern matching... how to generalize this?

type Match<E, Z> = (e: E) => Z;

type Matchers<Z> = {
  rename: Match<Rename, Z>;
  reinstruct: Match<Reinstruct, Z>;
  setForCredit: Match<SetForCredit, Z>;
  hideChild: Match<HideChild, Z>;
  changePointsPossible: Match<ChangePointsPossible, Z>;
  changeDueDay: Match<ChangeDueDay, Z>;
  changeDueTime: Match<ChangeDueTime, Z>;
  removeDueDate: Match<RemoveDueDate, Z>;
  move: Match<Move, Z>;
};

function assertNever(x: never): never {
  throw new Error('Unexpected object: ' + x);
}

type OptionalMatchers<Z> = Partial<Matchers<Z>> & { def: () => Z };

export function match<Z>(edit: ContentEdit, matchers: Matchers<Z>): Z {
  if (isActionOf(rename, edit)) {
    return matchers.rename(edit.payload);
  } else if (isActionOf(reinstruct, edit)) {
    return matchers.reinstruct(edit.payload);
  } else if (isActionOf(setForCredit, edit)) {
    return matchers.setForCredit(edit.payload);
  } else if (isActionOf(changePointsPossible, edit)) {
    return matchers.changePointsPossible(edit.payload);
  } else if (isActionOf(changeDueDay, edit)) {
    return matchers.changeDueDay(edit.payload);
  } else if (isActionOf(changeDueTime, edit)) {
    return matchers.changeDueTime(edit.payload);
  } else if (isActionOf(removeDueDate, edit)) {
    return matchers.removeDueDate(edit.payload);
  } else if (isActionOf(hideChild, edit)) {
    return matchers.hideChild(edit.payload);
  } else if (isActionOf(move, edit)) {
    return matchers.move(edit.payload);
  }
  return assertNever(edit);
}

export function matchC<Z>(matchers: Matchers<Z>): (edit: ContentEdit) => Z {
  return edit => match(edit, matchers);
}

export type ContentEdit =
  | ReturnType<typeof hideChild>
  | ReturnType<typeof move>
  | ReturnType<typeof rename>
  | ReturnType<typeof reinstruct>
  | ReturnType<typeof changeDueDay>
  | ReturnType<typeof changeDueTime>
  | ReturnType<typeof removeDueDate>
  | ReturnType<typeof setForCredit>
  | ReturnType<typeof changePointsPossible>;

export function matchDef<Z>(edit: ContentEdit, matchers: OptionalMatchers<Z>): Z {
  return match(edit, {
    rename: e => (matchers.rename ? matchers.rename(e) : matchers.def()),
    reinstruct: e => (matchers.reinstruct ? matchers.reinstruct(e) : matchers.def()),
    setForCredit: e => (matchers.setForCredit ? matchers.setForCredit(e) : matchers.def()),
    changePointsPossible: e =>
      matchers.changePointsPossible ? matchers.changePointsPossible(e) : matchers.def(),
    changeDueDay: e => (matchers.changeDueDay ? matchers.changeDueDay(e) : matchers.def()),
    changeDueTime: e => (matchers.changeDueTime ? matchers.changeDueTime(e) : matchers.def()),
    removeDueDate: e => (matchers.removeDueDate ? matchers.removeDueDate(e) : matchers.def()),
    hideChild: e => (matchers.hideChild ? matchers.hideChild(e) : matchers.def()),
    move: e => (matchers.move ? matchers.move(e) : matchers.def()),
  });
}
