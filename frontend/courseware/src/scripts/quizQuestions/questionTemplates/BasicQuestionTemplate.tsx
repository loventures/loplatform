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
import React, { useMemo } from 'react';

import { HtmlWithMathJax } from '../../components/HtmlWithMathjax';
import { useTranslation } from '../../i18n/translationContext.tsx';
import settings from '../../utilities/settingsService';
import {
  LEGACY_RESPONSE_SAVED,
  RESPONSE_NOT_SUBMITTED,
  RESPONSE_SUBMITTED,
} from '../../utilities/attemptStates.js';
import {
  QUESTION_TYPE_ESSAY,
  QUESTION_TYPE_FILL_BLANK,
  QUESTION_TYPE_HOTSPOT,
  QUESTION_TYPE_LEGACY_ESSAY,
  QUESTION_TYPE_LEGACY_FILL_BLANK,
  QUESTION_TYPE_LEGACY_HOTSPOT,
  QUESTION_TYPE_LEGACY_MULTIPLE_CHOICE,
  QUESTION_TYPE_LEGACY_TRUE_FALSE,
  QUESTION_TYPE_MULTIPLE_CHOICE,
  QUESTION_TYPE_TRUE_FALSE,
} from '../../utilities/questionTypes.js';

import QuestionCompetencies from '../questionAddons/QuestionCompetencies.tsx';
import { QuestionGradingStrategy } from '../questionAddons/questionGradingStrategy.tsx';
import { QuestionLevelRemediation } from '../questionAddons/QuestionLevelRemediation.tsx';
import { QuestionResourceRemediationList } from '../questionAddons/QuestionResourceRemediationList.tsx';
import { QuestionScore } from '../questionAddons/questionScore.tsx';

// Same partial-credit gate as the Angular controller: these types do NOT show a
// grading-strategy hint (until a non-spammy presentation exists).
export const getQuestionTypeHasPartialCredit = (questionType?: string) =>
  [
    QUESTION_TYPE_MULTIPLE_CHOICE,
    QUESTION_TYPE_TRUE_FALSE,
    QUESTION_TYPE_HOTSPOT,
    QUESTION_TYPE_ESSAY,
    QUESTION_TYPE_FILL_BLANK,
    QUESTION_TYPE_LEGACY_MULTIPLE_CHOICE,
    QUESTION_TYPE_LEGACY_TRUE_FALSE,
    QUESTION_TYPE_LEGACY_HOTSPOT,
    QUESTION_TYPE_LEGACY_ESSAY,
    QUESTION_TYPE_LEGACY_FILL_BLANK,
  ].indexOf(questionType as string) === -1;

interface Score {
  pointsAwarded?: number;
  pointsPossible?: number;
}
interface Response {
  state?: string;
  selection?: { skip?: boolean };
  score?: Score;
}
interface Question {
  _type?: string;
  reference?: { nodeName?: string };
  questionText?: string;
  remediationText?: { correct?: string; incorrect?: string };
  remediationResources?: any[];
  competencies?: any[];
  possiblePoints?: number;
}

export interface BasicQuestionTemplateProps {
  index: number;
  question: Question;
  response?: Response;
  attempt?: any;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  /** Extra classes for the root (the per-type classes the Angular base views set on `<basic-question-template>`). */
  className?: string;
  /** Overrides the default `.question-text` prompt (the Angular `questionTextSlot`). */
  questionTextSlot?: React.ReactNode;
  /** The answer UI (the Angular `questionContentSlot`). */
  children?: React.ReactNode;
}

/**
 * React port of the shared `basicQuestionTemplate` hub (B2-quiz). Every question
 * type renders its answer UI into this chrome: the question number column (with
 * answered/correct/incorrect/checkpoint state + aria), the prompt, the score and
 * grading-strategy hints, then question-level + resource remediation and aligned
 * competencies. The Angular transclusion slots become props: `children` is the
 * old `questionContentSlot`, `questionTextSlot` the optional prompt override.
 *
 * DOM is preserved verbatim from `basicQuestionTemplate.html` so the Selenide
 * quiz selectors hold — including wrapping the (already-React) competencies in a
 * `<question-competencies>` host element, which the page objects target by tag.
 *
 * This is a plain React component (no react2angular bridge): a React hub cannot
 * host Angular-transcluded content, so it is consumed by React question-type
 * views as they migrate. The Angular `basicQuestionTemplate` component remains
 * for the type views still on Angular.
 */
