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

import { Rubric, RubricFeedback, RubricScore } from '../../api/quizApi';
import { TranslationContext } from '../../i18n/translationContext';
import { keyBy, map, max, some, sum } from 'lodash';
import React, { useContext, useMemo } from 'react';

import RubricGridSection from './RubricGridSection';

const RubricGrid: React.FC<{
  noTitle?: boolean;
  rubric: Rubric;
  rubricScore?: RubricScore;
  rubricFeedback?: RubricFeedback[];
}> = ({ noTitle, rubric, rubricScore, rubricFeedback }) => {
  const translate = useContext(TranslationContext);
  const scores = (rubricScore && rubricScore.nullableCriterionScores) || {};
  const maxColumns = useMemo(() => {
    return (
      max(
        map(rubric.sections, section => {
          const sectionHasScore = !!scores[section.name];
          const isManual =
            sectionHasScore &&
            !some(section.levels, level => {
              return level.points === scores[section.name].pointsAwarded;
            });
          const columns = 1 + section.levels.length + (isManual ? 1 : 0);
          return columns;
        })
      ) || 0
    ); //probably never 0, but to make ts happy with `max`
  }, [rubric, rubricScore]);
  const feedbackBySection = keyBy(rubricFeedback, f => f.sectionName);
  const totalPointsAwarded = sum(map(scores, s => s.pointsAwarded));
  const totalPointsPossible = rubricScore
    ? sum(map(scores, s => s.pointsPossible))
    : sum(map(rubric.sections, s => max(map(s.levels, l => l.points))));
  return (
    <section
      className="rubric-grid"
      title={translate('GRADING_RUBRIC_REGION')}
    >
      {!noTitle && <header className="h5 mb-2">{translate('GRADING_RUBRIC')}</header>}

      <table className={`rubric-grid max-columns-${maxColumns}`}>
        <thead>
          <tr>
            <th>{translate('RUBRIC_CRITERIA')}</th>
            <th
              aria-label={translate('TABLE_COLUMN_SPANS_NUM', {
                num: maxColumns - 1,
              })}
            >
              {translate('RUBRIC_LEVELS')}
            </th>
          </tr>
        </thead>

        <tbody>
          {map(rubric.sections, section => (
            <RubricGridSection
              key={section.name}
              section={section}
              score={scores[section.name]}
              feedback={feedbackBySection[section.name]}
            />
          ))}
        </tbody>

        <tfoot>
          <tr>
            <td aria-label={translate('TABLE_COLUMN_SPANS_ALL')}>
              {rubricScore ? (
                <strong className="rubric-total-score">
                  {translate('RUBRIC_TOTAL_POINTS_SELECTED', {
                    pointsAwarded: totalPointsAwarded,
                    pointsPossible: totalPointsPossible,
                  })}
                </strong>
              ) : (
                <strong>
                  {translate('RUBRIC_TOTAL_POINTS_MAX', {
                    pointsPossible: totalPointsPossible,
                  })}
                </strong>
              )}
            </td>
          </tr>
        </tfoot>
      </table>
    </section>
  );
};

export default RubricGrid;
