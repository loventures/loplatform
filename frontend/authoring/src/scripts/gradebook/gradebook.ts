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

import React, { useMemo } from 'react';

import {
  computeEditedOutEdges,
  computeEditedTargets,
  useEditedTargets,
  useGraphEdits,
  useGraphEditSelector,
} from '../graphEdit';
import { useDcmSelector } from '../hooks';
import useHomeNodeName from '../hooks/useHomeNodeName';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { EdgeName, NewAsset, NodeName } from '../types/asset';

export const nanZero = (a: number) => (isNaN(a) ? 0 : a);

export const blurEnter = (e: React.KeyboardEvent) => {
  if (e.key === 'Enter') (e.target as HTMLElement).blur();
};

export const isAssignment = ({ typeId, data }: NewAsset<any>) => {
  switch (typeId) {
    case 'discussion.1':
    case 'courseLink.1':
      return data.gradable;
    case 'assignment.1':
    case 'assessment.1':
    case 'diagnostic.1':
    case 'poolAssessment.1':
    case 'observationAssessment.1':
    case 'scorm.1':
      return true;
    case 'lti.1':
      // we use the tool configuration on the tool itself because we do not want admin
      // changes to alter the gradebook configuration.
      return data.lti && data.lti.toolConfiguration && data.lti.toolConfiguration.isGraded;
    default:
      return false;
  }
};

// asset, category edge, path, asset edge name
export type GbAssignment = [NewAsset<any>, NodeName | undefined, NewAsset<any>[], EdgeName];

export const useEditedGradebookCategories = () => {
  const homeNodeName = useHomeNodeName();
  return useEditedTargets(homeNodeName, 'gradebookCategories', 'gradebookCategory.1');
};

export const useEditedFlatAssignments = () => {
  const homeNodeName = useHomeNodeName();
  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  const role = useDcmSelector(state => state.layout.role);
  const accessRights = useGraphEditSelector(state => state.contentTree.accessRights);
  return useMemo(() => {
    const contents = new Array<GbAssignment>();
    const categories = new Set(
      computeEditedOutEdges(homeNodeName, 'gradebookCategories', projectGraph, graphEdits).map(
        edge => edge.targetName
      )
    );
    const loop = (name: NodeName, path: NewAsset<any>[]) => {
      const targets = computeEditedTargets(name, 'elements', undefined, projectGraph, graphEdits);
      for (const element of targets) {
        if (isAssignment(element) && (!role || accessRights[element.name].ViewContent)) {
          const catEdges = computeEditedOutEdges(
            element.name,
            'gradebookCategory',
            projectGraph,
            graphEdits
          );
          const category = catEdges[0]?.targetName;
          contents.push([
            element,
            category && categories.has(category) ? category : undefined,
            path,
            element.edge.name,
          ]);
        }
        loop(element.name, [...path, element]);
      }
    };
    loop(homeNodeName, []);
    return contents;
  }, [homeNodeName, projectGraph, graphEdits]);
};

export const formatPercent = (value: number | undefined) =>
  value?.toLocaleString('en-US', {
    style: 'percent',
    maximumFractionDigits: 1,
  }) ?? '–';