export const BasicQuestionTemplate: React.FC<BasicQuestionTemplateProps> = ({
  index,
  question,
  response,
  assessment,
  questionCount,
  className,
  questionTextSlot,
  children,
}) => {
  const translate = useTranslation();
  const showQuestionPoints = useMemo(
    () => settings.isFeatureEnabled('ShowPoints'),
    []
  );

  const questionTypeHasPartialCredit = getQuestionTypeHasPartialCredit(question._type);
  const isCheckpoint = assessment?.settings?.isCheckpoint;
  const hideQuestionNumber = questionCount === 1;
  const showCompetencies = response?.state !== RESPONSE_NOT_SUBMITTED;

  const score = response?.score;
  const hasScore = !isEmpty(score);
  // Mirror the Angular controller exactly (undefined >= undefined is false); no defaulting.
  const isCorrect = !!score && (score.pointsAwarded as number) >= (score.pointsPossible as number);
  const isAnswered =
    response?.state === LEGACY_RESPONSE_SAVED ||
    response?.state === RESPONSE_SUBMITTED ||
    !!response?.selection;

  let questionAriaLabel = '';
  if (response) {
    const t9nArgs = { questionNumber: index + 1, questionType: translate(question._type ?? '') };
    questionAriaLabel = isCorrect
      ? translate('QUIZ_PLAYER_QUESTION_LABEL_CORRECT', t9nArgs)
      : hasScore
        ? translate('QUIZ_PLAYER_QUESTION_LABEL_INCORRECT', t9nArgs)
        : translate('QUIZ_PLAYER_QUESTION_LABEL', t9nArgs);
  }

  const numberClasses = [
    'question-number',
    isAnswered && !response?.selection?.skip ? 'answered' : '',
    response?.selection?.skip ? 'skipped' : '',
    isCorrect && !isCheckpoint ? 'correct' : '',
    hasScore && !isCorrect && !isCheckpoint ? 'incorrect' : '',
    isCheckpoint ? 'checkpoint' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div
      className={
        className
          ? `basic-question-template question-container ${className}`
          : 'basic-question-template question-container'
      }
      data-asset-name={question.reference?.nodeName}
    >
      <div className="question-body mb-2">
        {!hideQuestionNumber && (
          <div className="question-number-column">
            <h2
              id={`question-label=${index}`}
              className={numberClasses}
              aria-label={questionAriaLabel}
            >
              {index + 1}
            </h2>
          </div>
        )}

        <div className={hideQuestionNumber ? 'question-content-column unnumbered' : 'question-content-column'}>
          {questionTextSlot ?? (
            <div
              className="question-text"
              data-id="prompt"
            >
              <HtmlWithMathJax html={question.questionText} />
            </div>
          )}

          {showQuestionPoints && (
            <QuestionScore
              question={question}
              response={response}
              translate={translate}
            />
          )}

          {questionTypeHasPartialCredit && !isCheckpoint && (
            <QuestionGradingStrategy
              question={question}
              translate={translate}
            />
          )}

          {children}
        </div>
      </div>

      {question.remediationText && <QuestionLevelRemediation remediation={question.remediationText} />}

      {!!question.remediationResources?.length && (
        <QuestionResourceRemediationList
          resources={question.remediationResources}
          translate={translate}
        />
      )}

      {!!question.competencies?.length &&
        showCompetencies &&
        // The page objects target `question-competencies` by tag, so wrap the
        // (already-React) competencies in that host element, as react2angular did.
        React.createElement(
          'question-competencies',
          // a custom element gets no className→class mapping, so emit `class` directly
          { class: 'd-block', 'data-id': 'alignment' },
          <QuestionCompetencies
            competencies={question.competencies}
            translate={translate}
          />
        )}
    </div>
  );
};

export default BasicQuestionTemplate;
