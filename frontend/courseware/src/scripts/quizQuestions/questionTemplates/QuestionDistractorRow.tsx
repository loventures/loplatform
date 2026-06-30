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

import { find, isEmpty } from 'lodash';
import React, { useMemo } from 'react';

import { ChoiceCorrectness } from '../questionAddons/choiceCorrectness.tsx';
import { ChoiceLevelRemediation } from '../questionAddons/ChoiceLevelRemediation.tsx';

interface Rationale {
  _type?: string;
  reason?: string;
}
interface Choice {
  correct?: boolean;
  rationales?: Rationale[];
}

export interface QuestionDistractorRowProps {
  choice: Choice;
  hasCorrectness?: boolean;
  isCorrect?: boolean;
  isSelected?: boolean;
  /** Viewing the quiz (content review), not taking an attempt. */
  isInstructor?: boolean;
  isMulti?: boolean;
  index?: number;
  /** The distractor UI (the Angular `distractorSlot` transclusion). */
  children?: React.ReactNode;
}

// A rationale whose only content is an empty paragraph counts as no feedback.
const isBlank = (reason?: string) =>
  isEmpty(reason) || !!reason!.match(/^\s*<p[^>]*>(?:\s*<br[^>]*>)?\s*<\/p>\s*$/);

/**
 * React port of `questionDistractorRowTemplate` (B2-quiz): one choice row with
 * its correctness icon, the transcluded distractor UI (`children`), and the
 * inline choice-level remediation. DOM preserved from the template —
 * `.question-distractor-row-template > .distractor-with-correctness >
 * (choiceCorrectness?) + .distractor`, then the remediation. The dead modal
 * button (`showModalRemediation` was hard-coded false) is dropped.
 */
export const QuestionDistractorRow: React.FC<QuestionDistractorRowProps> = ({
  choice,
  hasCorrectness,
  isCorrect,
  isSelected,
  isInstructor,
  isMulti,
  index,
  children,
}) => {
  const { choiceFeedback, showInlineRemediation } = useMemo(() => {
    const feedback = choice.correct
      ? find(choice.rationales, ({ _type }) => _type === 'rationale')
      : find(choice.rationales, ({ _type }) => _type === 'textRemediation');
    const feedbackReason = !isBlank(feedback?.reason) ? feedback?.reason : undefined;
    const hasAttempt = !isInstructor;

    // Students see feedback for choices they selected; instructors/advisors see it on review.
    const showChoiceLevelRemediation = feedbackReason && (isSelected || !hasAttempt);
    // Also flag correct choices the learner missed that have no remediation content.
    const showInline =
      !!showChoiceLevelRemediation || !!(hasAttempt && hasCorrectness && !isSelected && choice.correct);

    return { choiceFeedback: feedbackReason, showInlineRemediation: showInline };
  }, [choice, hasCorrectness, isSelected, isInstructor]);

  return (
    <div className="question-distractor-row-template">
      <div className="distractor-with-correctness">
        {((hasCorrectness && isSelected) || isInstructor) && <ChoiceCorrectness isCorrect={isCorrect} />}
        <div className="distractor">{children}</div>
      </div>

      {showInlineRemediation && (
        <ChoiceLevelRemediation
          index={index}
          remediation={choiceFeedback}
          isChoiceCorrect={choice.correct}
          isInstructor={isInstructor}
          isSelected={isSelected}
          isMulti={isMulti}
        />
      )}
    </div>
  );
};
