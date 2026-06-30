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

import { compact, each, filter, find, first, invert, isEmpty, isNil, map, mapValues, pick, range, some } from 'lodash';

import { SELECTION_TYPE_MATCHING } from '../../../utilities/questionTypes.js';

// The matching data + interaction transforms from the AngularJS matchingQuestion
// controller, extracted as pure functions (B2-quiz). The React play view renders
// the computed rows (term + matched def per row) as flex rows — visually the same
// as the Angular two-column absolute-positioned layout, without the JS height math.

export const buildChoiceId = (question: any, index: number, prefix: string) =>
  `${question.id}-${prefix}-${index}`;

export interface Term {
  id: string;
  text?: string;
  termIndex: number;
  rowIndex?: number;
  matchedDef?: Def | null;
}
export interface Def {
  id: string;
  text?: string;
  defIndex: number;
  rowIndex?: number;
  matchedTerm?: Term | null;
}
export interface Row {
  rowIndex: number;
  term?: Term | null;
  def?: Def | null;
  isMatched?: boolean;
}

const createQuestionView = (question: any): { terms: Term[]; defs: Def[]; rowCount: number } => {
  const terms: Term[] = map(question.terms, (term: any, termIndex: number) => ({
    ...term,
    id: term.id || buildChoiceId(question, termIndex, 'term'),
    termIndex,
  }));
  const defs: Def[] = map(question.definitions, (def: any, defIndex: number) => ({
    ...def,
    id: def.id || buildChoiceId(question, defIndex, 'def'),
    defIndex,
  }));
  return { terms, defs, rowCount: Math.max(question.terms.length, question.definitions.length) };
};

/** The play rows: each row pairs a term with its matched (or filler) definition. */
export const buildDisplay = (question: any, response: any): { rows: Row[]; terms: Term[]; defs: Def[] } => {
  const { terms, defs, rowCount } = createQuestionView(question);

  const termToDefMatches: Record<string, number> =
    response && response.selection
      ? mapValues(response.selection.elementIndexesByGroupIndex, (defs: any) => first(defs) as number)
      : {};
  const defToTermMatches: Record<string, string> = invert(termToDefMatches) as any;

  each(terms, term => {
    const matchedDefIndex = termToDefMatches[term.termIndex];
    term.matchedDef = isNil(matchedDefIndex) ? null : defs[matchedDefIndex];
  });
  each(defs, def => {
    const matchedTermIndex = defToTermMatches[def.defIndex];
    def.matchedTerm = isNil(matchedTermIndex) ? null : terms[+matchedTermIndex];
  });

  const rows: Row[] = map(range(0, rowCount), rowIndex => ({ rowIndex }));
  each(rows, row => {
    row.term = terms[row.rowIndex];
    if (row.term) row.term.rowIndex = row.rowIndex;
    row.def = row.term ? row.term.matchedDef : null;
    if (row.def) row.def.rowIndex = row.rowIndex;
    row.isMatched = !!(row.term && row.def);
  });

  const unmatchedDefs = filter(defs, def => !def.matchedTerm);
  each(rows, row => {
    if (!row.isMatched && !row.def && unmatchedDefs.length) {
      row.def = unmatchedDefs.shift()!;
      row.def.rowIndex = row.rowIndex;
    }
  });

  return { rows, terms, defs };
};

/** The current term→def matches as elementIndexesByGroupIndex. */
export const buildMatchesFromRows = (rows: Row[]): Record<number, number[]> => {
  const matches: Record<number, number[]> = {};
  each(rows, row => {
    if (row.term) matches[row.term.termIndex] = row.isMatched ? [row.def!.defIndex] : [];
  });
  return matches;
};

/** Toggle a term↔def match (unmatch if already matched; steal the def from its old term). */
export const toggleMatch = (matches: Record<number, number[]>, term: Term, def: Def): Record<number, number[]> => {
  const next: Record<number, number[]> = {};
  Object.keys(matches).forEach(k => {
    next[+k] = [...matches[+k]];
  });
  if (next[term.termIndex] && next[term.termIndex][0] === def.defIndex) {
    next[term.termIndex] = [];
  } else {
    if (def.matchedTerm) next[def.matchedTerm.termIndex] = [];
    next[term.termIndex] = [def.defIndex];
  }
  return next;
};

export const selectionToResponse = (response: any, matches: Record<number, number[]>) => {
  const resp = response || {};
  const hasMatches = some(matches, m => !isEmpty(m));
  const selection = hasMatches
    ? { ...(resp.selection || {}), responseType: SELECTION_TYPE_MATCHING, elementIndexesByGroupIndex: matches }
    : null;
  return { ...resp, selection };
};

const getTermMapping = (question: any): Record<string, string> => {
  if (question.termMapping) return question.termMapping;
  if (question.correctDefinitionForTerm) {
    const termMapping: Record<string, string> = {};
    each(question.correctDefinitionForTerm, (defIndex: number, termIndex: number) => {
      termMapping[buildChoiceId(question, termIndex, 'term')] = buildChoiceId(question, defIndex, 'def');
    });
    return termMapping;
  }
  return {};
};

export interface ResultRow {
  rowIndex: number;
  term?: { id: string; text?: string };
  def?: { id: string; text?: string };
  isMatched: boolean;
  isCorrect?: boolean;
}

/** The learner's submitted rows, flagged correct/incorrect. */
export const createResponseRows = (question: any, rows: Row[]): ResultRow[] => {
  const termMapping = getTermMapping(question);
  const defMapping = invert(termMapping);
  return map(rows, ({ rowIndex, term, def, isMatched }) => {
    const responseRow: ResultRow = {
      rowIndex,
      term: term ? pick(term, ['id', 'text']) : undefined,
      def: def ? pick(def, ['id', 'text']) : undefined,
      isMatched: !!isMatched,
    };
    if (term && def) responseRow.isCorrect = isMatched && termMapping[term.id] === def.id;
    else if (term && !def) responseRow.isCorrect = !termMapping[term.id];
    else if (!term && def) responseRow.isCorrect = !(defMapping as any)[def.id];
    return responseRow;
  });
};

/** The correct matching (review "show answer"). */
export const createCorrectAnswerRows = (question: any, rows: Row[], defs: Def[]): ResultRow[] => {
  const termMapping = getTermMapping(question);
  const correctAnswerRows = compact(
    map(rows, ({ rowIndex, term }) => {
      const def = term && find(defs, { id: termMapping[term.id] });
      return {
        rowIndex,
        term: term ? pick(term, ['id', 'text']) : undefined,
        def: def ? pick(def, ['id', 'text']) : undefined,
        isCorrect: true,
        isMatched: !!(term && def),
      } as ResultRow;
    })
  );

  const defMapping = invert(termMapping);
  const unmatchedDefs = filter(defs, def => !(defMapping as any)[def.id]);
  each(correctAnswerRows, row => {
    if (!row.def && unmatchedDefs.length) {
      row.def = pick(unmatchedDefs.pop(), ['id', 'text']) as any;
    }
  });
  return correctAnswerRows;
};
