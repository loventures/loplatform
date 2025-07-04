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

//Tthis is to shrink category table width when the category name is too long
//The multiplier (7 now) has to be in sync with the column width in the stylesheet
const columnWidth = numColumns => (numColumns + 1) * 8 + 'rem';

const CategoryHeaderCell = ({ translate, numColumns, categoryTitle, collapseTable }) => (
  <div
    className="category-header-cell"
    style={{ width: columnWidth(numColumns) }}
  >
    <div className="header-text flex-col-fluid">{categoryTitle}</div>
    <button
      className="btn btn-outline-light flex-center-center"
      onClick={collapseTable}
      title={translate('GRADEBOOK_COLLAPSE_TABLE')}
    >
      <span
        className="icon-chevron-right"
        role="presentation"
      ></span>
      <span
        className="icon-chevron-left"
        role="presentation"
      ></span>
      <span className="sr-only">{translate('GRADEBOOK_COLLAPSE_TABLE')}</span>
    </button>
  </div>
);

export default withTranslation(CategoryHeaderCell);
