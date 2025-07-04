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

import { Dispatch, SetStateAction } from 'react';

import edgeRuleConstants from '../editor/EdgeRuleConstants';
import {
  addProjectGraphEdge,
  addProjectGraphNode,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  deleteProjectGraphEdge,
  getAllEditedInEdges,
  getContentTree,
  getFilteredContentList,
} from '../graphEdit';
import { getRemoveCompetencyConfirmConfig } from '../modals/confirmHelper';
import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';
import { newAssetData } from '../story/questionUtil';
import { storyTypeName } from '../story/story';
import { EdgePosition } from '../types/api';
import { NodeName } from '../types/asset';
import { Thunk } from '../types/dcmState';
import { EdgeGroup, NewAssetWithEdge } from '../types/edge';
import * as T from '../types/typeIds';
import { Level1Competency } from '../types/typeIds';
import { CompetencyTreeEdgeGroups } from './useFlatCompetencies';

export const addCompetencyAction =
  (
    competency: NewAssetWithEdge<any>,
    sibling: boolean,
    setAddedItem: Dispatch<SetStateAction<string>>,
    resetCompetencyTree: () => void
  ): Thunk =>
  dispatch => {
    let parent: NodeName,
      group: EdgeGroup,
      typeId: T.Level1Competency | T.Level2Competency | T.Level3Competency,
      newPosition: EdgePosition;
    if (sibling) {
      parent = competency.edge.sourceName;
      group = competency.edge.group;
      typeId = competency.typeId;
      newPosition = { after: competency.edge.name };
    } else {
      parent = competency.name;
      const rules = edgeRuleConstants[competency.typeId];
      group = competency.typeId === Level1Competency ? 'level2Competencies' : 'level3Competencies';
      typeId = rules[group][0];
      newPosition = 'start';
    }
    dispatch(beginProjectGraphEdit(`Add competency`));
    const addAsset = {
      name: crypto.randomUUID(),
      typeId: typeId,
      data: {
        ...newAssetData(typeId),
      },
    };
    dispatch(addProjectGraphNode(addAsset));
    const edge = {
      name: crypto.randomUUID(),
      sourceName: parent,
      targetName: addAsset.name,
      group: group,
      traverse: true,
      remote: null,
      data: {},
      newPosition,
    };
    dispatch(addProjectGraphEdge(edge));
    // no autosave because we'll autosave on blur
    setAddedItem(addAsset.name);
    resetCompetencyTree();
  };

export const removeCompetencyAction =
  (competency: NewAssetWithEdge<any>, resetCompetencyTree: () => void): Thunk =>
  (dispatch, getState) => {
    const {
      projectGraph,
      graphEdits,
      configuration: { translations: polyglot },
    } = getState();
    const childrenTree = getContentTree(
      competency,
      [],
      CompetencyTreeEdgeGroups,
      projectGraph,
      graphEdits
    );
    const linearChildren = getFilteredContentList(childrenTree);
    const competencyNames = [...linearChildren, competency].map(n => n.name);
    const allAlignments = competencyNames.flatMap(name =>
      getAllEditedInEdges(name, projectGraph, graphEdits).filter(
        edge =>
          !projectGraph.assetBranches[edge.sourceName] &&
          (edge.group === 'teaches' || edge.group === 'assesses')
      )
    );

    const removeConfirmConfig = getRemoveCompetencyConfirmConfig(
      polyglot,
      competency.data.title,
      linearChildren.length,
      () => {
        dispatch(beginProjectGraphEdit(`Delete ${storyTypeName(polyglot, competency.typeId)}`));
        allAlignments.forEach(edge => {
          dispatch(deleteProjectGraphEdge(edge));
        });
        dispatch(deleteProjectGraphEdge(competency.edge));
        dispatch(autoSaveProjectGraphEdits());
        resetCompetencyTree();
      }
    );
    dispatch(openModal(ModalIds.Confirm, removeConfirmConfig));
  };
