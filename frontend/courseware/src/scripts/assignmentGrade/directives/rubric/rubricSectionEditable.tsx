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
import { findIndex, max } from 'lodash';
import React, { useEffect, useReducer, useRef, useState } from 'react';

import { useTranslation } from '../../../i18n/translationContext';
import settings from '../../../utilities/settingsService';

interface RubricSectionEditableProps {
  /** A `RubricSection` model (stays Angular) — mutated in place via its setters. */
  section: any;
  titleIcon?: any;
  titleAction?: (section: any) => void;
}

const rounded = (n: number) => Math.round(n * 100) / 100;

/**
 * React port of the `rubricSectionEditable` component (assignmentGrade rubric panels): one editable
 * rubric criterion card — title/points, a clickable level list (+ an optional manual-grade row), and a
 * toggleable feedback textarea. Was an Angular component with a controller; now native React, bridged
 * native React; the native-React `gradingRubric` container renders it directly
 * (`{RubricSectionEditable}`). The `RubricSection` model is mutated in place via its setters
 * (`setSelection`/`setManual`/`setFeedback`) — the component force-renders after each (the old Angular
 * digest did this for free). DOM preserved for Selenide (`GradingPanelRubricCriterion`):
 * `.card.rubric-section`, `div.card-title`, `ol > li` (`.rubric-level` + `.selected`/`.manual-level`),
 * `.rubric-level-points`/`.rubric-level-name`, the manual `input`, `textarea.rubric-section-feedback-content`,
 * `button.toggle-add`/`button.toggle-remove`.
 */
