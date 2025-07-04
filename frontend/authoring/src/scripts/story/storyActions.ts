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

import { trackAuthoringEvent } from '../analytics';
import { trackNarrativeEditMode } from '../analytics/AnalyticsEvents';
import { UPDATE_ROLE } from '../dcmStoreConstants';
import { confirmSaveProjectGraphEdits, updateContentTree } from '../graphEdit';
import { ContextPath } from '../graphEdit/graphEditReducer';
import { NodeName } from '../types/asset';
import { Thunk } from '../types/dcmState';
import { broadcastToIFrames } from './storyFeedback';
import { NarrativeAssetState, StoryState } from './storyReducer';

export const SET_NARRATIVE_STATE = 'SET_NARRATIVE_STATE';

export const setNarrativeState = (state: Partial<StoryState>) => ({
  type: SET_NARRATIVE_STATE,
  state,
});

export const SET_NARRATIVE_ACTIVE = 'SET_NARRATIVE_ACTIVE';

export const setNarrativeActive = (name: NodeName, contextPath: ContextPath, delta?: number) => ({
  type: SET_NARRATIVE_ACTIVE,
  name,
  contextPath,
  delta,
});

export const SESSION_INLINE_VIEW = 'story.inlineView';

export const SESSION_KEYWORDS = 'story.keyWords';

export const SESSION_OMEGA_EDIT = 'story.omegaEdit';

export const SESSION_SYNCHRONOUS = 'story.synchronous';

export const SESSION_FLAG_MODE = 'story.flagMode';

export const SESSION_EDIT_MODE = (id: number) => `story.editMode.${id}`;

export const setNarrativeInlineViewAction =
  (inlineView: boolean): Thunk =>
  dispatch => {
    sessionStorage.setItem(SESSION_INLINE_VIEW, inlineView.toString());
    dispatch(setNarrativeState({ inlineView }));
  };

export const SET_NARRATIVE_ASSET_STATE = 'SET_NARRATIVE_ASSET_STATE';

export const setNarrativeAssetState = (name: NodeName, state: Partial<NarrativeAssetState>) => ({
  type: SET_NARRATIVE_ASSET_STATE,
  name,
  state,
});

export const editModeAction = (editMode: boolean): Thunk =>
  confirmSaveProjectGraphEdits((dispatch, getState) => {
    const project = getState().layout.project;
    sessionStorage.setItem(SESSION_EDIT_MODE(project.id), editMode.toString());
    dispatch(setNarrativeState({ editMode }));
    trackNarrativeEditMode(editMode ? 'On' : 'Off');
  });

export const editRoleAction =
  (role: string): Thunk =>
  dispatch => {
    dispatch({ type: UPDATE_ROLE, role });
    dispatch(updateContentTree()); // recompute cached access
  };

export const toggleKeywordsMode =
  (value?: boolean): Thunk =>
  (dispatch, getState) => {
    const { keyWords: priorValue } = getState().story;
    const keyWords = value ?? !priorValue;
    if (keyWords !== priorValue) {
      trackAuthoringEvent('Narrative Editor - Keywords Mode', keyWords ? 'On' : 'Off');
      sessionStorage.setItem(SESSION_OMEGA_EDIT, `${keyWords}`);
      dispatch(setNarrativeState({ keyWords }));
    }
  };

export const toggleOmegaEdit =
  (value?: boolean): Thunk =>
  (dispatch, getState) => {
    const { omegaEdit: priorEdit } = getState().story;
    const omegaEdit = value ?? !priorEdit;
    if (omegaEdit !== priorEdit) {
      trackAuthoringEvent('Narrative Editor - Ninja Raptor Mode', omegaEdit ? 'On' : 'Off');
      sessionStorage.setItem(SESSION_OMEGA_EDIT, `${omegaEdit}`);
      dispatch(setNarrativeState({ omegaEdit }));
    }
  };

export const toggleSynchronous =
  (value?: boolean): Thunk =>
  (dispatch, getState) => {
    const { synchronous: priorSync } = getState().story;
    const synchronous = value ?? !priorSync;
    if (synchronous !== priorSync) {
      trackAuthoringEvent('Narrative Editor - Synchronous Mode', synchronous ? 'On' : 'Off');
      sessionStorage.setItem(SESSION_SYNCHRONOUS, `${synchronous}`);
      dispatch(setNarrativeState({ synchronous }));
    }
  };

export const toggleFlagMode =
  (value?: boolean): Thunk =>
  (dispatch, getState) => {
    const { flagMode: priorMode } = getState().story;
    const flagMode = value ?? !priorMode;
    if (flagMode !== priorMode) {
      trackAuthoringEvent('Narrative Editor - Language Flags Mode', flagMode ? 'On' : 'Off');
      sessionStorage.setItem(SESSION_FLAG_MODE, `${flagMode}`);
      dispatch(setNarrativeState({ flagMode }));
      (window as any).flagMode = flagMode;
      broadcastToIFrames({ fn: 'checkEnabled' });
    }
  };

export const incrementFindCount = (): Thunk => (dispatch, getState) => {
  const { findCount } = getState().story;
  dispatch(setNarrativeState({ findCount: 1 + findCount }));
};
