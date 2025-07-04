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

import gretchen from '../../grfetchen/';

import { reloadAssetEditor } from '../../editor/assetEditorActions';
import { BranchLinkModalData } from '../../modals/BranchLinkModal';
import { ConfirmationTypes } from '../../modals/ConfirmModal';
import { openModal } from '../../modals/modalActions';
import { ModalIds } from '../../modals/modalIds';
import AuthoringOpsService from '../../services/AuthoringOpsService';
import { openToast } from '../../toast/actions';
import { WriteOp } from '../../types/api';
import { Thunk } from '../../types/dcmState';
import { addDependencies, ProjectResponse } from '../NarrativeMultiverse';

const syncDependencies = (projectId: number, ids: number[]): Promise<void> =>
  gretchen.post(`/api/v2/authoring/projects/${projectId}/dependencies/sync`).data({ ids }).exec();

export const synchronizeBranchAction =
  (branch: ProjectResponse, setLoading: (loading: boolean) => void): Thunk =>
  (dispatch, getState) => {
    const { projectGraph } = getState();
    const { branchId } = projectGraph;
    setLoading(true);
    syncDependencies(branchId, [branch.branchId])
      .then(() => {
        dispatch(reloadAssetEditor());
      })
      .catch(e => {
        console.log(e);
        dispatch(openToast('Sync failed.', 'danger'));
        throw e;
      })
      .finally(() => setLoading(false));
  };

export const synchronizeAllAction =
  (branches: ProjectResponse[], setLoading: (loading: boolean) => void): Thunk =>
  (dispatch, getState) => {
    const { projectGraph } = getState();
    const { branchCommits, branchId } = projectGraph;

    const branchIds = branches
      .filter(branch => branch.headId !== branchCommits[branch.branchId])
      .map(branch => branch.branchId);
    setLoading(true);
    syncDependencies(branchId, branchIds)
      .then(() => {
        dispatch(reloadAssetEditor());
      })
      .catch(e => {
        console.log(e);
        dispatch(openToast('Sync failed.', 'danger'));
        throw e;
      })
      .finally(() => setLoading(false));
  };

export const unlinkBranchAction =
  (branch: ProjectResponse): Thunk =>
  (dispatch, getState) => {
    const { projectGraph } = getState();
    // TODO: Support this in layered...
    const ops = Object.keys(projectGraph.nodes)
      .filter(name => !projectGraph.assetBranches[name])
      .flatMap<WriteOp>((/*name*/) => {
        // const edgeNames = projectGraph.outEdgesByNode[name] ?? [];
        // const edges = edgeNames.map(name => projectGraph.edges[name]);
        // const remoteEdges = edges.filter(e => e.remote === branch.branchId);
        // if (remoteEdges.length == 0) {
        //   return [];
        // } else {
        //   const deletes = remoteEdges.map<DeleteEdgeWriteOp>(edge => ({
        //     op: 'deleteEdge',
        //     name: edge.name,
        //   }));
        //   const groups = uniq(remoteEdges.map(edge => edge.group));
        //   const orders = groups.map<SetEdgeOrderWriteOp>(group => ({
        //     op: 'setEdgeOrder',
        //     sourceName: name,
        //     group: group,
        //     ordering: edges
        //             .filter(edge => edge.group === group && edge.remote !== branch.branchId)
        //             .map(edge => edge.name),
        //   }));
        //   return [...deletes, ...orders];
        // }
        return [];
      });
    dispatch(
      openModal(ModalIds.Confirm, {
        confirmationType: ConfirmationTypes.DeleteLink,
        color: 'danger',
        words: {
          header: 'Delete Remote Course?',
          body: `This will delete all links from this course to any content in ${branch.branchName}.`,
          confirm: 'Delete',
        },
        confirmCallback: () =>
          AuthoringOpsService.postWriteOps(ops)
            .then(() => {
              dispatch(reloadAssetEditor());
              dispatch(openToast(`Unlinked ${branch.project.name}.`, 'success'));
            })
            .catch(e => {
              console.log(e);
              dispatch(openToast('Unlink failed.', 'danger'));
              throw e;
            }),
      })
    );
  };

export const linkBranchAction = (): Thunk => (dispatch, getState) => {
  const { projectGraph } = getState();
  const { branchId } = projectGraph;
  const modalData: BranchLinkModalData = {
    callback: branch => {
      return addDependencies(branchId, [branch.branchId])
        .then(() => {
          dispatch(reloadAssetEditor());
        })
        .catch(e => {
          console.log(e);
          dispatch(openToast('Link failed.', 'danger'));
          throw e;
        });
    },
  };
  dispatch(openModal(ModalIds.BranchLinkModal, modalData));
};
