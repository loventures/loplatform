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

import { map } from 'lodash';
import { connect } from 'react-redux';
import { withTranslation } from '../../../i18n/translationContext';

import { selectProgressReportTableComponent } from '../selectors';

import ProgressBadge from '../../../directives/ProgressBadge';

import { InstructorProgressReportPageLink } from '../../../utils/pageLinks';
import LoLink from '../../../components/links/LoLink';

const ProgressReportTable = ({ translate, compactHeaders, headers, rows }) => (
  <div className={'student-progress-table-container ' + (compactHeaders ? 'compact' : '')}>
    <div className="responsive-table-container m-0">
      <table className="table table-striped table-sticky card-table student-progress-table">
        <thead className="thead-default">
          <tr>
            <th className="bg-white">{translate('STUDENT_PROGRESS_TABLE_LEARNERS_HEADER')}</th>
            {map(headers, header => (
              <th key={header.id}>
                {header.canDrill ? (
                  <LoLink
                    to={InstructorProgressReportPageLink.toLink({
                      contentId: header.id,
                    })}
                  >
                    <strong className="content-item-name">{header.name}</strong>
                  </LoLink>
                ) : (
                  <div>
                    <strong className="content-item-name">{header.name}</strong>
                  </div>
                )}
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {map(rows, row => (
            <tr key={row.learner.id}>
              <th>
                <div className="student-name">{row.learner.fullName}</div>
              </th>
              {map(row.cells, (progress, index) => (
                <td key={row.learner.id + '-' + index}>
                  <ProgressBadge
                    className="student-progress-cell"
                    progress={progress}
                  />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  </div>
);

export default connect(selectProgressReportTableComponent)(withTranslation(ProgressReportTable));
