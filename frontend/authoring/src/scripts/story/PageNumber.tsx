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

import classNames from 'classnames';
import React, { useEffect, useMemo } from 'react';
import { FiArrowLeft, FiArrowRight } from 'react-icons/fi';
import { HiArrowUturnLeft, HiArrowUturnRight } from 'react-icons/hi2';
import { useHistory } from 'react-router';
import { Link } from 'react-router-dom';

import { CONTAINER_AND_ELEMENT_TYPES } from '../editor/EdgeRuleConstants';
import {
  selectEditedAssetTypeId,
  useAllEditedOutEdges,
  useEditedAssetTitle,
  useEditedAssetTypeId,
  useGraphEditSelector,
} from '../graphEdit';
import { ContextPath } from '../graphEdit/graphEditReducer';
import { useBranchId, useDcmSelector } from '../hooks';
import { NodeName, TypeId } from '../types/asset';
import { isQuestion } from './questionUtil';
import {
  NarrativeMode,
  childEdgeGroup,
  editorUrl,
  preventUndefinedClick,
  truncateAssetTitle,
} from './story';
import { useRevisionCommit } from './storyHooks';

export const PageNumber: React.FC<{
  name: NodeName;
  typeId: TypeId;
  mode: NarrativeMode;
  parentName: string;
  contextPath: string;
}> = ({ name, typeId, mode, parentName, contextPath }) => {
  return mode === 'apex' ? (
    <PageNumberNavigation
      name={name}
      typeId={typeId}
      parentName={parentName}
      contextPath={contextPath}
    />
  ) : mode === 'inline' && parent ? (
    <SimplePageNumber
      name={name}
      typeId={typeId}
      parentName={parentName}
      contextPath={contextPath}
    />
  ) : null;
};

const SimplePageNumber: React.FC<{
  name: NodeName;
  typeId: TypeId;
  parentName: string;
  contextPath: ContextPath;
}> = ({ name, typeId, parentName, contextPath }) => {
  const parentTypeId = useEditedAssetTypeId(parentName);
  const parentTitle = useEditedAssetTitle(parentName);
  const subcontextPath = `${contextPath}.${name}`;
  const page = useGraphEditSelector(state => state.contentTree.page[subcontextPath]);
  const count = useGraphEditSelector(state => state.contentTree.count[contextPath]);
  const prefix =
    parentTypeId === 'course.1'
      ? 'Lesson'
      : isQuestion(typeId)
        ? 'Question'
        : truncateAssetTitle(parentTitle, parentTypeId) + ' –';
  return page == null ? null : (
    <div className="page-number mt-4 p-2">{`${prefix} ${page} / ${count}`}</div>
  );
};

const containerAndElementTypes = new Set(CONTAINER_AND_ELEMENT_TYPES);

// This is all expensivish but there's only ever one PageNumberNavigation on screen.
export const usePlaylistIndex = (name: NodeName, contextPath: string) => {
  const subcontextPath = contextPath ? `${contextPath}.${name}` : name;
  return useGraphEditSelector(state => state.contentTree.playlist.indexOf(subcontextPath));
};

export const useNextUp = (name: NodeName, contextPath: string, playlistIndex: number) => {
  const nextUpPath = useDcmSelector(state => {
    if (playlistIndex >= 0) {
      const playlist = state.graphEdits.contentTree.playlist;
      for (let next = 1 + playlistIndex; next < playlist.length; ++next) {
        const path = playlist[next];
        const name = path.substring(1 + path.lastIndexOf('.'));
        const typeId = selectEditedAssetTypeId(name)(state);
        if (containerAndElementTypes.has(typeId)) return path;
      }
    }
    return undefined;
  });
  const nextUp = nextUpPath?.substring(1 + nextUpPath.lastIndexOf('.'));
  const nextUpContext = nextUpPath?.slice(0, -1 - nextUp.length);
  return { nextUp, nextUpContext };
};

export const usePrevUp = (name: NodeName, contextPath: string, playlistIndex: number) => {
  const prevUpPath = useDcmSelector(state => {
    if (playlistIndex >= 0) {
      const playlist = state.graphEdits.contentTree.playlist;
      for (let prev = playlistIndex - 1; prev >= 0; --prev) {
        const path = playlist[prev];
        const name = path.substring(1 + path.lastIndexOf('.'));
        const typeId = selectEditedAssetTypeId(name)(state);
        if (containerAndElementTypes.has(typeId) || prev === 0) return path;
      }
    }
    return undefined;
  });
  const prevUp = prevUpPath?.substring(1 + prevUpPath.lastIndexOf('.'));
  const prevUpContext = prevUpPath?.slice(0, -1 - prevUp.length);
  return { prevUp, prevUpContext };
};

