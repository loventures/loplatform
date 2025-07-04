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

import { fetchStructure } from '../structurePanel/projectGraphActions';
import { NodeName } from '../types/asset';

// Name is optional because it's not clear that we should fetch assets in revision mode.
export const NarrativeDeleted: React.FC<{ name?: NodeName }> = ({ name }) => {
  const dispatch = useDispatch();
  useEffect(() => {
    // In case it exists but was deleted and so not in the project graph, try to
    // fetch the asset.
    if (name) dispatch(fetchStructure(name, undefined, true));
  }, [name]);
  return (
    <div className="container narrative-container">
      <div className="story-element text-center text-muted">
        This item is not present in the asset graph or was deleted.
      </div>
    </div>
  );
};
