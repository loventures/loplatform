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

import { withTranslation } from '../../../../i18n/translationContext';

import OverallColumnHeaderCell from '../cells/OverallColumnHeaderCell';
import GradeTableBody from './GradeTableBody';

const OverallTable = ({ translate, totalWeight }) => (
  <table className="gradebook-table category-table overall-table">
    <thead>
      <tr>
        <th className="category-header">
          <div className="category-header-cell">
            <div className="header-text">{translate('COURSE_GRADE_HEADER')}</div>
          </div>
        </th>
      </tr>
      <tr>
        <th className="column-header">
          <OverallColumnHeaderCell />
        </th>
      </tr>
    </thead>
    <GradeTableBody
      categoryId={'_root_'}
      percentFormat={totalWeight > 0}
    />
  </table>
);

export default withTranslation(OverallTable);
