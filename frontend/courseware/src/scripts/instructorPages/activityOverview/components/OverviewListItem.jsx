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

import LoLink from '../../../components/links/LoLink';
import { InstructorGraderPageLink } from '../../../utils/pageLinks';
import { first } from 'lodash';
import GradeBadge from '../../../directives/GradeBadge';
import { withTranslation } from '../../../i18n/translationContext';
import { getUserFullName } from '../../../utilities/getUserFullName';
import { connect } from 'react-redux';

import { openSendMessageModalActionCreator } from '../services/sendMessageActions';
import { lojector } from '../../../loject';

const formatDayjs = (time, fmt) => lojector.get('formatDayjsFilter')(time, fmt);

const OverviewListItem = ({ translate, item, content, withWork, openModal }) => (
  <li
    className="list-group-item"
    title={translate('OVERVIEW_GRADE_EXPLANATION')}
  >
    <div className="flex-row-content">
      <span className="flex-col-fluid">
        {withWork ? (
          <LoLink
            to={InstructorGraderPageLink.toLink({
              contentId: content.id,
              forLearnerId: item.learner.id,
              attemptId: first(item.gradeableAttempts),
            })}
          >
            <span className="learner-name">{getUserFullName(item.learner)}</span>
          </LoLink>
        ) : (
          <button
            className="btn btn-link"
            onClick={() => openModal(item)}
          >
            <span className="learner-name">{getUserFullName(item.learner)}</span>
          </button>
        )}
      </span>

      {item.invalidAttemptCount > 0 && (
        <small className="has-invalid text-danger d-flex align-items-center">
          <span className="icon icon-warning"></span>
          <span>{translate('OVERVIEW_GRADE_STATUS_HAS_INVALIDATED')}</span>
        </small>
      )}

      {item.gradeableAttempts.length > 0 && (
        <small className="needs-review text-danger d-flex align-items-center">
          <span className="icon icon-warning"></span>
          <span>&nbsp;</span>
          <span>
            {translate(
              'OVERVIEW_GRADE_STATUS_NEED_REVIEW',
              {
                num: item.gradeableAttempts.length,
              },
              'messageformat'
            )}
          </span>
        </small>
      )}

      {item.mostRecentSubmission && (
        <span className="submitted-date">
          <span>{translate('SUBMITTED')}</span>:&nbsp;
          <span>{formatDayjs(item.mostRecentSubmission)}</span>
        </span>
      )}

      <div
        role="button"
        tabIndex="0"
        onClick={event => {
          event.stopPropagation();
          openModal(item);
        }}
        onKeyPress={evt => {
          evt.keyCode === '13' && openModal(item);
        }}
      >
        <GradeBadge
          grade={item.grade}
          percent="full"
        />
      </div>
    </div>
  </li>
);

export default connect(null, {
  openModal: openSendMessageModalActionCreator,
})(withTranslation(OverviewListItem));
