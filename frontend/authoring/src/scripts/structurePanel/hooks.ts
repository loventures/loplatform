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

import { useMemo } from 'react';

import { Asset, NodeName, TypeId } from '../types/asset';
import { EdgeGroup } from '../types/edge';
import { useProjectGraph } from './projectGraphActions';
import { ProjectGraph } from './projectGraphReducer';

const NoTargets = [];

export const getTargets = <T extends TypeId = any>(
  source: NodeName,
  group: EdgeGroup,
  typeId: T | undefined,
  { edges, outEdgesByNode, nodes }: ProjectGraph
): Array<Asset<T>> =>
  outEdgesByNode[source]
    ?.map(name => edges[name])
    .filter(edge => edge?.group === group)
    .map(edge => nodes[edge.targetName])
    .filter(
      node => node != null && !node.data.archived && (typeId == null || typeId === node.typeId)
    ) ?? NoTargets;

export const useTargets = <T extends TypeId = any>(
  name: NodeName,
  group: EdgeGroup,
  typeId?: T
): Array<Asset<T>> => {
  const projectGraph = useProjectGraph();
  return useMemo(() => {
    return getTargets(name, group, typeId, projectGraph);
  }, [name, group, typeId, projectGraph]);
};
