/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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
import { useRef } from 'react';
import { connect } from 'react-redux';
import { CSSTransition, TransitionGroup } from 'react-transition-group';

import { collapseTableAC, expandTablesAC } from '../actions/tableOptionsActions';
import { selectGradebookTableStructure } from '../selectors/tableDataSelectors';
import GradebookTablesScroller from './controls/GradebookTablesScroller';
import TableCollapsedButton from './controls/TableCollapsedButton';
import CategoryTable from './tables/CategoryTable';
import LearnersTable from './tables/LearnersTable';
import OverallTable from './tables/OverallTable';

// Owns its own nodeRef (these live in a .map, so we can't call useRef per item
// inline) and forwards it to the button — react-transition-group needs nodeRef
// under React 19. Spreads the TransitionGroup-injected props onto CSSTransition.
interface CollapsedButtonTransitionProps {
  numInGroup: number;
  expandTables: () => void;
  [key: string]: any;
}

const CollapsedButtonTransition = ({
  numInGroup,
  expandTables,
  ...transitionProps
}: CollapsedButtonTransitionProps) => {
  const nodeRef = useRef(null);
  return (
    <CSSTransition
      nodeRef={nodeRef}
      className="align-self-start"
      classNames="gradebook-table-collapsed-button"
      timeout={500}
      {...transitionProps}
    >
      <TableCollapsedButton
        ref={nodeRef}
        numInGroup={numInGroup}
        expandTables={expandTables}
      />
    </CSSTransition>
  );
};

interface GradebookTablesProps {
  gradebookStructure: any[];
  totalWeight: number;
  collapseTable: (index: number) => void;
  expandTableRange: (start: number, end: number) => void;
}

const GradebookTables = ({
  gradebookStructure,
  totalWeight,
  collapseTable,
  expandTableRange,
}: GradebookTablesProps) => (
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
                  <CollapsedButtonTransition
                    key={'collapse-' + index}
                    numInGroup={numCollapsedWithColumns}
                    expandTables={() => expandTableRange(index, endIndex)}
                  />
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
