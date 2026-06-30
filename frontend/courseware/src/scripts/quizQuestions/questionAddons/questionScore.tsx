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

import * as React from 'react';
import { WithTranslateProps } from '../../i18n/translationContext.tsx';
import { grade, makeGradeDisplayMethods } from '../../filters/pure/grade.ts';

const gradeMethods = makeGradeDisplayMethods();

export type QuestionScoreProps = WithTranslateProps & {
  question?: { possiblePoints?: number };
  response?: { score?: any; assessedScore?: any };
};

/**
 * Shows a quiz question's earned points and the possible-points label, migrated
 * verbatim from the AngularJS `questionScore` component to React (bridged via
 * react2angular). DOM preserved: a `.question-score` div with the rounded score
 * (the `grade:'points'` filter) and a pluralized possible-points string.
 */
export const QuestionScore = ({ question, response, translate }: QuestionScoreProps) => {
  if (response?.score && !response?.assessedScore) {
    const points = question?.possiblePoints;
    const possibleLabel =
      points === 0
        ? translate('QUESTION_SCORE_NO_POINTS')
        : points === 1
          ? translate('QUESTION_SCORE_ONE_POINT')
          : translate('QUESTION_SCORE_N_POINTS', { points });

    return (
      <div className="question-score">
        <span>{grade(gradeMethods, response.score, 'points') as React.ReactNode}</span>
        <span>{possibleLabel}</span>
      </div>
    );
  }

  if (response?.assessedScore) {
    return <div className="question-score" />;
  }

  return null;
};
