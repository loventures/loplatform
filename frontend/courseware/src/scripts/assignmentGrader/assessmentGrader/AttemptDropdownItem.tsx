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

import { useTranslation } from '../../i18n/translationContext.tsx';

/**
 * React port of the `attemptDropdownItem` directive (the grader's attempt-selector row): the attempt
 * title + a single status `block-badge` reflecting the attempt's grading state, and (in the selector's
 * "selected" display) a trailing chevron `icon`. DOM preserved from attemptDropdownItem.html so the
 * `GradingPanelAttemptDropdown.attemptStatusBadge` (`.block-badge`) selector holds.
 */
export const AttemptDropdownItem: React.FC<{ attempt: any; icon?: string }> = ({ attempt, icon }) => {
  const translate = useTranslation();
  const sas = attempt?.scorableAttemptState ?? {};

  return (
    <div className="flex-row-content">
      <span className="flex-col-fluid text-start text-truncate">{attempt?.title}</span>

      {!attempt?.valid && (
        <span className="block-badge badge-danger">{translate('GRADER_ATTEMPT_DELETED')}</span>
      )}

      {attempt?.valid && attempt?.state === 'Open' && (
        <span className="block-badge badge-dark">{translate('GRADER_STATUS_IN_PROGRESS')}</span>
      )}

      {attempt?.valid && sas.awaitsInstructorInput && !sas.requiresInstructorInput && (
        <span className="block-badge badge-dark">{translate('GRADER_STATUS_NOT_REQUIRED')}</span>
      )}

      {attempt?.valid &&
        attempt?.state &&
        attempt.state !== 'Open' &&
        sas.awaitsInstructorInput &&
        sas.requiresInstructorInput && (
          <span className="block-badge badge-warning">{translate('GRADER_STATUS_NEEDS_GRADE')}</span>
        )}

      {attempt?.valid && !sas.scorePosted && sas.hasInstructorInput && (
        <span className="block-badge badge-info">{translate('GRADER_STATUS_DRAFT_SAVED')}</span>
      )}

      {attempt?.valid && sas.scorePosted && (
        <span className="block-badge badge-success">{translate('GRADER_STATUS_GRADE_RELEASED')}</span>
      )}

      {icon && (
        <span
          className={`icon ${icon}`}
          role="presentation"
        />
      )}
    </div>
  );
};

export default AttemptDropdownItem;
