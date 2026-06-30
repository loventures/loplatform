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

import classnames from 'classnames';
import React, { useState } from 'react';

import { useTranslation } from '../../../i18n/translationContext';

interface RubricSectionReadOnlyProps {
  /** A `RubricSection` model (stays Angular) — read here, never mutated. */
  section: any;
  titleIcon?: any;
  titleAction?: (section: any) => void;
  showLevelDesc?: boolean;
  hideFeedback?: boolean;
}

/**
 * React port of the `rubricSectionReadOnly` component (assignmentGrade rubric panels): one read-only
 * rubric criterion card — title/points, the level list (with the selected level highlighted, plus a
 * manual-grade row), and collapsible section feedback. Was a template-only Angular component; now
 * native React; the native-React containers (`gradingRubricViewCards`, `rubricModal`) render it
 * directly (`{RubricSectionReadOnly}`). The
 * `RubricSection` model is passed through untouched. DOM preserved for Selenide
 * (`GradingPanelRubricCriterion`): `.card.rubric-section`, `div.card-title`, `small`, `ol > li`
 * (`.rubric-level` + `.selected`/`.manual-level`), `.rubric-level-points`/`.rubric-level-name`/
 * `.rubric-level-desc`, `textarea`-free read view, `.rubric-section-feedback-content`.
 */
export const RubricSectionReadOnly: React.FC<RubricSectionReadOnlyProps> = ({
  section,
  titleIcon,
  titleAction,
  showLevelDesc,
  hideFeedback,
}) => {
  const translate = useTranslation();
  const [showingFeedback, setShowingFeedback] = useState(false);

  // `titleAction` opens the rubric modal (a React `openRubricModal`) or toggles the modal's active
  // criterion; the React modal re-renders itself, so no digest nudge is needed.
  const onTitle = () => {
    titleAction?.(section);
  };

  return (
    <div
      className="card rubric-section"
      role="region"
      aria-label={translate('GRADING_RUBRIC_SECTION_REGION')}
    >
      <div className="card-body">
        {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events */}
        <div
          className="card-title flex-row-content"
          title={translate('GRADING_RUBRIC_SECTION_TITLE_DESC', section)}
          onClick={onTitle}
        >
          <span className="flex-col-fluid">
            <span className="h5 word-wrap-all">{section.title}</span>
            <span className="text-nowrap">{translate('RUBRIC_SECTION_POINTS', { points: section.points })}</span>
          </span>
          <span className={classnames('h5 icon', titleIcon)} />
        </div>

        <small>{section.description}</small>

        <hr />

        <ol
          className="rubric-section-levels list-group"
          role="group"
          aria-label={translate('GRADING_RUBRIC_SECTION_SELECT_SCORE')}
        >
          {section.levels.map((level: any) => (
            <li
              key={level.index}
              className={classnames('rubric-level flex-row-content', {
                selected: section.selectionLevelIndex === level.index,
              })}
              title={translate('GRADING_RUBRIC_SECTION_ROW_DESC', level)}
              aria-selected={section.selectionLevelIndex === level.index}
            >
              <div className="rubric-level-points">{level.points}</div>
              <div className="rubric-level-name">
                <div>{level.name}</div>
                {showLevelDesc && !!level.description && (
                  <small className="rubric-level-desc">{level.description}</small>
                )}
              </div>
            </li>
          ))}

          {section.isSelectionManual && (
            <li
              className="rubric-level flex-row-content manual-level selected"
              title={translate('GRADING_RUBRIC_SECTION_MANUAL_LEVEL')}
              aria-selected={true}
            >
              <div className="rubric-level-points">{section.selectedPoints}</div>
              <div className="rubric-level-name">{translate('GRADING_RUBRIC_SECTION_MANUAL_GRADE')}</div>
            </li>
          )}
        </ol>

        {section.feedback && !hideFeedback && (
          <div
            className="rubric-section-feedback"
            role="region"
            aria-label={translate('GRADING_RUBRIC_SECTION_FEEDBACK')}
          >
            <div className="section-feedback-toggle">
              {!showingFeedback ? (
                // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
                <span
                  className="btn btn-link"
                  onClick={() => setShowingFeedback(true)}
                >
                  <span className="icon icon-bubble-plus" />
                  <span>{translate('RUBRIC_SECTION_FEEDBACK_SHOW')}</span>
                </span>
              ) : (
                // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
                <span
                  className="btn btn-link"
                  onClick={() => setShowingFeedback(false)}
                >
                  <span className="icon icon-minus" />
                  <span>{translate('RUBRIC_SECTION_FEEDBACK_HIDE')}</span>
                </span>
              )}
            </div>

            {showingFeedback && <div className="rubric-section-feedback-content">{section.feedback}</div>}
          </div>
        )}
      </div>
    </div>
  );
};
