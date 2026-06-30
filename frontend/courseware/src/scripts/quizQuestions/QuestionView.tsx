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

import {
  QUESTION_TYPE_BIN_DROP,
  QUESTION_TYPE_ESSAY,
  QUESTION_TYPE_FILL_BLANK,
  QUESTION_TYPE_HOTSPOT,
  QUESTION_TYPE_LEGACY_BIN_DROP,
  QUESTION_TYPE_LEGACY_ESSAY,
  QUESTION_TYPE_LEGACY_FILL_BLANK,
  QUESTION_TYPE_LEGACY_HOTSPOT,
  QUESTION_TYPE_LEGACY_MATCHING,
  QUESTION_TYPE_LEGACY_MULTIPLE_CHOICE,
  QUESTION_TYPE_LEGACY_MULTIPLE_SELECT,
  QUESTION_TYPE_LEGACY_ORDERING,
  QUESTION_TYPE_LEGACY_TRUE_FALSE,
  QUESTION_TYPE_MATCHING,
  QUESTION_TYPE_MULTIPLE_CHOICE,
  QUESTION_TYPE_MULTIPLE_SELECT,
  QUESTION_TYPE_ORDERING,
  QUESTION_TYPE_TRUE_FALSE,
} from '../utilities/questionTypes.js';
import { BinDropQuestionBaseView } from './questionTypes/binDrop/BinDropQuestionBaseView.tsx';
import { BinDropQuestionPrintView } from './questionTypes/binDrop/BinDropQuestionPrintView.tsx';
import { EssayQuestionBaseView } from './questionTypes/essay/EssayQuestionBaseView.tsx';
import { EssayQuestionPrintView } from './questionTypes/essay/EssayQuestionPrintView.tsx';
import { FillBlankQuestionBaseView } from './questionTypes/fillBlank/FillBlankQuestionBaseView.tsx';
import { FillBlankQuestionPrintView } from './questionTypes/fillBlank/FillBlankQuestionPrintView.tsx';
import { HotspotQuestionBaseView } from './questionTypes/hotspot/HotspotQuestionBaseView.tsx';
import { HotspotQuestionPrintView } from './questionTypes/hotspot/HotspotQuestionPrintView.tsx';
import { MatchingQuestionBaseView } from './questionTypes/matching/MatchingQuestionBaseView.tsx';
import { MultipleChoiceQuestionBaseView } from './questionTypes/multipleChoice/MultipleChoiceQuestionBaseView.tsx';
import { MultipleChoiceQuestionPrintView } from './questionTypes/multipleChoice/MultipleChoiceQuestionPrintView.tsx';
import { MultipleSelectQuestionBaseView } from './questionTypes/multipleSelect/MultipleSelectQuestionBaseView.tsx';
import { MultipleSelectQuestionPrintView } from './questionTypes/multipleSelect/MultipleSelectQuestionPrintView.tsx';
import { OrderingQuestionPrintView } from './questionTypes/ordering/OrderingQuestionPrintView.tsx';
import { OrderingQuestionBaseView } from './questionTypes/ordering/OrderingQuestionBaseView.tsx';
import { EssayQuestionGradingView } from './questionTypes/essay/EssayQuestionGradingView.tsx';

export interface QuestionViewProps {
  index: number;
  focusOnRender?: boolean;
  assessment?: any;
  questionCount?: number;
  question: any;
  response?: any;
  changeAnswer?: (index: number, response: any) => void;
  canEditAnswer?: boolean;
  grading?: boolean;
}

const noop = () => {};

/**
 * The question-type dispatcher (replaces the Angular `questionLoader`): `question._type` → the native
 * React view, by render context:
 *   - `window.inPrintMode` → `PRINT_VIEW_BY_TYPE` (print views; matching reuses its base view).
 *   - `grading` → `GRADING_VIEW_BY_TYPE` (only essay has a distinct grading view; every other type grades
 *     via its base view read-only, `canEditAnswer=false`).
 *   - otherwise → `VIEW_BY_TYPE` (the learner base/results views).
 * Used by the React quiz shells (learner) and the React `AssessmentGrader` (instructor grading). Unknown
 * types render null — `questionLoader` is fully retired.
 */
