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

import { push } from 'connected-react-router';

import { trackAuthoringEvent } from '../../analytics';
import edgeRules from '../../editor/EdgeRuleConstants';
import {
  confirmSaveProjectGraphEdits,
  getAllEditedOutEdges,
  getEditedAsset,
  suppressPromptForUnsavedGraphEdits,
} from '../../graphEdit';
import { NodeName, TypeId } from '../../types/asset';
import { Thunk } from '../../types/dcmState';
import { EdgeGroup } from '../../types/edge';
import { childEdgeGroup, editorUrl } from '../story';
import { setNarrativeState } from '../storyActions';
import gretchen from '../../grfetchen/';
import { openToast, TOAST_TYPES } from '../../toast/actions.ts';

export type PreviewRole = 'Learner' | 'Instructor';

export const previewCourseContent =
  (
    newTab: boolean,
    branchId: number,
    courseName: string,
    edgeNames: string,
    role: PreviewRole | null
  ): Thunk =>
  dispatch => {
    const win = newTab ? window.open('', '') : null;
    // if you studo straight to instructor/learner from authoring, set a return URL
    if (!newTab)
      sessionStorage.setItem('returnUrl', document.location.pathname + document.location.search);
    gretchen
      .post('/api/v2/lwc/preview/url')
      .data({ branchId, courseName, edgeNames, role })
      .exec()
      .then(url => ((win || window).location = url))
      .catch(() => dispatch(openToast('Preview failed', TOAST_TYPES.DANGER)));
  };

export const previewAction = (
  name: NodeName,
  contextPath: string,
  role: PreviewRole | null,
  newTab: boolean
): Thunk =>
  confirmSaveProjectGraphEdits((dispatch, getState) => {
    const {
      projectGraph: { branchId, homeNodeName, outEdgesByNode, edges },
    } = getState();
    const path = contextPath ? [...contextPath.split('.'), name] : [name];
    const edgePath = new Array<string>();
    for (let i = 0; i < path.length - 1; ++i) {
      const from = path[i],
        to = path[i + 1];
      const edge = outEdgesByNode[from]
        ?.map(name => edges[name])
        ?.find(
          edge => edge?.sourceName == from && edge?.targetName == to && edge?.group === 'elements'
        );
      if (edge != null) edgePath.push(edge.name);
    }
    trackAuthoringEvent('Narrative Editor - Preview', role ?? 'Author');
    if (newTab) suppressPromptForUnsavedGraphEdits(true);
    dispatch(previewCourseContent(newTab, branchId, homeNodeName, edgePath.join('.'), role));
  });

export const revisionHistoryAction = (name: NodeName, typeId: TypeId, contextPath: string): Thunk =>
  confirmSaveProjectGraphEdits((dispatch, getState) => {
    const { branchId } = getState().projectGraph;
    trackAuthoringEvent('Narrative Editor - Revision History', typeId);
    // Ugh delay this so the redux state has updated.. Scenario: I edit the title,
    // click on View Revision History, it prompts me to save, I choose Discard, then
    // it prompts me again by way of the PreventNavAndUCP because the above dispatch
    // has not yet hit the store.
    setTimeout(() => dispatch(push(editorUrl('revision', branchId, name, contextPath))), 0);
  });

// This is disabled at the course level because it tends to esplode the browser.
export const expandAllAction =
  (name: NodeName): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits, story } = getState();
    const assetStates = { ...story.assetStates };
    assetStates[name] = { ...(assetStates[name] ?? {}), expanded: true };
    const loop = (name: NodeName, group: EdgeGroup) => {
      const typeId = getEditedAsset(name, projectGraph, graphEdits)?.typeId;
      if (edgeRules[typeId]?.[group]) {
        assetStates[name] = { ...(assetStates[name] ?? {}), expanded: true };
        for (const { group, targetName } of getAllEditedOutEdges(name, projectGraph, graphEdits)) {
          if (group === 'elements') loop(targetName, group);
        }
      }
    };
    const group = childEdgeGroup(getEditedAsset(name, projectGraph, graphEdits)?.typeId);
    loop(name, group);
    dispatch(setNarrativeState({ inlineView: true, assetStates }));
  };

// This is disabled at the course level because it tends to esplode the browser.
export const expandParentAction =
  (name: NodeName, contextPath: string): Thunk =>
  (dispatch, getState) => {
    const index = contextPath.lastIndexOf('.');
    const parentName = contextPath.substring(1 + index);
    const parentContext = index < 0 ? undefined : contextPath.substring(0, index);
    const { story, projectGraph } = getState();
    const assetStates = { ...story.assetStates };
    assetStates[parentName] = {
      ...(assetStates[parentName] ?? {}),
      expanded: true,
      renderAll: true,
    };
    const scrollTo = contextPath ? `${contextPath}.${name}` : name;
    dispatch(setNarrativeState({ inlineView: true, assetStates, scrollTo }));
    dispatch(push(editorUrl('story', projectGraph.branchId, parentName, parentContext)));
  };

export const expandDescendantAction =
  (name: NodeName, contextPath: string): Thunk =>
  (dispatch, getState) => {
    const { story } = getState();
    const assetStates = { ...story.assetStates };
    for (const ancestorName of contextPath.split('.')) {
      assetStates[ancestorName] = {
        ...(assetStates[ancestorName] ?? {}),
        expanded: true,
        renderAll: true,
      };
    }
    const scrollTo = contextPath ? `${contextPath}.${name}` : name;
    dispatch(setNarrativeState({ inlineView: true, assetStates, scrollTo }));
  };
