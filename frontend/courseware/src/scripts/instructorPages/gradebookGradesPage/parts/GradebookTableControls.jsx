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

import { LoCheckbox } from '../../../directives/LoCheckbox';
import { withTranslation } from '../../../i18n/translationContext';
import { InactiveFilter } from '../../../listComponents/parts/InactiveFilter';
import ListSearch from '../../../listComponents/parts/ListSearch';
import ListSort from '../../../listComponents/parts/ListSort';
import { connect } from 'react-redux';

import { searchActionCreator, sortActionCreator } from '../actions/tableActions';
import {
  setGradeDisplayMethodAC,
  toggleExternalIdsAC,
  toggleForCreditOnlyAC,
} from '../actions/tableOptionsActions';
import { searchByProps, sortByProps } from '../config';
import { selectTableListControlComponent } from '../selectors/tableDataSelectors';

const GradebookTableControls = ({
  translate,
  listState,
  sort,
  search,
  showExternalIds,
  showForCreditOnly,
  gradeDisplayMethod,
  toggleShowExternalIds,
  toggleShowForCreditOnly,
  setGradeDisplayMethod,
}) => {
  const inactive = !!listState.activeOptions.options?.inactive;
  return (
    <div className="gradebook-table-controls mb-3">
      <div className="flex-row-content flex-wrap">
        <div className="lo-select-wrap">
          <select
            className="form-control"
            aria-label={translate('GRADEBOOK_DISPLAY')}
            value={gradeDisplayMethod}
            onChange={event => setGradeDisplayMethod(event.target.value)}
          >
            <option value={'percentSign'}>{translate('GRADEBOOK_DISPLAY_PERCENT')}</option>
            <option value={'pointsOutOf'}>{translate('GRADEBOOK_DISPLAY_POINTS')}</option>
          </select>
        </div>

        <div>
          <LoCheckbox
            checkboxFor="gradebook-tables-external-ids-toggle"
            checkboxLabel="GRADEBOOK_DISPLAY_EXTERNAL_ID"
            onToggle={toggleShowExternalIds}
            state={showExternalIds}
          />
        </div>

        <div>
          <LoCheckbox
            checkboxFor="gradebook-tables-for-credit-toggle"
            checkboxLabel="GRADEBOOK_DISPLAY_CREDIT_ONLY"
            onToggle={toggleShowForCreditOnly}
            state={showForCreditOnly}
          />
        </div>

        <div>
          <ListSort
            activeSortKey={listState.activeOptions.sortKey}
            sortAction={sort}
            sortByProps={sortByProps}
          ></ListSort>
        </div>

        <div
          className="d-flex flex-grow-1"
          style={{ minWidth: '20rem' }}
        >
          <ListSearch
            className="flex-col-fluid gradebook-search flex-grow-1"
            activeSearchString={listState.activeOptions.searchString}
            searchAction={search}
            searchByProps={searchByProps(inactive)}
          ></ListSearch>
          <InactiveFilter
            inactive={inactive}
            toggle={() =>
              search(
                listState.activeOptions.searchString,
                searchByProps(!inactive).SEARCH_BY_USER_ALL
              )
            }
          />
        </div>
      </div>
    </div>
  );
};

export default connect(selectTableListControlComponent, {
  sort: sortActionCreator,
  search: searchActionCreator,
  toggleShowExternalIds: toggleExternalIdsAC,
  toggleShowForCreditOnly: toggleForCreditOnlyAC,
  setGradeDisplayMethod: setGradeDisplayMethodAC,
})(withTranslation(GradebookTableControls));
