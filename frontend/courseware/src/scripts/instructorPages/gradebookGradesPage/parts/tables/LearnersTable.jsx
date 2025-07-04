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

import classNames from 'classnames';
import LoLink from '../../../../components/links/LoLink';
import { InstructorLearnerAssignmentsPageLink } from '../../../../utils/pageLinks';
import { map } from 'lodash';
import { withTranslation } from '../../../../i18n/translationContext';
import { getUserFullName } from '../../../../utilities/getUserFullName';
import { connect } from 'react-redux';

import { selectGradebookLearners } from '../../selectors/tableDataSelectors';
import { lojector } from '../../../../loject';

const LearnersTable = ({ translate, learners, showExternalIds, courseId }) => (
  <table className="gradebook-table names-table">
    <thead>
      <tr>
        <th className="column-header">
          <div className="names-header-cell">
            <div className="header-text">{translate('GRADEBOOK_STUDENTS')}</div>
          </div>
        </th>
      </tr>
    </thead>
    <tbody>
      {map(learners, learner => (
        <tr key={learner.id}>
          <td>
            <div
              className={classNames('grade-body-cell', {
                'show-external-ids': showExternalIds,
              })}
            >
              <LoLink
                to={InstructorLearnerAssignmentsPageLink.toLink(learner.id)}
                disabled={learner.inactive}
                style={{ whiteSpace: 'normal' }}
              >
                {(learner.givenName || learner.familyName) && (
                  <>
                    <div
                      className="name-text"
                      style={{ paddingRight: '.25em' }}
                    >
                      {learner.givenName}
                    </div>
                    <div className="name-text">{learner.familyName}</div>
                  </>
                )}
                {!(learner.givenName || learner.familyName) && (
                  <div className="name-text">{getUserFullName(learner)}</div>
                )}
                {showExternalIds && (
                  <div className="name-text external-id">
                    {learner.externalId || translate('LEARNER_NO_EXTERNAL_ID')}
                  </div>
                )}
              </LoLink>
              <span>
                <a
                  className="icon-button icon-download"
                  target="_blank"
                  href={lojector.get('GradebookAPI').downloadStudentGrades(courseId, learner.id)}
                >
                  <span className="sr-only">{translate('DOWNLOAD_STUDENT_GRADEBOOK')}</span>
                </a>
              </span>
            </div>
          </td>
        </tr>
      ))}
    </tbody>
  </table>
);

export default connect(selectGradebookLearners)(withTranslation(LearnersTable));
