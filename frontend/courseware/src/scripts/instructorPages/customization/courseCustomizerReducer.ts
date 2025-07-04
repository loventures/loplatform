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

import { CustomisableContent } from '../../api/customizationApi';
import { popLast, toggle } from '../../types/arrays';
import { omit } from 'lodash';
import { Option, getOrElse } from '../../types/option';
import lscache from 'lscache';
import { AnyAction, Reducer, Dispatch } from 'redux';
import { createAction, isActionOf } from 'typesafe-actions';

import { ContentEdit } from './contentEdits';
import { Tree } from './Tree';

const CourseCustomizerSkipHiddenCustomizationKey = 'CourseCustomizerSkipHiddenCustomization';

export type CourseCustomizerState = {
  expandedContent: string[];
  hiddenItemsVisible: boolean;
  currentDraggingContext?: {
    dragging: string;
    parent: string;
  };
  redoStack: ContentEdit[];
  edits: ContentEdit[];
  hideConfirmModalContent: Option<{
    id: string;
    childId: string;
    nextVisibleNodeId?: string;
  }>;
  skipHiddenConfirmation: boolean;
  activeContentEditor: Option<{ id: string; setFocus: boolean }>;
  contentBeingEdited: Option<string>;
  outdatedMousePosition: boolean;
};

const skipHiddenConfirmation = lscache.get(CourseCustomizerSkipHiddenCustomizationKey) as
  | boolean
  | undefined;
const initialState: CourseCustomizerState = {
  expandedContent: [],
  hiddenItemsVisible: true,
  redoStack: [],
  edits: [],
  hideConfirmModalContent: null,
  skipHiddenConfirmation: getOrElse<boolean>(skipHiddenConfirmation, false),
  activeContentEditor: null,
  contentBeingEdited: null,
  outdatedMousePosition: false,
};
export type DragContext = {
  dragging: string;
  parent: string;
};

const setSkipHiddenConfirmationAction = createAction(
  'CUSTOMIZER_TOGGLE_SKIP_CONFIRMATION'
)<boolean>();
export const toggleSkipHiddenConfirmation = (skip: boolean) => (dispatch: Dispatch) => {
  lscache.set(CourseCustomizerSkipHiddenCustomizationKey, skip);
  dispatch(setSkipHiddenConfirmationAction(skip));
};

export const closeHideConfirmModal = createAction('CUSTOMIZER_HIDE_CONFIRM_MODAL')();
export const openHideConfirmModal = createAction('CUSTOMIZER_OPEN_CONFIRM_MODAL')<{
  id: string;
  childId: string;
  nextVisibleNodeId?: string;
}>();

export const toggleContentOpen = createAction('CUSTOMIZER_TOGGLE_CONTENT_OPEN')<string>();

export const updateActiveContentEditor = createAction('CUSTOMIZER_UPDATE_ACTIVE_CONTENT_EDITOR')<
  Option<{ id: string; setFocus: boolean }>
>();

export const updateContentBeingEdited = createAction('CUSTOMIZER_UPDATE_CONTENT_BEING_EDITED')<
  Option<string>
>();

export const setMouseState = createAction('CUSTOMIZER_SET_MOUSE_STATE')<boolean>();

export const startDragging = createAction('CUSTOMIZER_START_DRAGGING')<DragContext>();
export const stopDragging = createAction('CUSTOMIZER_STOP_DRAGGING')();

export const addEdit = createAction('CUSTOMIZER_ADD_EDIT')<ContentEdit>();
export const undo = createAction('CUSTOMIZER_UNDO')();
export const redo = createAction('CUSTOMIZER_REDO')();

export const initializeTree = createAction('INITIALIZE_TREE')<Tree<CustomisableContent>>();

export const toggleHiddenItemsVisible = createAction('CUSTOMIZER_TOGGLE_HIDDEN_ITEMS_VISIBLE')();

export const resetEdits = createAction('CUSTOMIZER_RESET_EDITS')();

export const courseCustomizerReducer: Reducer<CourseCustomizerState, AnyAction> = (
  state = initialState,
  action
) => {
  if (isActionOf(toggleContentOpen, action)) {
    return {
      ...state,
      expandedContent: toggle(state.expandedContent, action.payload),
    };
  } else if (isActionOf(startDragging, action)) {
    return {
      ...state,
      currentDraggingContext: action.payload,
    };
  } else if (isActionOf(stopDragging, action)) {
    return omit({ ...state, outdatedMousePosition: true }, 'currentDraggingContext');
  } else if (isActionOf(addEdit, action)) {
    return {
      ...state,
      edits: state.edits.concat(action.payload),
      redoStack: [],
    };
  } else if (isActionOf(undo, action)) {
    const [editToUndo, edits] = popLast(state.edits);
    return {
      ...state,
      redoStack: state.redoStack.concat(editToUndo),
      edits,
    };
  } else if (isActionOf(redo, action)) {
    const [editToRedo, redoStack] = popLast(state.redoStack);
    return {
      ...state,
      redoStack,
      edits: state.edits.concat(editToRedo),
    };
  } else if (isActionOf(toggleHiddenItemsVisible, action)) {
    return {
      ...state,
      hiddenItemsVisible: !state.hiddenItemsVisible,
    };
  } else if (isActionOf(resetEdits, action)) {
    return {
      ...state,
      edits: [],
      redoStack: [],
    };
  } else if (isActionOf(openHideConfirmModal, action)) {
    return {
      ...state,
      hideConfirmModalContent: action.payload,
    };
  } else if (isActionOf(closeHideConfirmModal, action)) {
    return {
      ...state,
      hideConfirmModalContent: null,
    };
  } else if (isActionOf(setSkipHiddenConfirmationAction, action)) {
    return {
      ...state,
      skipHiddenConfirmation: action.payload,
    };
  } else if (isActionOf(updateActiveContentEditor, action)) {
    return {
      ...state,
      activeContentEditor: action.payload,
    };
  } else if (isActionOf(setMouseState, action)) {
    return {
      ...state,
      outdatedMousePosition: action.payload,
    };
  } else if (isActionOf(updateContentBeingEdited, action)) {
    return {
      ...state,
      contentBeingEdited: action.payload,
    };
  } else {
    return state;
  }
};
