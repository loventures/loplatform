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

import { useQuizApi } from '../../../../../api/quizApi.js';
import dayjs from 'dayjs';
import localized from 'dayjs/plugin/localizedFormat';
import GradeBadge from '../../../../../directives/GradeBadge.jsx';
import GradeBadgeWithTooltip from '../../../../../directives/GradeBadgeWithTooltip.jsx';
import { quizActivityAfterInvalidateActionCreator } from '../../../../../courseActivityModule/actions/quizActivityActions.js';
import { withTranslation } from '../../../../../i18n/translationContext.js';
import { isScoreFinalized } from '../../../../../utilities/attemptStates.js';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

dayjs.extend(localized);

const ViewAttemptTab = ({
  translate,
  attempt,
  attemptNumber,
  showGrade,
  content,
  quiz,
  viewingAs,
}) => {
  const { invalidateAttempt } = useQuizApi();
  const dispatch = useDispatch();
  const canDiscard = !!quiz && window.lo_platform.course.groupType === 'PreviewSection';
  return (
    <div className="flex-row-content">
      <i
        className={attempt.determinesGrade ? 'icon-checkmark' : 'icon-text'}
        aria-hidden={true}
      />

      <span>{translate('ATTEMPT_NUMBER', { number: attemptNumber + 1 })}</span>

      <span className="flex-col-fluid attempt-submission-time">
        {dayjs(attempt.submitTime).format('LLL')}
      </span>

      {attempt.invalid && (
        <span className="block-badge badge-danger">{translate('ATTEMPT_INVALIDATED')}</span>
      )}

      {canDiscard && (
        <Button
          tag="div"
          color="danger"
          size="sm"
          onClick={e => {
            e.stopPropagation();
            invalidateAttempt(attempt.id).then(() => {
              dispatch(
                quizActivityAfterInvalidateActionCreator(content, quiz, viewingAs, viewingAs.id)
              );
            });
          }}
        >
          {translate('ATTEMPT_DISCARD')}
        </Button>
      )}

      {attempt.invalid || !showGrade ? null : isScoreFinalized(attempt) ? (
        <GradeBadge
          grade={attempt.score}
          percent="full"
          className="border border-white"
        />
      ) : (
        <GradeBadgeWithTooltip
          className="grade-badge border border-white colored-grade-bg done-"
          tooltipText={translate('NOT_GRADED')}
          tooltipPlacement="bottom"
          tooltipColor="dark"
        />
      )}
    </div>
  );
};

export default withTranslation(ViewAttemptTab);
