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

import { Reducer, Action } from 'redux';

import { INITIALIZE_DCM, UPDATE_BRANCH, UPDATE_ROLE } from '../dcmStoreConstants';

export interface LayoutState {
  branchId?: string;
  branchName?: string;
  project?: Project;
  userCanEdit: boolean; // you can edit this project, ignoring rôle
  userCanEditSettings: boolean; // you can edit the settings of this project, ignoring rôle
  probableAdmin: boolean; // a proxy for admin access
  role: string | null; // your rôle in this project
  platform?: any;
  preferencesOpen?: boolean;
}

export type ProjectType = 'Course' | 'Program';

export type Project = {
  id: number;
  name: string;
  projectType: ProjectType;
  homeNodeName: string;
  rootNodeName: string;
  ownedBy: number;
  contributedBy: Record<number, string | null>;
  archived: boolean;
  code?: string;
  productType?: string;
  category?: string;
  subCategory?: string;
  revision?: number;
  launchDate?: string;
  liveVersion?: string;
  s3?: string;
  maintenance: boolean;
};

export const defaultLayoutState: LayoutState = {
  userCanEdit: true,
  userCanEditSettings: true,
  probableAdmin: true,
  role: null,
};

export const PREFERENCES_OPEN = 'PREFERENCES_OPEN';

type LayoutAction =
  | {
      type: typeof INITIALIZE_DCM | typeof UPDATE_BRANCH;
      layout: LayoutState;
    }
  | {
      type: typeof UPDATE_ROLE;
      role: string | null;
    }
  | {
      type: typeof PREFERENCES_OPEN;
      open: boolean;
    };

const layoutReducer: Reducer<LayoutState, LayoutAction> = (state = defaultLayoutState, action) => {
  switch (action.type) {
    case INITIALIZE_DCM: {
      return {
        ...state,
        ...action.layout,
      };
    }
    case UPDATE_BRANCH: {
      return {
        ...state,
        ...action.layout,
      };
    }
    case UPDATE_ROLE: {
      return {
        ...state,
        role: action.role,
      };
    }
    case PREFERENCES_OPEN: {
      return {
        ...state,
        preferencesOpen: action.open ?? !state.preferencesOpen,
      };
    }
    default:
      return state;
  }
};

export default layoutReducer;
