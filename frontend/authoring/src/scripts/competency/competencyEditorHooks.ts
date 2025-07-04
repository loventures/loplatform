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

import gretchen from '../grfetchen/';
import { omitBy, sortBy } from 'lodash';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useDebounce } from 'use-debounce';

import {
  getAllEditedOutEdges,
  getContentTree,
  getEditedAsset,
  getFilteredContentList,
  RootAsset,
  TreeAsset,
  useGraphEditSelector,
} from '../graphEdit';
import { noGraphEdits } from '../graphEdit/graphEditReducer';
import { useDcmSelector } from '../hooks';
import { receiveProjectGraph, useProjectGraph } from '../structurePanel/projectGraphActions';
import { StructureWebResponse } from '../types/api';
import { NodeName } from '../types/asset';
import {
  CompetencyContentEdgeGroups,
  CompetencyTreeEdgeGroups,
  useEditedCompetencySetEdges,
} from './useFlatCompetencies';

// Madness so we can avoid recomputing the competency tree when just a competency set name is edited.
// Be warned, the nodes in this tree may have stale data. Really, we just want the tree to provide
// structure to avoid errors in judgment.
export const useCachedCompetencyTree = () => {
  const competencySetEdge = useEditedCompetencySetEdges()[0];
  const generation = useGraphEditSelector(state => state.generation);
  const [competencyTree, setCompetencyTree] = useState<RootAsset | undefined>();
  const resetCompetencyTree = useCallback(() => setCompetencyTree(undefined), []);
  useEffect(resetCompetencyTree, [generation]);
  const newCompetencyTree = useDcmSelector(({ projectGraph, graphEdits }) => {
    if (competencyTree != null || !competencySetEdge) return competencyTree;
    const competencySetAsset = getEditedAsset(
      competencySetEdge.targetName,
      projectGraph,
      graphEdits
    );
    return getContentTree(
      competencySetAsset,
      [],
      CompetencyTreeEdgeGroups,
      projectGraph,
      graphEdits
    );
  });
  useEffect(() => setCompetencyTree(newCompetencyTree), [newCompetencyTree]);
  return [newCompetencyTree, resetCompetencyTree] as const;
};

// Madness so we can avoid recomputing the filtered competencies as minor edits are made
export const useFilteredCompetencies = (competencyTree: RootAsset | undefined, search: string) => {
  const [lc] = useDebounce(search.toLowerCase(), 100);
  const [filteredCompetencies, setFilteredCompetencies] = useState<TreeAsset[] | undefined>();
  useEffect(() => setFilteredCompetencies(undefined), [lc, competencyTree]);
  const newFilteredCompetencies = useDcmSelector(({ projectGraph, graphEdits }) => {
    if (filteredCompetencies != null || !competencyTree) return filteredCompetencies;
    return getFilteredContentList(
      competencyTree,
      !lc
        ? undefined
        : asset =>
            getEditedAsset(asset.name, projectGraph, graphEdits)
              ?.data.title.toLowerCase()
              .includes(lc)
    );
  });
  useEffect(() => setFilteredCompetencies(newFilteredCompetencies), [newFilteredCompetencies]);
  return newFilteredCompetencies;
};

// Madness so we can avoid recomputing the alignments as minor edits are made
export const useCachedCompetencyAlignments = () => {
  const generation = useGraphEditSelector(state => state.generation);
  const [alignments, setAlignments] = useState<Record<NodeName, number> | undefined>();
  const resetAlignments = useCallback(() => setAlignments(undefined), []);
  useEffect(resetAlignments, [generation]);
  const newAlignments = useDcmSelector(({ projectGraph, graphEdits }) => {
    if (alignments != null) return alignments;
    const newAlignments: Record<NodeName, number> = {};
    const homeNode = getEditedAsset(projectGraph.homeNodeName, projectGraph, graphEdits);
    const contentTree = getContentTree(
      homeNode,
      [],
      CompetencyContentEdgeGroups,
      projectGraph,
      graphEdits
    );
    for (const content of getFilteredContentList(contentTree)) {
      for (const { group, targetName } of getAllEditedOutEdges(
        content.name,
        projectGraph,
        graphEdits
      )) {
        if (group === 'teaches' || group === 'assesses') {
          newAlignments[targetName] = 1 + (newAlignments[targetName] ?? 0);
        }
      }
    }
    return newAlignments;
  });
  useEffect(() => setAlignments(newAlignments), [newAlignments]);
  return [newAlignments, resetAlignments] as const;
};

export type RelatedCompetencies = {
  remote: number;
  level1Competencies: NodeName[];
};

// Return the L1 competencies of multiversally-linked projects
export const useRelatedCompetencies = () => {
  const dispatch = useDispatch();
  const projectGraph = useProjectGraph();
  const { branchId, nodes, assetBranches, branchProjects } = projectGraph;
  // In which we load the competency sets of all related projects into our local project
  // graph so we can find them in our project graph and link them.
  useEffect(() => {
    if (projectGraph.rootNodeName) {
      gretchen
        .get(`/api/v2/authoring/${branchId}/relatedCompetencies`)
        .exec()
        .then((response: StructureWebResponse) => {
          // omit nodes/edges we already know about.
          const nodes = omitBy(response.nodes, (_, name) => projectGraph.nodes[name]);
          const edges = omitBy(response.edges, (_, name) => projectGraph.edges[name]);
          dispatch(receiveProjectGraph({ ...response, nodes, edges }, true));
        });
    }
  }, [branchId, projectGraph.rootNodeName]);

  // In which we search the entire project graph for remote competencies sets and their L1s.
  // L1s that we can link to will never be in the graph edit store.
  const relatedCompetencies = useMemo(() => {
    const related = new Array<RelatedCompetencies>();
    for (const node of Object.values(nodes)) {
      if (node.typeId === 'competencySet.1' && !node.data.archived) {
        const remote = assetBranches[node.name];
        if (remote) {
          const allEdges = getAllEditedOutEdges(node.name, projectGraph, noGraphEdits);
          const l1 = allEdges.filter(edge => edge.group === 'level1Competencies');
          if (l1.length) related.push({ remote, level1Competencies: l1.map(c => c.targetName) });
        }
      }
    }
    return sortBy(related, a => branchProjects[a.remote]?.branchName);
  }, [projectGraph]);

  return relatedCompetencies;
};

export const useLevel1LinkedBranches = (competencyTree: RootAsset | undefined) =>
  useMemo(() => {
    const result = new Set<number>();
    // for (const l1 of competencyTree?.children ?? []) {
    //   if (l1.edge.remote != null) result.add(l1.edge.remote);
    // }
    return result;
  }, [competencyTree]);
