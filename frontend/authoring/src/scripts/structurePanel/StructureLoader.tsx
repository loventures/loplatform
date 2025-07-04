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

import { useBranchId, useDcmSelector, useHomeNodeName } from '../hooks';
import { useRevisionCommit } from '../story/storyHooks';
import { fetchStructure } from './projectGraphActions';

const StructureLoader: React.FC = () => {
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const homeNodeName = useHomeNodeName();
  const commit = useRevisionCommit();
  const reload = useDcmSelector(state => state.assetEditor.reload);

  useEffect(() => {
    if (branchId && homeNodeName) dispatch(fetchStructure(homeNodeName, commit));
  }, [branchId, homeNodeName, commit, reload]);

  return null;
};

export default StructureLoader;
