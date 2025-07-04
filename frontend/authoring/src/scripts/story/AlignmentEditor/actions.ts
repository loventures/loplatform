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
  addProjectGraphEdge,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  computeEditedOutEdges,
  deleteProjectGraphEdge,
} from '../../graphEdit';
import { NodeName } from '../../types/asset';
import { Thunk } from '../../types/dcmState';
import { EdgeGroup, NewEdge } from '../../types/edge';

export const setAlignedAction =
  (name: NodeName, group: EdgeGroup, competencies: NodeName[], aligned: Set<NodeName>): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    if (
      competencies.length === aligned.size &&
      competencies.every(competency => aligned.has(competency))
    )
      return;
    dispatch(beginProjectGraphEdit('Edit alignment'));
    const outEdges = computeEditedOutEdges(name, group, projectGraph, graphEdits);
    const remove = outEdges.filter(c => !competencies.includes(c.targetName));
    for (const edge of remove) {
      dispatch(deleteProjectGraphEdge(edge));
    }
    const addEdges = competencies
      .filter(competency => !aligned.has(competency))
      .map<NewEdge>(competency => ({
        name: crypto.randomUUID(),
        sourceName: name,
        targetName: competency,
        group: group,
        traverse: false,
        data: {},
        newPosition: 'end',
      }));
    for (const edge of addEdges) {
      dispatch(addProjectGraphEdge(edge));
    }
    dispatch(autoSaveProjectGraphEdits());
  };
