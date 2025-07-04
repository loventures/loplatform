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

import edgeRules from '../editor/EdgeRuleConstants';
import { EdgePosition } from './api';
import { AssetNode, NewAsset, TypeId, UnionToIntersection } from './asset';

/**
 * Edge Rules
 *
 * We import the const that defines possible edges and type it as strongly as possible. Non-exported
 * members are the type checking. We export valid EdgeGroups and a generic Includes type.
 *
 * NOTE: If you are adding edge groups remember to add edge groups in the constant below.
 *       If EdgeGroup or EdgeRuleMap are ever `never` you have mispelled a type or forgotten to add
 *       a constant below or in typeId.
 * */
type EdgeRules = typeof edgeRules;
type FilteredEdgeRules = {
  readonly [asset in Extract<keyof EdgeRules, TypeId>]: {
    readonly [group in Extract<
      keyof EdgeRules[asset],
      EdgeGroupUnion
    >]: EdgeRules[asset][group] extends TypeId[] ? EdgeRules[asset][group] : never;
  };
};
// Check that the filtered rules and actual rules are the same.
type EdgeRuleMap = FilteredEdgeRules extends EdgeRules ? EdgeRules : never;
type AssetTypesWithEdgeRules = keyof EdgeRuleMap;

export type EdgeGroup = keyof UnionToIntersection<EdgeRuleMap[keyof EdgeRuleMap]>;
export type Includes<T extends TypeId = 'ignoreme'> = T extends AssetTypesWithEdgeRules
  ? {
      [G in keyof EdgeRuleMap[T]]: FullEdge[];
    }
  : {
      [K in EdgeGroup]: FullEdge[];
    };

// NOTE: We list these manually to type check the groups.
//       You have to add new groups here and in edgeRules.
export type EdgeGroupUnion =
  | 'courses'
  | 'competencySets'
  | 'criteria'
  | 'level1Competencies'
  | 'level2Competencies'
  | 'level3Competencies'
  | 'elements'
  | 'questions'
  | 'cblRubric'
  | 'dependencies'
  | 'gates'
  | 'testsOut'
  | 'remediationResources'
  | 'cssResources'
  | 'scripts'
  | 'stylesheets'
  | 'captions'
  | 'content'
  | 'assesses'
  | 'teaches'
  | 'survey'
  | 'resources'
  | 'image'
  | 'gradebookCategories'
  | 'gradebookCategory'
  | 'hyperlinks';

/**
 * Edge shapes.
 *
 * There are slim and full edges. Slim edges are normalized to avoid redundantly storing assets in
 * the structure graph.
 * */
export type BaseEdge = {
  created: string;
  data: EdgeData;
  edgeId: string;
  group: EdgeGroup;
  id: number;
  modified: string;
  name: string;
  position: number;
  traverse: boolean;
};

export type Edge = FullEdge | SlimEdge;

export type FullEdge = BaseEdge & {
  source: AssetNode;
  target: AssetNode;
};

export type SlimEdge = BaseEdge & {
  sourceName: string;
  targetName: string;
};

// the stuff you need to create an edge
export type NewEdge = Pick<
  SlimEdge,
  'name' | 'group' | 'sourceName' | 'targetName' | 'data' | 'traverse'
> & { edgeId?: string; newPosition?: EdgePosition };

/** The mixed-in edge is the edge used to traverse *to* this asset in a particular context. */
export type NewAssetWithEdge<T extends TypeId> = NewAsset<T> & {
  index: number;
  edge: NewEdge;
  restored?: boolean;
};

/**
 * Edge Data
 * */
export type EdgeData<T> = {
  [key in T]: (typeof T)[key];
};

export interface PerformanceGateData {
  performanceGate: {
    threshold: number;
  };
}
