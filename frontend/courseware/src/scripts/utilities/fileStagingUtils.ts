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

import { StagedFile } from '../api/fileUploadApi.ts';
import { findIndex, flatten, map, without } from 'lodash';
import { NGAttachmentService } from '../services/AttachmentService';
import { Dispatch, Reducer } from 'redux';
import { batchActions } from 'redux-batched-actions';
import { lojector } from '../loject.ts';

export const FILE_STAGING_SELECTED = 'FILE_STAGING_SELECTED';
export const FILE_STAGING_PROGRESS = 'FILE_STAGING_PROGRESS';
export const FILE_STAGING_SUCCESS = 'FILE_STAGING_SUCCESS';
export const FILE_STAGING_ERROR = 'FILE_STAGING_ERROR';

const fileSelectedActionCreator = (config = {}, objectUrl: string, name: string) => ({
  ...config,
  type: FILE_STAGING_SELECTED,
  data: { selectedFile: { objectUrl, name } },
});

const stagingProgressActionCreator = (config = {}, objectUrl: string, progress: any) => ({
  ...config,
  type: FILE_STAGING_PROGRESS,
  data: { objectUrl, progress },
});

const stagingSuccessActionCreator = (config = {}, objectUrl: string) => ({
  ...config,
  type: FILE_STAGING_SUCCESS,
  data: { objectUrl },
});

const stagingErrorActionCreator = (
  config = {},
  objectUrl: string,
  error = 'FILE_STAGING_ERROR'
) => ({
  ...config,
  type: FILE_STAGING_ERROR,
  data: { objectUrl, error },
});

export const fileStagingActionCreatorMaker = (
  additionalSuccessACs: any[] = [],
  configMapper: (...args: any[]) => Record<any, any> = () => ({})
) => {
  return (file: File, ...args: any[]) => {
    return (dispatch: Dispatch) => {
      const configForAC = configMapper(...args);

      const objectUrl = URL.createObjectURL(file);

      dispatch(fileSelectedActionCreator(configForAC, objectUrl, file.name));

      const onSuccess = (stagedFile: StagedFile) => {
        const actions = flatten(map(additionalSuccessACs, ac => ac(stagedFile, ...args)));
        dispatch(batchActions([stagingSuccessActionCreator(configForAC, objectUrl), ...actions]));
      };

      const onError = (error: any) => {
        dispatch(stagingErrorActionCreator(configForAC, objectUrl, error));
      };

      const onProgress = (progress: any) => {
        dispatch(stagingProgressActionCreator(configForAC, objectUrl, progress));
      };

      const AttachmentService: NGAttachmentService = lojector.get('AttachmentService');

      AttachmentService.uploadStaging(file).then(onSuccess, onError, onProgress);
    };
  };
};

const getIndex = (objectUrl: string, files: UnstagedFile[]) => {
  const index = findIndex(files, file => file.objectUrl === objectUrl);
  if (index < 0) {
    console.error('Failure to find', objectUrl, 'in', files);
    return 0;
  }
  return index;
};

export function humanFileSize(size: number): string {
  const i = size == 0 ? 0 : Math.floor(Math.log2(size) / 10);
  return Number((size / Math.pow(1024, i)).toFixed(2)) * 1 + ' ' + ['B', 'kB', 'MB', 'GB', 'TB'][i];
}

/** The File Staging State uses array index to keep track of error/progress info for each file.
 *  progresses[1] and errors[1] would refer to the second unstaged file, i.e. unstagedFiles[1].
 * */
type UnstagedFile = { name: string; objectUrl: string };

export interface FileStagingState {
  unstagedFiles: UnstagedFile[];
  progresses: { percent?: number }[];
  errors: any[];
}

export const fileStagingInitialState: FileStagingState = {
  unstagedFiles: [],
  progresses: [],
  errors: [],
};

export const fileStagingReducer: Reducer<FileStagingState> = function (
  state = fileStagingInitialState,
  action
) {
  switch (action.type) {
    case FILE_STAGING_SELECTED:
      return {
        ...state,
        unstagedFiles: state.unstagedFiles.concat(action.data.selectedFile),
        progresses: state.progresses.concat({ percent: 0 }),
      };
    case FILE_STAGING_PROGRESS: {
      const index = getIndex(action.data.objectUrl, state.unstagedFiles);
      return {
        ...state,
        progresses: [
          ...state.progresses.slice(0, index),
          action.data.progress,
          ...state.progresses.slice(index + 1),
        ],
      };
    }
    case FILE_STAGING_ERROR: {
      const index = getIndex(action.data.objectUrl, state.unstagedFiles);
      return {
        ...state,
        errors: [
          ...state.errors.slice(0, index),
          action.data.error,
          ...state.errors.slice(index + 1),
        ],
      };
    }
    case FILE_STAGING_SUCCESS: {
      const index = getIndex(action.data.file, state.unstagedFiles);
      return {
        ...state,
        unstagedFiles: without(state.unstagedFiles, state.unstagedFiles[index]),
        progresses: without(state.progresses, state.progresses[index]),
        errors: without(state.errors, state.errors[index]),
      };
    }
    default:
      return state;
  }
};
