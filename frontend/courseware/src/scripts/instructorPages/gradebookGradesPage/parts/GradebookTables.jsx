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

import VariableWidthTransition from '../../../components/transitions/VariableWidthTransition';
import { filter, findIndex, map } from 'lodash';
import { connect } from 'react-redux';
import { CSSTransition, TransitionGroup } from 'react-transition-group';

import { collapseTableAC, expandTablesAC } from '../actions/tableOptionsActions';
import { selectGradebookTableStructure } from '../selectors/tableDataSelectors';
import GradebookTablesScroller from './controls/GradebookTablesScroller';
import TableCollapsedButton from './controls/TableCollapsedButton';
import CategoryTable from './tables/CategoryTable';
import LearnersTable from './tables/LearnersTable';
import OverallTable from './tables/OverallTable';

const GradebookTables = ({ gradebookStructure, totalWeight, collapseTable, expandTableRange }) => (
  <div>
    <div className="gradebook-tables-panel">
      <LearnersTable />
      <OverallTable totalWeight={totalWeight} />
      <GradebookTablesScroller>
        <TransitionGroup className="grade-tables">
          {map(gradebookStructure, (categoryStructure, index) => {
            if (categoryStructure.visible) {
              return (
                <VariableWidthTransition
                  key={categoryStructure.categoryId}
                  classNames="gradebook-table-transition"
                  timeout={500}
                >
                  <CategoryTable
                    categoryId={categoryStructure.categoryId}
                    categoryTitle={categoryStructure.categoryTitle}
                    columnIds={categoryStructure.columnIds}
                    weight={categoryStructure.weight}
                    totalWeight={totalWeight}
                    collapseTable={() => collapseTable(index)}
                  />
                </VariableWidthTransition>
              );
            } else if (index === 0 || gradebookStructure[index - 1].visible) {
              const nextVisible = findIndex(gradebookStructure, s => s.visible, index);
              const endIndex = nextVisible === -1 ? gradebookStructure.length : nextVisible;
              const numCollapsedWithColumns = filter(
                gradebookStructure.slice(index, endIndex),
                s => s.columnIds.length > 0
              ).length;
              if (numCollapsedWithColumns === 0) {
                return null;
              } else {
                return (
                  <CSSTransition
                    key={'collapse-' + index}
                    className="align-self-start"
                    classNames="gradebook-table-collapsed-button"
                    timeout={500}
                  >
                    <TableCollapsedButton
                      numInGroup={numCollapsedWithColumns}
                      expandTables={() => expandTableRange(index, endIndex)}
                    />
                  </CSSTransition>
                );
              }
            } else {
              return null;
            }
          })}
        </TransitionGroup>
      </GradebookTablesScroller>
    </div>
  </div>
);

export default connect(selectGradebookTableStructure, {
  collapseTable: collapseTableAC,
  expandTableRange: expandTablesAC,
})(GradebookTables);
