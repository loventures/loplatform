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

import { withTranslation } from '../../../../../i18n/translationContext.js';

const TrialLearnerReadOnlyText = ({ translate }) => (
  <span className="flex-col-fluid text-right">{translate('ASSESSMENT_READ_ONLY_STATUS')}</span>
);

const AttemptsRemainingText = ({
  translate,
  unlimitedAttempts,
  attemptsRemaining,
  isCheckpoint,
}) =>
  unlimitedAttempts ? (
    !isCheckpoint ? (
      <span className="flex-col-fluid text-right">
        {translate('ASSIGNMENT_ATTEMPT_UNLIMITED_ATTEMPTS')}
      </span>
    ) : null
  ) : (
    <span className="flex-col-fluid d-flex align-items-center justify-content-end">
      <i
        className="display-attempts-remaining icon icon-warning me-1"
        aria-hidden="true"
      />
      <span className="display-attempts-remaining">
        {translate(
          isCheckpoint
            ? 'CHECKPOINT_SUBMISSIONS_REMAINING_COUNT'
            : 'ASSIGNMENT_ATTEMPTS_REMAINING_COUNT',
          {
            count: attemptsRemaining,
          }
        )}
      </span>
    </span>
  );

const NewAttemptTab = ({
  translate,
  attemptsRemaining,
  unlimitedAttempts,
  isReadOnly,
  viewingAs,
  isCheckpoint,
  i18nKey,
}) => (
  <div className="flex-row-content justify-content-between">
    <i
      className="icon icon-plus-circle"
      aria-hidden={true}
    />
    <span className="flex-col-fluid text-left">{translate(i18nKey)}</span>

    {isReadOnly && viewingAs.isUnderTrialAccess ? (
      <TrialLearnerReadOnlyText translate={translate} />
    ) : (
      <AttemptsRemainingText
        translate={translate}
        unlimitedAttempts={unlimitedAttempts}
        attemptsRemaining={attemptsRemaining}
        isCheckpoint={isCheckpoint}
      />
    )}
  </div>
);

export default withTranslation(NewAttemptTab);