const PageNumberNavigation: React.FC<{
  name: NodeName;
  typeId: TypeId;
  parentName: string;
  contextPath: string;
}> = ({ name, parentName, contextPath }) => {
  const branchId = useBranchId();
  const parentTypeId = useEditedAssetTypeId(parentName);
  const edgeGroup = childEdgeGroup(parentTypeId);
  const allEdges = useAllEditedOutEdges(parentName);
  const siblings = useMemo(
    () => allEdges.filter(edge => edge.group === edgeGroup),
    [allEdges, edgeGroup]
  );
  const siblingIndex = siblings.findIndex(node => node.targetName === name);
  const lag = siblings[siblingIndex - 1]?.targetName;
  const lagTitle = useEditedAssetTitle(lag);
  const lead = siblings[siblingIndex + 1]?.targetName;
  const leadTitle = useEditedAssetTitle(lead);
  const history = useHistory();
  const commit = useRevisionCommit();
  const commitQuery = commit ? `&commit=${commit}` : '';

  const playlistIndex = usePlaylistIndex(name, contextPath);
  const { nextUp, nextUpContext } = useNextUp(name, contextPath, playlistIndex);
  const nextUpTitle = useEditedAssetTitle(nextUp);
  const { prevUp, prevUpContext } = usePrevUp(name, contextPath, playlistIndex);
  const prevUpTitle = useEditedAssetTitle(prevUp);

  // space/shift-space for next up/prev up.
  const spaceNext = edgeGroup === 'questions' ? lead : nextUp;
  const spaceNextContext = edgeGroup === 'questions' ? contextPath : nextUpContext;
  const spacePrev = edgeGroup === 'questions' ? lag : prevUp;
  const spacePrevContext = edgeGroup === 'questions' ? contextPath : prevUpContext;
  useEffect(() => {
    const listener = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      const tag = target?.tagName;
      if (e.key === ' ' && (tag === 'BODY' || tag === 'A') && !target.closest('.eat-space')) {
        // if you click a link, the link receives the space
        const dst = e.shiftKey ? spacePrev : spaceNext;
        const dstContext = e.shiftKey ? spacePrevContext : spaceNextContext;
        e.preventDefault();
        if (dst) {
          history.push(`/branch/${branchId}/story/${dst}?contextPath=${dstContext}${commitQuery}`);
        }
      }
    };
    document.addEventListener('keydown', listener);
    return () => {
      document.removeEventListener('keydown', listener);
    };
  }, [spaceNext, spacePrev, spaceNextContext, spacePrevContext]);

  return siblingIndex < 0 && !nextUp ? null : (
    <div className="page-number-cluster">
      <Link
        to={prevUp ? `${prevUp}?contextPath=${prevUpContext}${commitQuery}` : ''}
        className={classNames(
          'btn btn-transparent text-primary border-0 d-flex p-2 br-50 me-2 prev-up',
          (!prevUp || prevUp === lag) && 'disabled'
        )}
        title={`Previous: ${prevUpTitle}`}
      >
        <HiArrowUturnLeft style={{ transform: 'scaleY(-1)', strokeWidth: 0.4 }} />
      </Link>
      <Link
        to={lag ? editorUrl('story', branchId, lag, contextPath, { commit }) : ''}
        className={classNames(
          'btn btn-transparent text-primary border-0 me-3 d-flex p-2 br-50 prev-page',
          !lag && 'disabled'
        )}
        onClick={preventUndefinedClick(lag)}
        title={lagTitle}
        tabIndex={!lag ? -1 : undefined}
      >
        <FiArrowLeft />
      </Link>
      {siblingIndex < 0 ? null : `${siblingIndex + 1} / ${siblings.length}`}
      <Link
        to={lead ? editorUrl('story', branchId, lead, contextPath, { commit }) : ''}
        className={classNames(
          'btn btn-transparent text-primary border-0 ms-3 d-flex p-2 br-50 next-page',
          !lead && 'disabled'
        )}
        onClick={preventUndefinedClick(lead)}
        title={leadTitle}
        tabIndex={!lead ? -1 : undefined}
      >
        <FiArrowRight />
      </Link>
      <Link
        to={nextUp ? `${nextUp}?contextPath=${nextUpContext}${commitQuery}` : ''}
        className={classNames(
          'btn btn-transparent text-primary border-0 d-flex p-2 br-50 ms-2 next-up',
          !nextUp && 'disabled'
        )}
        title={`Next Up: ${nextUpTitle}`}
      >
        <HiArrowUturnRight style={{ transform: 'scaleY(-1)', strokeWidth: 0.4 }} />
      </Link>
    </div>
  );
};
