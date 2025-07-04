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

import { getFilteredContentList, useAllEditedOutEdges } from '../graphEdit';
import { useRootNodeName } from '../hooks';
import { useProjectGraphSelector } from '../structurePanel/projectGraphHooks';
import { NewAsset } from '../types/asset';
import { EdgeGroup } from '../types/edge';
import { useCachedCompetencyTree } from './competencyEditorHooks';

// Be warned, this aggressively caches under the assumption that the user of flat competencies
// will never also be editing said tree.
export const useFlatCompetencies = (): NewAsset<any>[] => {
  const [competencyTree] = useCachedCompetencyTree();
  return useMemo(() => getFilteredContentList(competencyTree), [competencyTree]);
};

export const useEditedCompetencySetEdges = () => {
  const rootNodeName = useRootNodeName();
  const allEdges = useAllEditedOutEdges(rootNodeName);
  const nodes = useProjectGraphSelector(state => state.nodes);
  return useMemo(
    () =>
      allEdges.filter(
        edge => edge.group === 'competencySets' && !nodes[edge.targetName]?.data.archived
      ),
    [allEdges, nodes]
  );
};

export const CompetencyTreeEdgeGroups: EdgeGroup[] = [
  'level1Competencies',
  'level2Competencies',
  'level3Competencies',
];

export const CompetencyContentEdgeGroups: EdgeGroup[] = [
  'elements',
  'questions',
  'cblRubric',
  'criteria',
];
