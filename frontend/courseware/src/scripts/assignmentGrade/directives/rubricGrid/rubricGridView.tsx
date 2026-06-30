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

import React, { useMemo, useState } from 'react';

import { useTranslation } from '../../../i18n/translationContext.tsx';

interface Level {
  index: number;
  name?: string;
  description?: string;
  points?: number;
}
interface Section {
  index: number;
  title?: string;
  description?: string;
  points?: number;
  selectedPoints?: number;
  selectionLevelIndex?: number;
  isSelectionManual?: boolean;
  feedback?: string;
  levels: Level[];
}
export interface RubricGridViewProps {
  rubric: { sections: Section[] };
}

/**
 * React port of the `rubricGridView` component (the grading-rubric table). Reads a
 * rubric model's `.sections` (a `Rubric`/`ViewRubric` instance — both still Angular,
 * passed through) and renders the criteria × levels grid with collapsible rows and
 * the points totals. DOM preserved from rubricGridView.html (`.rubric-grid`,
 * `table.rubric-grid.max-columns-N`, `.rubric-grid-cell`, `.selected`, …). Bridged
 * via react2angular so `<rubric-grid-view>` (essay, viewCompositeGrade, rubricGrid)
 * renders unchanged.
 */
export const RubricGridView: React.FC<RubricGridViewProps> = ({ rubric }) => {
  const translate = useTranslation();
  const sections = rubric.sections ?? [];

  const { maxColumns, totalPointsAwarded, totalPointsPossible } = useMemo(() => {
    const maxColumns = sections.reduce((max, section) => {
      const columns = 1 + section.levels.length + (section.isSelectionManual ? 1 : 0);
      return max < columns ? columns : max;
    }, 0);
    return {
      maxColumns,
      totalPointsAwarded: sections.reduce((s, sec) => s + (sec.selectedPoints as number), 0),
      totalPointsPossible: sections.reduce((s, sec) => s + (sec.points ?? 0), 0),
    };
  }, [sections]);

  const numCriteria = { num: maxColumns - 1 };

  // Rows start collapsed (the Angular `rowState.rowCollapsed = true`).
  const [expanded, setExpanded] = useState<Record<number, boolean>>({});
  const toggle = (index: number) => setExpanded(prev => ({ ...prev, [index]: !prev[index] }));

  return (
    <section
      className="rubric-grid"
      role="region"
      title={translate('GRADING_RUBRIC_REGION')}
    >
      <header className="mt-4 h5">{translate('GRADING_RUBRIC')}</header>

      <table
        role="grid"
        className={`rubric-grid max-columns-${maxColumns}`}
      >
        <thead role="rowgroup">
          <tr role="row">
            <th role="columnheader">{translate('RUBRIC_CRITERIA')}</th>
            <th
              role="columnheader"
              aria-label={translate('TABLE_COLUMN_SPANS_NUM', numCriteria)}
            >
              {translate('RUBRIC_LEVELS')}
            </th>
          </tr>
        </thead>

        <tbody role="rowgroup">
          {sections.map(section => {
            const collapsed = !expanded[section.index];
            return (
              <tr
                role="row"
                key={section.index}
                className={collapsed ? 'row-collapsed' : ''}
              >
                <th role="gridcell">
                  <div className="rubric-grid-cell">
                    <div className="h5 mb-0 flex-row-content align-items-start">
                      <span className="flex-col-fluid word-wrap-all">{section.title}</span>
                      <span
                        className={`rubric-criteiria-toggle icon ${collapsed ? 'icon-chevron-right' : 'icon-chevron-down'}`}
                        aria-label={translate('RUBRIC_CRITERIA_TOGGLE')}
                        onClick={() => toggle(section.index)}
                      />
                    </div>
                    <small className="description">{section.description}</small>

                    {!((section.selectedPoints as number) >= 0) && (
                      <strong className="rubric-criteria-points">
                        {translate('RUBRIC_SECTION_POINTS_MAX', { pointsPossible: section.points })}
                      </strong>
                    )}
                    {(section.selectedPoints as number) >= 0 && (
                      <strong className="rubric-criteria-points">
                        {translate('RUBRIC_SECTION_POINTS_SELECTED', {
                          pointsAwarded: section.selectedPoints,
                          pointsPossible: section.points,
                        })}
                      </strong>
                    )}

                    {(section.selectionLevelIndex as number) >= 0 && (
                      <strong className="selected-name-notice">
                        ({section.levels[section.selectionLevelIndex as number].name})
                      </strong>
                    )}
                    {section.selectionLevelIndex === -1 && (
                      <strong className="manual-entry-notice">
                        ({translate('GRADING_RUBRIC_SECTION_MANUAL_GRADE')})
                      </strong>
                    )}

                    {section.feedback && (
                      <small className="section-feedback">
                        <div>{translate('RUBRIC_SECTION_FEEDBACK')}</div>
                        <span>{section.feedback}</span>
                      </small>
                    )}
                  </div>
                </th>

                {section.isSelectionManual && (
                  <td
                    role="gridcell"
                    className="selected"
                  >
                    <div className="rubric-grid-cell">
                      <div className="h6 no-ip mb-0">{translate('GRADING_RUBRIC_SECTION_MANUAL_GRADE')}</div>
                      <small className="description" />
                      <strong className="rating-points-awarded">
                        {translate('RUBRIC_SECTION_POINTS', { points: section.selectedPoints })}
                      </strong>
                    </div>
                  </td>
                )}

                {section.levels.map(level => (
                  <td
                    role="gridcell"
                    key={level.index}
                    className={section.selectionLevelIndex === level.index ? 'selected' : ''}
                  >
                    <div className="rubric-grid-cell">
                      <div className="name h6 no-ip mb-0">{level.name}</div>
                      <small className="description">{level.description}</small>
                      <strong className="rating-points-awarded">
                        {translate('RUBRIC_SECTION_POINTS', { points: level.points })}
                      </strong>
                    </div>
                  </td>
                ))}
              </tr>
            );
          })}
        </tbody>

        <tfoot role="rowgroup">
          <tr role="row">
            <td
              role="gridcell"
              aria-label={translate('TABLE_COLUMN_SPANS_ALL')}
            >
              {totalPointsAwarded >= 0 ? (
                <strong>
                  {translate('RUBRIC_TOTAL_POINTS_SELECTED', {
                    pointsAwarded: totalPointsAwarded,
                    pointsPossible: totalPointsPossible,
                  })}
                </strong>
              ) : (
                <strong>{translate('RUBRIC_TOTAL_POINTS_MAX', { pointsPossible: totalPointsPossible })}</strong>
              )}
            </td>
          </tr>
        </tfoot>
      </table>
    </section>
  );
};