const VIEW_BY_TYPE: Record<string, React.FC<any>> = {
  [QUESTION_TYPE_MULTIPLE_CHOICE]: MultipleChoiceQuestionBaseView,
  [QUESTION_TYPE_LEGACY_MULTIPLE_CHOICE]: MultipleChoiceQuestionBaseView,
  [QUESTION_TYPE_TRUE_FALSE]: MultipleChoiceQuestionBaseView,
  [QUESTION_TYPE_LEGACY_TRUE_FALSE]: MultipleChoiceQuestionBaseView,
  [QUESTION_TYPE_MULTIPLE_SELECT]: MultipleSelectQuestionBaseView,
  [QUESTION_TYPE_LEGACY_MULTIPLE_SELECT]: MultipleSelectQuestionBaseView,
  [QUESTION_TYPE_ORDERING]: OrderingQuestionBaseView,
  [QUESTION_TYPE_LEGACY_ORDERING]: OrderingQuestionBaseView,
  [QUESTION_TYPE_BIN_DROP]: BinDropQuestionBaseView,
  [QUESTION_TYPE_LEGACY_BIN_DROP]: BinDropQuestionBaseView,
  [QUESTION_TYPE_MATCHING]: MatchingQuestionBaseView,
  [QUESTION_TYPE_LEGACY_MATCHING]: MatchingQuestionBaseView,
  [QUESTION_TYPE_FILL_BLANK]: FillBlankQuestionBaseView,
  [QUESTION_TYPE_LEGACY_FILL_BLANK]: FillBlankQuestionBaseView,
  [QUESTION_TYPE_HOTSPOT]: HotspotQuestionBaseView,
  [QUESTION_TYPE_LEGACY_HOTSPOT]: HotspotQuestionBaseView,
  [QUESTION_TYPE_ESSAY]: EssayQuestionBaseView,
  [QUESTION_TYPE_LEGACY_ESSAY]: EssayQuestionBaseView,
};

// Native React print views. All learner question types now have one, so the Angular `QuestionLoader`
// print fallback below is effectively unused (matching has no separate print view — its base view
// renders in print, as in the Angular template).
const PRINT_VIEW_BY_TYPE: Record<string, React.FC<any>> = {
  [QUESTION_TYPE_MULTIPLE_CHOICE]: MultipleChoiceQuestionPrintView,
  [QUESTION_TYPE_LEGACY_MULTIPLE_CHOICE]: MultipleChoiceQuestionPrintView,
  [QUESTION_TYPE_TRUE_FALSE]: MultipleChoiceQuestionPrintView,
  [QUESTION_TYPE_LEGACY_TRUE_FALSE]: MultipleChoiceQuestionPrintView,
  [QUESTION_TYPE_MULTIPLE_SELECT]: MultipleSelectQuestionPrintView,
  [QUESTION_TYPE_LEGACY_MULTIPLE_SELECT]: MultipleSelectQuestionPrintView,
  [QUESTION_TYPE_ORDERING]: OrderingQuestionPrintView,
  [QUESTION_TYPE_LEGACY_ORDERING]: OrderingQuestionPrintView,
  [QUESTION_TYPE_BIN_DROP]: BinDropQuestionPrintView,
  [QUESTION_TYPE_LEGACY_BIN_DROP]: BinDropQuestionPrintView,
  [QUESTION_TYPE_MATCHING]: MatchingQuestionBaseView,
  [QUESTION_TYPE_LEGACY_MATCHING]: MatchingQuestionBaseView,
  [QUESTION_TYPE_FILL_BLANK]: FillBlankQuestionPrintView,
  [QUESTION_TYPE_LEGACY_FILL_BLANK]: FillBlankQuestionPrintView,
  [QUESTION_TYPE_HOTSPOT]: HotspotQuestionPrintView,
  [QUESTION_TYPE_LEGACY_HOTSPOT]: HotspotQuestionPrintView,
  [QUESTION_TYPE_ESSAY]: EssayQuestionPrintView,
  [QUESTION_TYPE_LEGACY_ESSAY]: EssayQuestionPrintView,
};

// Distinct grading views (only essay has one; every other type grades via its base view, read-only).
const GRADING_VIEW_BY_TYPE: Record<string, React.FC<any>> = {
  [QUESTION_TYPE_ESSAY]: EssayQuestionGradingView,
  [QUESTION_TYPE_LEGACY_ESSAY]: EssayQuestionGradingView,
};

export const QuestionView: React.FC<QuestionViewProps> = props => {
  const inPrintMode = !!(window as any).inPrintMode;
  const type = props.question?._type;
  const ReactView = inPrintMode
    ? PRINT_VIEW_BY_TYPE[type]
    : props.grading
      ? (GRADING_VIEW_BY_TYPE[type] ?? VIEW_BY_TYPE[type])
      : VIEW_BY_TYPE[type];

  if (ReactView) {
    return <ReactView {...props} changeAnswer={props.changeAnswer ?? noop} />;
  }
  return null;
};

export default QuestionView;
