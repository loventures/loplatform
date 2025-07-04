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
import { sortBy } from 'lodash';
import React, { useCallback, useEffect, useMemo, useRef } from 'react';
import { IoAddOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  deleteProjectGraphEdge,
  editProjectGraphEdgeData,
  useAllEditedOutEdges,
  useGraphEditSelector,
} from '../graphEdit';
import { FakeEdge } from '../graphEdit/graphEditReducer';
import { NodeName, TypeId } from '../types/asset';
import { EdgeGroup, NewEdge } from '../types/edge';
import { addContentGateAction } from './GateEditor/actions';
import { GateRow } from './GateEditor/GateRow';
import { scrollBottomIntoView } from './story';
import { useIsEditable, useNarrativeAssetState } from './storyHooks';

export const GateEditor: React.FC<{ name: NodeName; typeId: TypeId; group: EdgeGroup }> = ({
  name,
  group,
}) => {
  const dispatch = useDispatch();
  const editMode = useIsEditable(name, 'EditSettings');
  const allEdges = useAllEditedOutEdges(name);
  const contextPaths = useGraphEditSelector(state => state.contentTree.contextPaths);
  const playlist = useGraphEditSelector(state => state.contentTree.playlist);
  const gates = useMemo(
    () =>
      sortBy(
        allEdges.filter(edge => edge.group === group),
        edge => playlist.indexOf(contextPaths[edge.targetName] + '.' + edge.targetName)
      ),
    [allEdges, group, playlist, contextPaths]
  );

  const { gated: justGated } = useNarrativeAssetState(name);
  const divRef = useRef<HTMLDivElement>();

  const onAddGate = () => dispatch(addContentGateAction(name, group));

  const onDeleteGate = (gate: NewEdge | FakeEdge) => {
    dispatch(
      beginProjectGraphEdit(group === 'testsOut' ? 'Delete test out' : 'Delete content gate')
    );
    dispatch(deleteProjectGraphEdge(gate as NewEdge));
    dispatch(autoSaveProjectGraphEdits());
  };

  const onEditGate = (gate: NewEdge | FakeEdge, threshold: number) => {
    const typeStr = group === 'testsOut' ? 'test out' : 'gate';
    dispatch(beginProjectGraphEdit(`Edit ${typeStr} percentage`, `${gate.targetName}:percent`));
    dispatch(
      editProjectGraphEdgeData(name, (gate as NewEdge).name, {
        performanceGate: { threshold },
      })
    );
    // autosave in the blur
  };

  const onBlur = useCallback(() => dispatch(autoSaveProjectGraphEdits()), []);

  const hasValidGates = gates.some(gate => contextPaths[gate.targetName]);

  useEffect(() => {
    if (justGated && hasValidGates) scrollBottomIntoView(divRef.current);
  }, [justGated, hasValidGates]);

  return hasValidGates ? (
    <div
      className={classNames(
        'gater d-flex flex-row-reverse align-items-center',
        editMode && 'edit-mode'
      )}
      ref={divRef}
    >
      {editMode && (
        <Button
          className="d-flex align-items-center justify-content-center flex-shrink-0 p-1 mini-button"
          color="warning"
          outline
          onClick={onAddGate}
        >
          <IoAddOutline />
        </Button>
      )}
      <div className="d-flex flex-column flex-grow-1 minw-0">
        {gates.map(gate => (
          <GateRow
            key={gate.targetName}
            gate={gate}
            editMode={editMode}
            onDeleteGate={() => onDeleteGate(gate)}
            onEditGate={threshold => onEditGate(gate, threshold)}
            onBlur={onBlur}
          />
        ))}
      </div>
    </div>
  ) : null;
};
