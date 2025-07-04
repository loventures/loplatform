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

import {
  selectAssignmentsListUser,
  selectLearnerAssignmentsForCreditOnlyStatus,
} from '../../../commonPages/assignmentsPage/selectors/baseSelectors';
import { useCourseSelector } from '../../../loRedux';
import { LoCheckbox } from '../../../directives/LoCheckbox';
import ListMessages from '../../../listComponents/parts/ListMessages';
import ListPaginate from '../../../listComponents/parts/ListPaginate';
import ListSearch from '../../../listComponents/parts/ListSearch';
import ListSort from '../../../listComponents/parts/ListSort';
import { LoaderComponent } from '../../../utilities/withLoader';
import React from 'react';
import { useDispatch } from 'react-redux';

import {
  paginateActionCreator,
  searchActionCreator,
  sortActionCreator,
} from '../actions/listActions';
import { toggleForCreditActionCreator } from '../actions/toggleActions';
import { searchByProps, sortByProps } from '../config';
import { selectLearnerAssignmentsList } from '../selectors/listSelectors';
import LearnerAssignmentsListRows from './LearnerAssignmentsListRows';

const LearnerAssignmentsList: React.FC = () => {
  const { listState } = useCourseSelector(selectLearnerAssignmentsList);
  const viewingAs = useCourseSelector(selectAssignmentsListUser);
  const forCreditOnly = useCourseSelector(selectLearnerAssignmentsForCreditOnlyStatus);
  const dispatch = useDispatch();
  return (
    <div>
      <div className="card-list">
        <div className="card-list-filters">
          <div className="flex-row-content flex-wrap">
            <div>
              <LoCheckbox
                checkboxFor="learner-assignment-list-for-credit-only"
                checkboxLabel="LEARNER_ASSIGNMENT_LIST_CREDIT_FILTER"
                onToggle={() => dispatch(toggleForCreditActionCreator(viewingAs.id))}
                state={forCreditOnly}
              />
            </div>
            <ListSort
              activeSortKey={listState.activeOptions.sortKey}
              sortAction={(key: any) => dispatch(sortActionCreator(key, viewingAs.id))}
              sortByProps={sortByProps}
            ></ListSort>
            <ListSearch
              className="flex-col-fluid"
              activeSearchString={listState.activeOptions.searchString}
              searchAction={(searchString: any, searchConfig: any) =>
                dispatch(searchActionCreator(searchString, searchConfig, viewingAs.id))
              }
              searchByProps={searchByProps}
              ariaControls={undefined}
            ></ListSearch>
          </div>
        </div>

        <ListMessages
          listState={listState}
          emptyMessage="SEARCH_NO_RESULTS"
          filteredMessage="SEARCH_NO_RESULTS"
        />
      </div>

      <LoaderComponent
        loadingState={listState.loadingState}
        loadAction={() => dispatch(paginateActionCreator(1, viewingAs.id))}
      >
        <LearnerAssignmentsListRows />
      </LoaderComponent>

      <ListPaginate
        page={listState.activeOptions.currentPage}
        numPages={listState.activeOptions.totalPages}
        pageAction={page => dispatch(paginateActionCreator(page, viewingAs.id))}
      ></ListPaginate>
    </div>
  );
};

export default LearnerAssignmentsList;
