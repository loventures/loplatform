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

import { SET_DROPBOX_STATE } from './dropboxActions';
import { DropboxFileDto } from './dropboxApi';

export type DropboxDirectory = {
  directory?: DropboxFileDto; // undefined for root
  path: DropboxFileDto[]; // path to this directory
  subdirectories: DropboxFileDto[]; // direct subdirectories
};

export interface DropboxState {
  filter: string;
  archived: boolean;
  directories: Record<number, DropboxDirectory>;
}

const initialState: DropboxState = {
  filter: '',
  archived: false,
  directories: {},
};

export default function dropboxReducer(state: DropboxState = initialState, action): DropboxState {
  switch (action.type) {
    case SET_DROPBOX_STATE: {
      const { state: update } = action;
      return {
        ...state,
        ...update,
      };
    }

    default:
      return state;
  }
}
