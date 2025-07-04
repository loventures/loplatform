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

import { isEqual } from 'lodash';
import { useMemo } from 'react';

import edgeRuleConstants from '../editor/EdgeRuleConstants';
import { selectRouterPathVariable, selectRouterQueryParam, useDcmSelector } from '../hooks';
import { toMultiWordRegex } from '../story/questionUtil';
import { subPageNames } from '../story/story';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { ProjectGraph } from '../structurePanel/projectGraphReducer';
import { AssetNodeData, EdgeName, NewAsset, NodeName, TypeId } from '../types/asset';
import { DcmState } from '../types/dcmState';
import { EdgeGroup, NewAssetWithEdge, NewEdge } from '../types/edge';
import {
  computeEditedTargets,
  getAllEditedInEdges,
  getAllEditedOutEdges,
  getEditedAsset,
} from './graphEdit';
import { ProjectGraphEditState } from './graphEditReducer';

export const useGraphEditSelector = <A>(selector: (state: ProjectGraphEditState) => A) =>
  useDcmSelector(state => selector(state.graphEdits));

// kill, this is a horrid antipattern that causes constant total renders
export const useGraphEdits = () => useDcmSelector(state => state.graphEdits);

export const useAllEditedOutEdges = (name: string | undefined): Array<NewEdge> =>
  useDcmSelector(state => getAllEditedOutEdges(name, state.projectGraph, state.graphEdits));

export const useAllEditedInEdges = (name: string | undefined): Array<NewEdge> =>
  useDcmSelector(state => getAllEditedInEdges(name, state.projectGraph, state.graphEdits));

// kill
export const useEditedTargets = <T extends TypeId = any>(
  name: NodeName | undefined,
  group: EdgeGroup,
  typeId?: T
): Array<NewAssetWithEdge<T>> => {
  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  return useMemo(
    () => (name ? computeEditedTargets(name, group, typeId, projectGraph, graphEdits) : []),
    [name, group, typeId, projectGraph, graphEdits]
  );
};

/** Returns whether a node has been added or edited. */
export const useIsEdited = (name?: NodeName): boolean =>
  useGraphEditSelector(
    ({ addNodes, editNodes, addEdges, deleteEdges, edgeOrders }) =>
      !!addNodes[name] ||
      !!editNodes[name] ||
      !!addEdges[name] ||
      !!deleteEdges[name] ||
      !!edgeOrders[name]
  );

export const useIsConflicted = (name?: NodeName): boolean =>
  useDcmSelector(state => {
    const edit = state.graphEdits.editNodes[name];
    return (
      !!edit &&
      Object.keys(edit).some(
        key =>
          !isEqual(
            state.projectGraph.nodes[name]?.data[key],
            state.graphEdits.originalNodes[name]?.data[key]
          )
      )
    );
  });

export const useIsEdgeEdited = (name: EdgeName): boolean =>
  useGraphEditSelector(state => !!state.editEdges[name]);

/** Returns whether a node has been added in this edit session. */
export const useIsAdded = (name?: NodeName): boolean =>
  useGraphEditSelector(state => !!state.addNodes[name]);

export const useEditedAsset = (name?: NodeName): NewAsset<any> | undefined =>
  useDcmSelector(state => getEditedAsset(name, state.projectGraph, state.graphEdits));

export const useRestoredAsset = (name?: NodeName): NewAsset<any> | undefined =>
  useGraphEditSelector(state => state.restoredNodes[name]);

export const selectEditedAssetTypeId =
  (name?: NodeName) =>
  (state: DcmState): TypeId | undefined =>
    getEditedAsset(name, state.projectGraph, state.graphEdits)?.typeId;

export const useEditedAssetTypeId = (name?: NodeName): TypeId | undefined =>
  useDcmSelector(selectEditedAssetTypeId(name));

export const useEditedAssetTitle = (name?: NodeName, unknown = 'Unknown'): string =>
  useDcmSelector(state =>
    !state.projectGraph.homeNodeName
      ? 'Loading...'
      : (getEditedAsset(name, state.projectGraph, state.graphEdits)?.data.title ?? unknown)
  );

export const useEditedAssetKeywords = (name?: NodeName): string =>
  useDcmSelector(
    state => getEditedAsset(name, state.projectGraph, state.graphEdits)?.data.keywords ?? ''
  );

export const useEditedAssetContextPath = (name?: NodeName): string | undefined =>
  useGraphEditSelector(state => state.contentTree.contextPaths[name]);

export const useEditedAssetDatum = <A>(
  name: NodeName | undefined,
  f: (data: AssetNodeData) => A
): A | undefined =>
  useDcmSelector(state => {
    const asset = getEditedAsset(name, state.projectGraph, state.graphEdits);
    return asset ? f(asset.data) : undefined;
  });

export const selectCurrentAssetName = (state: DcmState) => {
  const name = selectRouterPathVariable('name')(state);
  const homeNodeName = state.layout.project?.homeNodeName;
  return !name || subPageNames[name] ? homeNodeName : name;
};

