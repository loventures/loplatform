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

import { RubricFeedback, RubricSection, Score } from '../../api/quizApi';
import { TranslationContext } from '../../i18n/translationContext';
import { find, map, max } from 'lodash';
import React, { useContext, useMemo } from 'react';

const RubricGridSection: React.FC<{
  section: RubricSection;
  score: Score;
  feedback: RubricFeedback;
}> = ({ section, score, feedback }) => {
  const translate = useContext(TranslationContext);
  const sectionMaxPoints = useMemo(() => {
    return max(map(section.levels, l => l.points));
  }, [section]);
  const selectedLevel = useMemo(() => {
    if (!score) {
      return null;
    }
    return find(section.levels, l => l.points === score.pointsAwarded) || null;
  }, [section, score]);
  return (
    <tr role="row">
      <th role="gridcell">
        <div className="rubric-grid-cell">
          <div className="h6 no-ip mb-0 flex-row-content align-items-start">
            <span className="flex-col-fluid word-wrap-all">{section.title}</span>
          </div>
          <small className="description">{section.description}</small>

          {feedback && !score && (
            <small className="section-feedback">
              <div className="font-weight-bold">{translate('RUBRIC_SECTION_FEEDBACK')}</div>
              <span>{feedback.comment}</span>
            </small>
          )}

          {score && (
            <div className="rubric-criteria-points">
              {translate('RUBRIC_SECTION_POINTS_SELECTED', {
                pointsAwarded: score.pointsAwarded,
                pointsPossible: sectionMaxPoints,
              })}
            </div>
          )}

          {score && selectedLevel && (
            <span className="selected-name-notice sr-only">{selectedLevel.name}</span>
          )}

          {score && !selectedLevel && (
            <span className="manual-entry-notice sr-only">
              {translate('GRADING_RUBRIC_SECTION_MANUAL_GRADE')}
            </span>
          )}
        </div>
      </th>

      {score && !selectedLevel && (
        <td className="selected">
          <div className="rubric-grid-cell">
            <div className="h6 no-ip mb-0">{translate('GRADING_RUBRIC_SECTION_MANUAL_GRADE')}</div>
            <small className="description"></small>
            {feedback && !selectedLevel && (
              <small className="section-feedback">
                <div className="font-weight-bold">{translate('RUBRIC_SECTION_FEEDBACK')}</div>
                <span>{feedback.comment}</span>
              </small>
            )}
            <div className="rating-points-awarded">
              {translate('RUBRIC_SECTION_POINTS', {
                points: score.pointsAwarded,
              })}
            </div>
          </div>
        </td>
      )}

      {map(section.levels, (level, idx) => (
        <td
          key={idx}
          className={level === selectedLevel ? 'selected' : undefined}
        >
          <div className="rubric-grid-cell">
            <div className="name h6 no-ip mb-0">{level.name}</div>
            <small className="description">{level.description}</small>
            {feedback && level === selectedLevel && (
              <small className="section-feedback">
                <div className="font-weight-bold">{translate('RUBRIC_SECTION_FEEDBACK')}</div>
                <span>{feedback.comment}</span>
              </small>
            )}
            <div className="rating-points-awarded">
              {translate('RUBRIC_SECTION_POINTS', {
                points: level.points,
              })}
            </div>
          </div>
        </td>
      ))}
    </tr>
  );
};

export default RubricGridSection;
