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

import * as React from 'react';
import { CSSProperties, useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import edgeRuleConstants from '../editor/EdgeRuleConstants';
import {
  autoSaveProjectGraphEdits,
  safeSaveProjectGraphEdits,
  useCurrentAssetName,
  useEditedAssetDatum,
  useEditedAssetTypeId,
  useIsAdded,
} from '../graphEdit';
import { useDcmSelector, useNumericRouterQueryParam } from '../hooks';
import { FullPresentUser } from '../presence/PresenceReducer';
import PresenceService from '../presence/services/PresenceService';
import { CommitSegment } from '../revision/revision';
import ReactRouterService from '../router/ReactRouterService';
import { NewAsset, NodeName } from '../types/asset';
import { Thunk } from '../types/dcmState';
import { ContentAccess } from './contentStatus';
import { useContentAccess } from './hooks/useContentAccess';
import { childEdgeGroup, useEditSession } from './story';
import { NarrativeAssetState, StoryState } from './storyReducer';
import qs from 'qs';
import { useProjectGraphSelector } from '../structurePanel/projectGraphHooks.ts';

export const useRevisionCommit = (): number | undefined => useNumericRouterQueryParam('commit');

export const useDiffCommit = (): number | undefined => useNumericRouterQueryParam('diff');

export const useStorySelector = <A>(selector: (state: StoryState) => A) =>
  useDcmSelector(state => selector(state.story));

const NoState = {};

export const useNarrativeAssetState = (asset: NewAsset<any> | NodeName): NarrativeAssetState =>
  useStorySelector(
    state => state.assetStates[typeof asset === 'string' ? asset : asset.name] ?? NoState
  );

export const useIsStoryEditMode = (restoreRevision = false): boolean => {
  const editMode = useStorySelector(state => state.editMode);
  const { userCanEdit } = useDcmSelector(state => state.layout);
  const revisionMode = useDcmSelector(state => ReactRouterService.isRevisionRoute(state)); // horrid
  const commit = useRevisionCommit();
  return editMode && userCanEdit && (restoreRevision || (!revisionMode && !commit));
};

export const useIsEditable = (
  name?: NodeName,
  contentAccess: ContentAccess = 'EditContent',
  restoreRevision = false
) => {
  const editMode = useIsStoryEditMode(restoreRevision);
  const hasAccess = useContentAccess(name)[contentAccess];
  return hasAccess && editMode && !!name;
};

/** Checks for unsaved edits. If none, it dispatches the callback immediately,
 * otherwise if autosave then autosave and continue, else
 * prompt to save and-or continue.
 */
export const narrativeSaveAndContinue =
  (andThen: Thunk): Thunk =>
  (dispatch, getState) => {
    const { graphEdits } = getState();
    if (!graphEdits.dirty) {
      dispatch(andThen);
    } else {
      dispatch(safeSaveProjectGraphEdits(andThen));
    }
  };

export const NoRevisionHistory = [];

export const useRevisionHistory = (name: NodeName): CommitSegment[] => {
  const revisionHistory = useStorySelector(state => state.revisionHistory);
  return revisionHistory && revisionHistory.name === name
    ? revisionHistory.history
    : NoRevisionHistory;
};

export const useIsInlineNarrativeView = () => {
  const inlineView = useStorySelector(state => state.inlineView);
  const name = useCurrentAssetName();
  const typeId = useEditedAssetTypeId(name);
  const edgeGroup = childEdgeGroup(typeId);
  return inlineView && !!edgeRuleConstants[typeId]?.[edgeGroup];
};

export const useRemoteEditor = (name: NodeName | undefined, field: string, editing?: boolean) => {
  const color = useDcmSelector(state => {
    const handle = state.presence.usersOnField[`${name}:${field}`];
    const profile = handle && state.presence.profiles[handle];
    return (profile as FullPresentUser)?.color;
  });
  useEffect(() => {
    if (editing != null) PresenceService.onAssetField(`${name}:${field}`, editing);
  }, [name, field, editing]);
  return useMemo(
    () => (color ? ({ '--remote-editor': color } as unknown as CSSProperties) : undefined),
    [color]
  );
};

// TODO: this could do escape abandonment...
export const useFocusedRemoteEditor = (name: NodeName | undefined, field: string) => {
  const dispatch = useDispatch();
  const [editing, setEditing] = useState(false);
  const session = useEditSession(editing);

  const onFocus = useCallback(() => {
    setEditing(true);
  }, []);

  const onBlur = useCallback((event: React.SyntheticEvent) => {
    if ((event as any).relatedTarget?.classList?.contains('dropdown-item')) {
      // If you select an item then refocus the component and don't autosave
      (event as any).target.focus();
    } else {
      setEditing(false);
      dispatch(autoSaveProjectGraphEdits());
    }
  }, []);

  const remoteEditor = useRemoteEditor(name, field, editing);

  return { onFocus, onBlur, remoteEditor, session };
};

export const useEditedServeUrl = (name: NodeName | undefined) => {
  const branchId = useProjectGraphSelector(graph => graph.branchId);
  const added = useIsAdded(name); // we can't render an unsaved asset :(
  const source = useEditedAssetDatum(name, data => data.source);
  const sourceParams = source && qs.stringify(source, { addQueryPrefix: true });
  return source && !added
    ? `/api/v2/authoring/${branchId}/nodes/${name}/serve${sourceParams}`
    : undefined;
};
