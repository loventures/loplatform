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
  addProjectGraphNode,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
  getAllEditedOutEdges,
  getEditedAsset,
  insertProjectGraphNode,
} from '../../graphEdit';
import { openModal } from '../../modals/modalActions';
import { ModalIds } from '../../modals/modalIds';
import { Thunk } from '../../types/dcmState';
import { ResetContentStatusModalData } from './ResetContentStatusModal';
import AuthoringApiService from '../../services/AuthoringApiService.ts';
import { NewAsset } from '../../types/asset';

const nodeName = (contextPath: string): string =>
  contextPath.substring(1 + contextPath.lastIndexOf('.'));

export const editProjectStatusAction =
  (
    projectStatus: string | null,
    contentStatus: string | null | undefined,
    autosave: boolean
  ): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const statusedContent = graphEdits.contentTree.playlist.filter(
      contextPath =>
        getEditedAsset(nodeName(contextPath), projectGraph, graphEdits)?.data.contentStatus
    );
    dispatch(beginProjectGraphEdit('Edit project status'));
    if (
      getEditedAsset(projectGraph.rootNodeName, projectGraph, graphEdits).data.projectStatus !==
      projectStatus
    ) {
      dispatch(
        editProjectGraphNodeData(projectGraph.rootNodeName, {
          projectStatus,
        })
      );
    }
    if (contentStatus !== undefined) {
      if (
        getEditedAsset(projectGraph.homeNodeName, projectGraph, graphEdits).data.contentStatus !==
        contentStatus
      ) {
        dispatch(editProjectGraphNodeData(projectGraph.homeNodeName, { contentStatus }));
      }
      for (const contextPath of statusedContent) {
        const name = nodeName(contextPath);
        if (name !== projectGraph.homeNodeName)
          dispatch(editProjectGraphNodeData(name, { contentStatus: null }));
      }
    }
    if (autosave) dispatch(autoSaveProjectGraphEdits());
  };

export const setProjectStatusAction =
  (projectStatus: string | null): Thunk =>
  (dispatch, getState) => {
    const { configuration } = getState();
    const contentStatus = configuration.projectStatusMapping?.[projectStatus] ?? null;
    dispatch(
      openModal<ResetContentStatusModalData>(ModalIds.ResetContentStatus, {
        partial: false,
        courseStatus: configuration.contentStatuses[contentStatus] ?? 'Unset',
        callback: reset =>
          dispatch(editProjectStatusAction(projectStatus, reset ? contentStatus : undefined, true)),
      })
    );
  };

export const setContentStatusAction =
  (name: string, contextPath: string, contentStatus: string | null): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const prefix = contextPath ? `${contextPath}.${name}.` : `${name}.`;
    const statusedContent = graphEdits.contentTree.playlist.filter(
      contextPath =>
        contextPath.startsWith(prefix) &&
        getEditedAsset(nodeName(contextPath), projectGraph, graphEdits)?.data.contentStatus
    );
    const editContentStatus = (reset: boolean) => {
      dispatch(beginProjectGraphEdit('Edit content status'));
      dispatch(editProjectGraphNodeData(name, { contentStatus }));
      if (reset) {
        for (const contextPath of statusedContent) {
          dispatch(editProjectGraphNodeData(nodeName(contextPath), { contentStatus: null }));
        }
      }
      dispatch(autoSaveProjectGraphEdits());
    };
    if (statusedContent.length) {
      dispatch(
        openModal<ResetContentStatusModalData>(ModalIds.ResetContentStatus, {
          partial: true,
          callback: editContentStatus,
        })
      );
    } else {
      editContentStatus(false);
    }
  };

export const setCourseBannerImage =
  (file: File, andThen: () => void): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const home = projectGraph.homeNodeName;
    const edges = getAllEditedOutEdges(home, projectGraph, graphEdits);
    const existingImage = edges.find(e => e.group === 'image')?.targetName;

    const filename = file.name ?? 'Banner Image';
    AuthoringApiService.uploadToBlobstore(
      { typeId: 'image.1', data: { title: 'Banner Image' } },
      file
    )
      .then(blobData => {
        dispatch(beginProjectGraphEdit('Upload banner image'));
        if (existingImage) {
          dispatch(
            editProjectGraphNodeData(existingImage, {
              title: filename,
              source: blobData,
            })
          );
        } else {
          const addAsset: NewAsset<any> = {
            name: crypto.randomUUID(),
            typeId: 'image.1',
            data: {
              title: filename,
              source: blobData,
            },
          };
          dispatch(addProjectGraphNode(addAsset));
          dispatch(insertProjectGraphNode(addAsset.name, home, 'image'));
        }
        dispatch(autoSaveProjectGraphEdits());
        dispatch(andThen);
      })
      .catch(e => {
        console.log(e);
      });
  };
