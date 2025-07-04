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

import { uniqueId } from 'lodash';

import { AnythingAlignable, computeEditedOutEdges, TeachesAndAssesses } from '../graphEdit';
import { ProjectGraphEditState } from '../graphEdit/graphEditReducer';
import { generateImportWriteOps, ImportError, ImportResult } from '../importer/importUtils2';
import AuthoringOpsService from '../services/AuthoringOpsService';
import { ProjectGraph } from '../structurePanel/projectGraphReducer';
import { WriteOp } from '../types/api';
import { EdgeName, NewAsset, NodeName } from '../types/asset';
import * as T from '../types/typeIds';

export const generateWriteOps = (
  file: File,
  csNode: NewAsset<'competencySet.1'> | undefined,
  edgeNames: EdgeName[],
  competencyTitles: string[],
  rootNodeName: NodeName
): Promise<ImportResult> =>
  generateImportWriteOps(file, requiredHeaders, optionalHeaders, data =>
    validateRows(data, csNode, edgeNames, competencyTitles, rootNodeName, file.name)
  );

const requiredHeaders = {
  'Level One Competency': 'l1',
  'Level Two Competency': 'l2',
  'Level Three Competency': 'l3',
};

const optionalHeaders = {
  '': 'blank',
};

const validateRows = (
  data: Record<string, string>[],
  csOpt: NewAsset<'competencySet.1'> | undefined,
  edgeNames: EdgeName[],
  competencyTitles: string[],
  rootNodeName: NodeName,
  fileName: string
): ImportResult => {
  const titles = new Set(competencyTitles.map(n => n.trim().toLowerCase()));
  const ops = new Array<WriteOp>();
  const errors = new Array<ImportError>();
  const warnings = new Array<ImportError>();
  let l1Name: string | undefined = undefined;
  let l2Name: string | undefined = undefined;
  let rowNum = 1; // header
  let csNodeName = csOpt?.name;
  if (!csNodeName) {
    csNodeName = uniqueId();
    ops.push({
      op: 'addNode',
      name: csNodeName,
      data: { title: fileName.replace(/\.csv$/i, '') },
      typeId: 'competencySet.1',
    });
    const edgeName = uniqueId();
    ops.push({
      op: 'addEdge',
      name: edgeName,
      sourceName: rootNodeName,
      targetName: csNodeName,
      group: 'competencySets',
      traverse: true,
      position: 'end',
    });
  }

  const edgeOrders: Record<string, { group: string; ordering: string[] }> = {
    [csNodeName]: { group: 'level1Competencies', ordering: edgeNames },
  };
  for (const row of data) {
    ++rowNum;
    const { l1, l2, l3 } = row;
    if (!l1 && !l2 && !l3) continue;
    if ((l1 && l2) || (l1 && l3) || (l2 && l3)) {
      warnings.push({ rowNum, err: 'COMPETENCY_SET_IMPORT.TOO_MANY_COMPETENCIES' });
      continue;
    }
    const title = (l1 || l2 || l3).trim();
    if (titles.has(title.toLowerCase())) {
      warnings.push({ rowNum, err: 'COMPETENCY_SET_IMPORT.DUPLICATE_NAME' });
      continue;
    } else if (title.length > 255) {
      warnings.push({ rowNum, err: 'COMPETENCY_SET_IMPORT.NAME_TOO_LONG' });
      continue;
    }
    if ((l2 && !l1Name) || (l3 && !l2Name)) {
      warnings.push({ rowNum, err: 'COMPETENCY_SET_IMPORT.INVALID_LEVEL' });
      continue;
    }
    const typeId = l1 ? T.Level1Competency : l2 ? T.Level2Competency : T.Level3Competency;
    const group = l1 ? 'level1Competencies' : l2 ? 'level2Competencies' : 'level3Competencies';
    const sourceName = l1 ? csNodeName : l2 ? l1Name! : l2Name!;
    const nodeName = uniqueId();
    const edgeName = uniqueId();
    ops.push({
      op: 'addNode',
      name: nodeName,
      data: { title },
      typeId,
    });
    ops.push({
      op: 'addEdge',
      name: edgeName,
      sourceName,
      targetName: nodeName,
      group,
      traverse: true,
    });
    if (!edgeOrders[sourceName]) {
      edgeOrders[sourceName] = { group, ordering: [edgeName] };
    } else {
      edgeOrders[sourceName].ordering.push(edgeName);
    }
    titles.add(title.toLowerCase());
    if (l1) {
      l1Name = nodeName;
      l2Name = undefined;
    } else if (l2) {
      l2Name = nodeName;
    }
  }
  ops.push(
    ...Object.entries(edgeOrders).map<WriteOp>(([sourceName, { group, ordering }]) => ({
      op: 'setEdgeOrder',
      sourceName,
      group,
      ordering,
    }))
  );
  return { errors, warnings, ops };
};

export const getDeleteOps = (
  replacePrevious: boolean,
  competencySet: NewAsset<'competencySet.1'> | undefined,
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState
): WriteOp[] => {
  if (!replacePrevious || !competencySet) {
    return [];
  }
  const wops = new Array<WriteOp>();

  for (const l1 of computeEditedOutEdges(
    competencySet.name,
    'level1Competencies',
    projectGraph,
    graphEdits
  )) {
    wops.push(AuthoringOpsService.deleteEdgeOp(l1.name));
  }

  const visited = new Set<NodeName>();
  const loop = (name: NodeName) => {
    visited.add(name);
    for (const align of computeEditedOutEdges(name, TeachesAndAssesses, projectGraph, graphEdits)) {
      wops.push(AuthoringOpsService.deleteEdgeOp(align.name));
    }
    for (const edge of computeEditedOutEdges(name, AnythingAlignable, projectGraph, graphEdits)) {
      if (!visited.has(edge.targetName)) loop(edge.targetName);
    }
  };
  loop(projectGraph.homeNodeName);

  return wops;
};
