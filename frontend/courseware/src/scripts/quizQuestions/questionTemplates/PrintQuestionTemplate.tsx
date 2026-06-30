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

import React, { useMemo } from 'react';

import { HtmlWithMathJax } from '../../components/HtmlWithMathjax';
import { useTranslation } from '../../i18n/translationContext.tsx';
import settings from '../../utilities/settingsService';
import QuestionCompetencies from '../questionAddons/QuestionCompetencies.tsx';
import { QuestionGradingStrategy } from '../questionAddons/questionGradingStrategy.tsx';
import { QuestionLevelRemediation } from '../questionAddons/QuestionLevelRemediation.tsx';
import { QuestionResourceRemediationList } from '../questionAddons/QuestionResourceRemediationList.tsx';
import { QuestionScore } from '../questionAddons/questionScore.tsx';
import { BasicQuestionTemplateProps, getQuestionTypeHasPartialCredit } from './BasicQuestionTemplate.tsx';

/**
 * React port of the shared `printQuestionTemplate` hub (B2-quiz print): the print chrome a question's
 * print answer UI renders into — the inline "N." number, the prompt, the score + grading-strategy
 * hints, then question-level + resource remediation and competencies. Shares the Angular controller's
 * computed props with `BasicQuestionTemplate` (showQuestionPoints / questionTypeHasPartialCredit /
 * isCheckpoint / hideQuestionNumber). DOM preserved verbatim from `printQuestionTemplate.html`.
 *
 * A plain React component (no react2angular bridge) consumed by React print views as they migrate.
 */
export const PrintQuestionTemplate: React.FC<BasicQuestionTemplateProps> = ({
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
  const showQuestionPoints = useMemo(() => settings.isFeatureEnabled('ShowPoints'), []);
  const questionTypeHasPartialCredit = getQuestionTypeHasPartialCredit(question._type);
  const isCheckpoint = assessment?.settings?.isCheckpoint;
  const hideQuestionNumber = questionCount === 1;

  return (
    <div className={className ? `print-question-template ${className}` : 'print-question-template'}>
      <div className="mb-3">
        {!hideQuestionNumber && <span className="float-left me-2">{index + 1}.</span>}

        <div>
          {questionTextSlot ?? (
            <div className="question-text mb-2">
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

          <div>{children}</div>
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
        // The page objects target `question-competencies` by tag, so wrap the (already-React)
        // competencies in that host element, as react2angular did. (Print shows them unconditionally.)
        React.createElement(
          'question-competencies',
          { class: 'd-block my-3' },
          <QuestionCompetencies
            competencies={question.competencies}
            translate={translate}
          />
        )}
    </div>
  );
};

export default PrintQuestionTemplate;
