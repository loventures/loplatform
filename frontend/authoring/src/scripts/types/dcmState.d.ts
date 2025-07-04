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

import { ThunkAction } from 'redux-thunk';

import { SidePanelState } from '../assetSidePanel/assetSidePanelReducer';
import { DropboxState } from '../dropbox/dropboxReducer';
import { AssetEditorState } from '../editor/assetEditorReducer';
import { FeedbackState } from '../feedback/feedbackReducer';
import { ProjectGraphEditState } from '../graphEdit/graphEditReducer';
import { LayoutState } from '../layout/dcmLayoutReducer';
import { ModalState } from '../modals/modalReducer';
import { PresenceState } from '../presence/PresenceReducer';
import {
  ContentAccessByRoleAndStatus,
  ContentStatusConfiguration,
  ProjectAccessByRoleAndStatus,
} from '../story/contentStatus';
import { DataState } from '../story/dataReducer';
import { StoryState } from '../story/storyReducer';
import { ProjectGraph } from '../structurePanel/projectGraphReducer';
import { ProjectStructure } from '../structurePanel/projectStructureReducer';
import { ToastState } from '../toast/reducer';
import { UserState } from '../user/reducers';
import { TypeId } from './asset';
import { Polyglot } from './polyglot';

interface ConfigurationState extends ContentStatusConfiguration {
  translations: Polyglot;
  domain: {
    id: string;
    name: string;
    logo?: {
      url: string;
    };
    logo2?: {
      url: string;
    };
  };
  presenceEnabled: boolean;
  chatEnabled: boolean;
  eBookSupportEnabled: boolean;
  user: any;
  adobeStockApiKey?: string;
  contentRightsByRoleAndStatus: ContentAccessByRoleAndStatus;
  projectRightsByRoleAndStatus: ProjectAccessByRoleAndStatus;
  realTime: boolean;
  semiRealTime: boolean;
}

export interface DcmState {
  assetEditor: AssetEditorState;
  configuration: ConfigurationState;
  modal: ModalState;
  layout: LayoutState;
  sidePanelReducer: SidePanelState;
  toast: ToastState;
  projectStructure: ProjectStructure;
  projectGraph: ProjectGraph;
  graphEdits: ProjectGraphEditState;
  router: any;
  user: UserState;
  feedback: FeedbackState;
  story: StoryState;
  data: DataState;
  dropbox: DropboxState;
  presence: PresenceState;
}

export type Thunk<ReturnType = void> = ThunkAction<ReturnType, DcmState, unknown, any>;
