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

import { trackNarrativeAdd } from '../../analytics/AnalyticsEvents';
import {
  addProjectGraphEdge,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  computeEditedOutEdges,
  deleteProjectGraphEdge,
  getCopiedAsset,
  getEditedAsset,
} from '../../graphEdit';
import { noGraphEdits } from '../../graphEdit/graphEditReducer';
import { FindContentModalData } from '../../modals/FindContentModal';
import { openModal } from '../../modals/modalActions';
import { ModalIds } from '../../modals/modalIds';
import { loadRemoteProjectGraph } from '../../modals/narrative/ProjectContentSelector';
import { addAsset, cloneAsset } from '../../story/AddAsset';
import { editorUrl } from '../../story/story';
import { CopiedAsset } from '../../story/storyReducer';
import { fetchStructure } from '../../structurePanel/projectGraphActions';
import { NodeName } from '../../types/asset';
import { Thunk } from '../../types/dcmState';
import { NewEdge } from '../../types/edge';

export const addSurveyAction =
  (name: NodeName, contextPath: string): Thunk =>
  (dispatch, getState) => {
    const {
      projectGraph,
      graphEdits,
      configuration: { translations },
    } = getState();
    const { branchId } = projectGraph;
    const asset = getEditedAsset(name, projectGraph, graphEdits);
    trackNarrativeAdd('survey.1');
    const survey = addAsset(
      'survey.1',
      asset,
      undefined,
      undefined,
      'survey',
      translations,
      dispatch
    );
    dispatch(push(editorUrl('story', branchId, survey, contextPath, { confirm: false })));
  };

export const findSurveyAction =
  (name: NodeName, contextPath: string): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const { branchId } = projectGraph;
    const asset = getEditedAsset(name, projectGraph, graphEdits);
    const modalData: FindContentModalData = {
      mode: 'survey',
      parent: asset,
      link: (branch, names) => {
        // This never happens because linking is questionable and so we don't show the button
        dispatch(beginProjectGraphEdit('Link survey'));
        const surveyName = names[0];
        const newEdge: NewEdge = {
          name: crypto.randomUUID(),
          sourceName: asset.name,
          targetName: surveyName,
          group: 'survey',
          data: {},
          traverse: true,
          newPosition: 'end',
        };
        dispatch(addProjectGraphEdge(newEdge));
        dispatch(autoSaveProjectGraphEdits());
        // The FE does not have the full workspace. If the new survey is not know, load it.
        if (!projectGraph.nodes[surveyName]) {
          dispatch(fetchStructure(surveyName, undefined, true));
        }
        dispatch(push(editorUrl('story', branchId, surveyName, contextPath, { confirm: false })));
      },
      clone: (project, names) => {
        dispatch(beginProjectGraphEdit('Clone survey'));
        const surveyName = names[0];
        let surveyPromise: Promise<CopiedAsset>;
        if (project.branchId === branchId && projectGraph.nodes[surveyName]) {
          // If it's a known local asset clone it from the project graph
          surveyPromise = Promise.resolve(getCopiedAsset(surveyName, projectGraph, graphEdits));
        } else {
          // If it's a foreign asset, clone it from the remote project graph
          surveyPromise = loadRemoteProjectGraph(project).then(projectGraph =>
            getCopiedAsset(surveyName, projectGraph, noGraphEdits)
          );
        }
        surveyPromise.then(original => {
          const copied = cloneAsset(original, dispatch, original.data.title);
          const newEdge: NewEdge = {
            name: crypto.randomUUID(),
            sourceName: asset.name,
            targetName: copied.name,
            group: 'survey',
            data: {},
            traverse: true,
            newPosition: 'end',
          };
          dispatch(addProjectGraphEdge(newEdge));
          dispatch(autoSaveProjectGraphEdits());
          dispatch(
            push(editorUrl('story', branchId, copied.name, contextPath, { confirm: false }))
          );
        });
      },
    };
    dispatch(openModal(ModalIds.FindContentModal, modalData));
  };

export const removeSurveyAction =
  (name: NodeName): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const surveyEdge = computeEditedOutEdges(name, 'survey', projectGraph, graphEdits)[0];
    dispatch(beginProjectGraphEdit('Remove survey'));
    dispatch(deleteProjectGraphEdge(surveyEdge));
    dispatch(autoSaveProjectGraphEdits());
  };
