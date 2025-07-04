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

import { setRemove, setToggle } from '../../gradebook/set';
import {
  QuestionsAndElements,
  beginProjectGraphEdit,
  deleteProjectGraphEdge,
  getAllEditedOutEdges,
  getContentTree,
  getEditedAsset,
  getFilteredContentList,
  autoSaveProjectGraphEdits,
} from '../../graphEdit';
import { NewAsset, NodeName } from '../../types/asset';
import { Thunk } from '../../types/dcmState';
import { toMultiWordRegex } from '../questionUtil';
import { plural } from '../story';

// So weird but allows me to defer accessing this state from the store until you
// search, but not store the results in the store.
export const navigationSearchAction =
  (search: string, setter: (found: Set<NodeName>) => void): Thunk =>
  (_, getState) => {
    const { layout, projectGraph, graphEdits } = getState();
    const home = getEditedAsset(projectGraph.homeNodeName, projectGraph, graphEdits);
    const contentTree = getContentTree(home, [], QuestionsAndElements, projectGraph, graphEdits);
    const accessRights = graphEdits.contentTree.accessRights;
    const regex = toMultiWordRegex(search);
    const predicate = (node: NewAsset<any>) =>
      layout.role && !accessRights[node.name].ViewContent ? '.' : regex.test(node.data.title);
    const contentList = getFilteredContentList(contentTree, predicate);
    setter(new Set(contentList.map(node => node.name).concat(home.name)));
  };

// implemented as a thunk with a setState so the callback function is stable
export const toggleSelectedAction =
  (name: NodeName, setter: (updater: (selected: Set<NodeName>) => Set<NodeName>) => void): Thunk =>
  (_, getState) => {
    const { projectGraph, graphEdits } = getState();
    const descendants = new Array<NodeName>();
    setter(selected => {
      const loop = (name: NodeName) => {
        if (selected.has(name)) {
          descendants.push(name);
        } else {
          for (const { group, targetName } of getAllEditedOutEdges(
            name,
            projectGraph,
            graphEdits
          )) {
            if (group === 'elements' || group === 'questions') loop(targetName);
          }
        }
      };
      if (!selected.has(name)) loop(name);
      return setToggle(setRemove(selected, descendants), name);
    });
  };

export const deleteAssetsAction =
  (names: Set<NodeName>): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    dispatch(beginProjectGraphEdit(`Delete ${plural(names.size, 'asset')}`));
    const loop = (name: NodeName) => {
      for (const edge of getAllEditedOutEdges(name, projectGraph, graphEdits)) {
        if (edge.group === 'elements' || edge.group === 'questions') {
          if (names.has(edge.targetName)) {
            dispatch(deleteProjectGraphEdge(edge));
          } else {
            loop(edge.targetName);
          }
        }
      }
    };
    loop(projectGraph.homeNodeName);
    dispatch(autoSaveProjectGraphEdits());
  };
