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
import React, { useMemo } from 'react';
import { FiArrowLeft, FiArrowRight, FiArrowUp } from 'react-icons/fi';
import { HiArrowUturnRight } from 'react-icons/hi2';
import { Link } from 'react-router-dom';

import { useAllEditedOutEdges, useEditedAssetTitle, useEditedAssetTypeId } from '../graphEdit';
import { useBranchId } from '../hooks';
import { NodeName } from '../types/asset';
import { useNextUp, usePlaylistIndex } from './PageNumber';
import { childEdgeGroup, editorUrl, preventUndefinedClick } from './story';
import { useRevisionCommit } from './storyHooks';

export const PageNub: React.FC<{
  name: NodeName;
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
  const commit = useRevisionCommit();
  const parentTitle = useEditedAssetTitle(parentName);
  const parentContext = parentName ? contextPath.substring(0, contextPath.lastIndexOf('.')) : '';

  const playlistIndex = usePlaylistIndex(name, contextPath);
  const { nextUp, nextUpContext } = useNextUp(name, contextPath, playlistIndex);
  const nextUpTitle = useEditedAssetTitle(nextUp);

  // These have -1 tab index to stay out of the tab order as they are entirely duplicative of the
  // page number gadget that comes just before.
  return (
    <div className="page-number-nub-holder">
      <div className="page-number-nub d-flex flex-column">
        <Link
          to={parentName ? editorUrl('story', branchId, parentName, parentContext, { commit }) : ''}
          className={classNames(
            'btn btn-transparent text-primary border-0 d-flex up-page-nub',
            !parentName && 'disabled'
          )}
          onClick={preventUndefinedClick(parentName)}
          title={parentTitle}
          tabIndex={-1}
        >
          <FiArrowUp />
        </Link>

        <Link
          to={lead ? editorUrl('story', branchId, lead, contextPath, { commit }) : ''}
          className={classNames(
            'btn btn-transparent text-primary border-0 d-flex next-page-nub',
            !lead && 'disabled'
          )}
          onClick={preventUndefinedClick(lead)}
          title={leadTitle}
          tabIndex={-1}
        >
          <FiArrowRight />
        </Link>
        <Link
          to={lag ? editorUrl('story', branchId, lag, contextPath, { commit }) : ''}
          className={classNames(
            'btn btn-transparent text-primary border-0 d-flex prev-page-nub',
            !lag && 'disabled'
          )}
          onClick={preventUndefinedClick(lag)}
          title={lagTitle}
          tabIndex={-1}
        >
          <FiArrowLeft />
        </Link>
        <Link
          to={nextUp ? editorUrl('story', branchId, nextUp, nextUpContext, { commit }) : ''}
          className={classNames(
            'btn btn-transparent text-primary border-0 d-flex next-up-page-nub',
            !nextUp && 'disabled'
          )}
          onClick={preventUndefinedClick(nextUp)}
          title={nextUpTitle ? `Next Up: ${nextUpTitle}` : undefined}
          tabIndex={-1}
        >
          <HiArrowUturnRight style={{ transform: 'scaleY(-1)', strokeWidth: 0.4 }} />
        </Link>
      </div>
    </div>
  );
};
