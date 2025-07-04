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

import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';

import Loading from '../authoringUi/Loading';
import { useEditedAssetContextPath, useEditedAssetTypeId } from '../graphEdit';
import useHomeNodeName from '../hooks/useHomeNodeName';
import { useRouteParameter } from '../router/ReactRouterService';
import { fetchStructure, useProjectGraph } from '../structurePanel/projectGraphActions';
import { editorUrl, subPageNames } from './story';
import { useRevisionCommit } from './storyHooks';

export const LaunchAsset: React.FC = () => {
  const dispatch = useDispatch();
  const history = useHistory();
  const homeNodeName = useHomeNodeName();
  const name = useRouteParameter('name');
  const nodeName = subPageNames[name] || !name || name === 'home' ? homeNodeName : name;
  const { branchId } = useProjectGraph();
  const commit = useRevisionCommit();

  // If it's in the project graph we'll get a type
  const typeId = useEditedAssetTypeId(nodeName);
  // If it's in the learning path we'll get a contextPath
  const contextPath = useEditedAssetContextPath(nodeName);

  useEffect(() => {
    if (homeNodeName && !branchId) dispatch(fetchStructure(homeNodeName, commit));
  }, [homeNodeName, branchId, commit]);

  useEffect(() => {
    // If I have the project graph but not the node then it wasn't in my normal traversal, may be an
    // unused asset or may be one of the "file" assets or such that I don't fetch, so fetch it.
    if (branchId && !typeId) dispatch(fetchStructure(nodeName, commit, true));
  }, [branchId, typeId, nodeName, commit]);

  useEffect(() => {
    if (typeId) {
      history.replace(editorUrl('story', branchId, nodeName, contextPath, { commit }));
    }
  }, [nodeName, typeId, branchId, contextPath]);

  return (
    <div>
      <Loading />
    </div>
  );
};
