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

const TableCollapsedButton = ({ translate, numInGroup, expandTables }) => (
  <button
    className="gradebook-table-collapsed-button btn-reset"
    title={translate('GRADEBOOK_COLLAPSED_BUTTON', { numTables: numInGroup })}
    onClick={expandTables}
  >
    <span
      className="collapse-number block-badge badge-primary"
      role="presentation"
    >
      {numInGroup}
    </span>
    <div
      className="table-collapse-indicator"
      role="presentation"
    >
      <div className="icon icon-chevron-left"></div>
      <div className="line-in-middle"></div>
      <div className="icon icon-chevron-right"></div>
    </div>
    <div className="sr-only">
      {translate('GRADEBOOK_COLLAPSED_BUTTON', {
        numTables: numInGroup,
      })}
    </div>
  </button>
);

export default withTranslation(TableCollapsedButton);