export const useCurrentAssetName = (): NodeName => useDcmSelector(selectCurrentAssetName);

export const useEditedCurrentAsset = (): NewAsset<any> => {
  const name = useCurrentAssetName();
  return useEditedAsset(name);
};

export const useIsReusedContent = (name?: NodeName): boolean =>
  useGraphEditSelector(state => state.contentTree.contentReuse.has(name));

export const useCurrentContextPath = (): string | undefined =>
  useDcmSelector(selectRouterQueryParam('contextPath'));

export type NodeInfo = {
  context: (NewAsset<any> | NewAssetWithEdge<any>)[];
  depth: number;
  children: TreeAsset[];
};

export type TreeAsset = NewAssetWithEdge<any> & NodeInfo;

export type TreeAssetWithParent = TreeAsset & { parent: TreeAsset | RootAsset };

export type RootAsset = NewAsset<any> & NodeInfo;

// kill
export const getContentTree = (
  asset: NewAsset<any> | undefined,
  context: NewAsset<any>[],
  groups: EdgeGroup[],
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState
): RootAsset | undefined => {
  const loop = <T extends NewAsset<any> | NewAssetWithEdge<any>>(
    asset: T,
    context: NewAsset<any>[],
    depth: number
  ): T & NodeInfo => {
    const subPath = [...context, asset];
    const edgeGroup = groups.find(g => edgeRuleConstants[asset.typeId]?.[g]) ?? 'elements';
    const children = computeEditedTargets(
      asset.name,
      edgeGroup,
      undefined,
      projectGraph,
      graphEdits
    );
    return {
      ...asset,
      depth,
      context,
      children: children.map(child => loop(child, subPath, 1 + depth)),
    };
  };
  return asset ? loop(asset, context, context.length) : undefined;
};

// kill
export const useContentTree = (
  asset: NewAsset<any> | undefined,
  context: NewAsset<any>[],
  groups: EdgeGroup[]
): RootAsset | undefined => {
  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  return useMemo(
    () => getContentTree(asset, context, groups, projectGraph, graphEdits),
    [projectGraph, graphEdits, asset, context, groups]
  );
};

const Nothing = [];

/** true means include this asset and all its ancestors and descendants, '.' means include
 * this asset and its ancestors but no descendants. */
// kill
export const getFilteredContentList = (
  contentTree?: RootAsset,
  predicate?: (node: TreeAsset) => boolean | '.'
): TreeAssetWithParent[] => {
  if (!contentTree) return Nothing;
  const matchedNodeNames = new Set<NodeName>();
  const matchLoop = <T extends RootAsset | TreeAsset>(
    content: T,
    thisMatch: boolean | '.'
  ): boolean | '.' => {
    let anyMatch = thisMatch;
    if (thisMatch !== '.') {
      for (const child of content.children) {
        const childMatch = thisMatch || predicate?.(child);
        anyMatch = matchLoop(child, childMatch) || anyMatch;
      }
    }
    if (anyMatch) matchedNodeNames.add(content.name);
    return anyMatch;
  };
  if (predicate) matchLoop(contentTree, false);
  const result = new Array<TreeAssetWithParent>();
  const loop = <T extends RootAsset | TreeAsset>(parent: T): void => {
    for (const child of parent.children) {
      if (child.data.archived) continue;
      if (!predicate || matchedNodeNames.has(child.name)) result.push({ ...child, parent });
      loop(child);
    }
  };
  loop(contentTree);
  return result;
};

export const SurveyAndElements: EdgeGroup[] = ['elements', 'survey'];
export const QuestionsAndElements: EdgeGroup[] = ['elements', 'questions'];
export const ElementsOnly: EdgeGroup[] = ['elements'];
export const RubricAndQuestionsAndElements: EdgeGroup[] = ['elements', 'questions', 'cblRubric'];
export const TeachesAndAssesses: EdgeGroup[] = ['teaches', 'assesses'];
export const AnythingAlignable: EdgeGroup[] = [...RubricAndQuestionsAndElements, 'criteria'];

// TODO: turn these into compute() functions that have no dependency on graphEdits so they
// can safely be used in modals that don't edit the structure.
// kill
export const useFilteredContentList = (
  asset: NewAsset<any>,
  context: NewAsset<any>[],
  questions: boolean,
  search: string
): TreeAsset[] => {
  const contentTree = useContentTree(
    asset,
    context,
    questions ? QuestionsAndElements : ElementsOnly
  );
  const role = useDcmSelector(state => state.layout.role);
  const accessRights = useGraphEditSelector(state => state.contentTree.accessRights);
  const predicate = useMemo(() => {
    const regex = toMultiWordRegex(search);
    return (node: NewAsset<any>) =>
      role && !accessRights[node.name].ViewContent ? '.' : regex.test(node.data.title);
  }, [search, role, accessRights]);
  return useMemo(() => getFilteredContentList(contentTree, predicate), [contentTree, predicate]);
};
