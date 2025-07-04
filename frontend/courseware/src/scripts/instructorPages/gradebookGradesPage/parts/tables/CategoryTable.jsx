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
import { percentFilter } from '../../../../filters/percent';
import { withTranslation } from '../../../../i18n/translationContext';

import CategoryHeaderCell from '../cells/CategoryHeaderCell';
import ColumnHeaderCell from '../cells/ColumnHeaderCell';
import OverallColumnHeaderCell from '../cells/OverallColumnHeaderCell';
import GradeTableBody from './GradeTableBody';

const suffix = (weight, totalWeight) =>
  totalWeight ? ' - ' + percentFilter(weight / totalWeight, 1) : '';

const CategoryTable = ({
  categoryId,
  categoryTitle,
  columnIds,
  collapseTable,
  weight,
  totalWeight,
}) => (
  <table className="gradebook-table category-table">
    <thead>
      <tr>
        <th
          className="category-header"
          colSpan={columnIds.length + 1}
        >
          <CategoryHeaderCell
            numColumns={columnIds.length}
            categoryTitle={categoryTitle + suffix(weight, totalWeight)}
            collapseTable={collapseTable}
          />
        </th>
      </tr>
      <tr>
        {map(columnIds, columnId => (
          <th
            className="column-header"
            key={columnId}
          >
            <ColumnHeaderCell columnId={columnId} />
          </th>
        ))}
        <th className="column-header">
          <OverallColumnHeaderCell categoryId={categoryId} />
        </th>
      </tr>
    </thead>
    <GradeTableBody
      categoryId={categoryId}
      columnIds={columnIds}
      percentFormat={totalWeight > 0}
    />
  </table>
);

export default withTranslation(CategoryTable);