export const RubricSectionEditable: React.FC<RubricSectionEditableProps> = ({ section, titleIcon, titleAction }) => {
  const translate = useTranslation();
  const allowManualGrading = settings.isFeatureEnabled('InstructorRubricManualGrading');
  const [, forceRender] = useReducer((c: number) => c + 1, 0);
  // The manual-score input is UNCONTROLLED. The grading panel's 150ms re-render poll (the consolidated
  // GraderProvider, #1552) fires constantly while grading and a controlled `value` would be re-asserted
  // mid-entry — racing Selenide's clear+type so the typed value gets clobbered or appended (the
  // GradingPanelTest "upgrade the grade to something invalid" flake: e.g. value="46-5" instead of "-5").
  // With no `value` prop the poll re-render can't touch the DOM value, so editing is reliable. The model +
  // the `invalid` flag (for the ng-invalid class) are updated in onChange; external section changes resync
  // the DOM via the ref. Validation/model use Number(text) — a plain `type=text` keeps "-"/"." intact.
  const manualInputRef = useRef<HTMLInputElement>(null);
  const manualValue = () =>
    section.isSelectionManual && section.selectedPoints != null ? String(section.selectedPoints) : '';
  const [manualInvalid, setManualInvalid] = useState<boolean>(() => {
    const v = manualValue();
    return v !== '' && !section.isValidManualScore(Number(v));
  });
  const feedbackRef = useRef<HTMLTextAreaElement>(null);

  // CBLPROD: optionally preselect the highest-points level on first render.
  useEffect(() => {
    if (settings.isFeatureEnabled('RubricGraderUseMaxCriterionValue')) {
      const maxPoints = max(section.levels.map((l: any) => l.points));
      const maxLevelIndex = findIndex(section.levels, (level: any) => level.points === maxPoints);
      section.setSelection(maxLevelIndex);
      forceRender();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // The old `$onChanges({ section })`: keep the manual input in sync when the section changes — including
  // when an external "Undo Changes" replaces the rubric and `RubricGradePanel` re-renders with fresh
  // section objects (see the rubric-reference watch there).
  useEffect(() => {
    const v = manualValue();
    if (manualInputRef.current) manualInputRef.current.value = v;
    setManualInvalid(v !== '' && !section.isValidManualScore(Number(v)));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [section]);

  // The feedback textarea is shown via the model's `feedbackStatus`; autofocus it when revealed.
  useEffect(() => {
    if (section.feedbackStatus) feedbackRef.current?.focus();
  }, [section.feedbackStatus]);

  // Mutating the `RubricSection` happens in React-land; re-render here so this criterion card reflects
  // the change. The still-Angular grading panel's running points-awarded total re-evaluates via the
  // GraderProvider 150ms poll.
  const sync = () => {
    forceRender();
  };

  const setSelection = (index: number) => {
    section.setSelection(index);
    sync();
  };
  const setManual = (text: string) => {
    section.setManual(text === '' ? null : Number(text));
    setManualInvalid(text !== '' && !section.isValidManualScore(Number(text)));
    sync();
  };
  const setFeedbackText = (value: string) => {
    section.setFeedback(value);
    sync();
  };
  const setFeedbackStatus = (status: boolean) => {
    section.feedbackStatus = status;
    if (!status) section.setFeedback('');
    sync();
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
          onClick={() => {
            titleAction?.(section);
          }}
        >
          <span className="flex-col-fluid">
            <span className="h5 word-wrap-all">{section.title}</span>
            <span className="text-nowrap">{translate('RUBRIC_SECTION_POINTS', { points: section.points })}</span>
          </span>
          <span className={classnames('h5 icon', titleIcon)} />
        </div>

        <hr />

        <ol
          className="list-group"
          role="group"
          aria-label={translate('GRADING_RUBRIC_SECTION_SELECT_SCORE')}
        >
          {section.levels.map((level: any) => (
            // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions, jsx-a11y/click-events-have-key-events
            <li
              key={level.index}
              className={classnames('rubric-level flex-row-content', {
                selected: section.selectionLevelIndex === level.index,
              })}
              onClick={() => setSelection(level.index)}
              title={translate('GRADING_RUBRIC_SECTION_ROW_DESC', level)}
              aria-selected={section.selectionLevelIndex === level.index}
            >
              <div className="rubric-level-points">{level.points}</div>
              <div className="rubric-level-name">{level.name}</div>
            </li>
          ))}

          {allowManualGrading && (
            <li
              className={classnames('rubric-level flex-row-content manual-level', {
                selected: section.isSelectionManual,
              })}
              title={translate('GRADING_RUBRIC_SECTION_MANUAL_LEVEL')}
            >
              <div className="rubric-level-points">
                {section.isSelectionManual && section.selectedPoints != null ? (
                  <span>{rounded(section.selectedPoints)}</span>
                ) : (
                  <span
                    className="icon icon-pencil"
                    role="presentation"
                  />
                )}
              </div>

              <div className="rubric-level-name">
                <input
                  // `ng-invalid` reproduces Angular's number-input out-of-range class the grading panel
                  // (and GradingPanelTest) keys off — a manual score must be 0..section.points.
                  ref={manualInputRef}
                  className={classnames('form-control', { 'ng-invalid': manualInvalid })}
                  type="text"
                  inputMode="decimal"
                  aria-label={translate('GRADING_RUBRIC_MANUAL_GRADE_PLACEHOLDER')}
                  defaultValue={manualValue()}
                  onChange={e => setManual(e.target.value)}
                  placeholder={translate('RUBRIC_MANUAL_GRADE_PLACEHOLDER', section)}
                />
              </div>
            </li>
          )}
        </ol>

        <div
          className="rubric-section-feedback"
          role="region"
          aria-label={translate('GRADING_RUBRIC_SECTION_FEEDBACK')}
        >
          {section.feedbackStatus && (
            <textarea
              ref={feedbackRef}
              className="form-control rubric-section-feedback-content"
              value={section.feedback ?? ''}
              onChange={e => setFeedbackText(e.target.value)}
              onBlur={() => setFeedbackStatus(!!section.feedback)}
              aria-label={translate('GRADING_RUBRIC_SECTION_FEEDBACK_PLACEHOLDER')}
              placeholder={translate('GRADING_RUBRIC_SECTION_FEEDBACK_PLACEHOLDER')}
            />
          )}

          {/* Both buttons stay in the DOM (the old `ng-hide`/`ng-show`), visibility toggled by
              `feedbackStatus` — the page object selects the add button as the first `button.btn-link`,
              so their order must be stable. */}
          <button
            className="toggle-add btn btn-link"
            type="button"
            aria-label={translate('GRADING_RUBRIC_SECTION_FEEDBACK_ADD')}
            style={section.feedbackStatus ? { display: 'none' } : undefined}
            onClick={() => setFeedbackStatus(true)}
          >
            <span className="icon icon-bubble-plus" />
            <span>{translate('GRADING_RUBRIC_SECTION_FEEDBACK_ADD')}</span>
          </button>

          <button
            className="toggle-remove btn btn-link"
            type="button"
            aria-label={translate('GRADING_RUBRIC_SECTION_FEEDBACK_REMOVE')}
            style={section.feedbackStatus ? undefined : { display: 'none' }}
            onClick={() => setFeedbackStatus(false)}
          >
            <span className="icon icon-cross" />
            <span>{translate('GRADING_RUBRIC_SECTION_FEEDBACK_REMOVE')}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
