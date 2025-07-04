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
import { generateImportWriteOps, ImportError, ImportResult } from '../importer/importUtils2';
import { WriteOp } from '../types/api';
import { NewAsset } from '../types/asset';
import { NewEdge } from '../types/edge';

export type ElementNode = {
  node: NewAsset<any>;
  teaches: NewEdge[];
  assesses: NewEdge[];
  children: ElementNodes;
};

export type ElementNodes = Record<string, ElementNode>;

export const generateWriteOps = (
  file: File,
  contentTree: ElementNodes,
  competenciesByName: Record<string, string>
): Promise<ImportResult> =>
  generateImportWriteOps(file, requiredHeaders, optionalHeaders, data =>
    validateRows(data, contentTree, competenciesByName)
  );

const requiredHeaders = {
  'Level One': 'l1',
  'Level Two': 'l2',
  'Level Three': 'l3',
  Teaches: 'teaches',
};

const optionalHeaders = {
  'LO Asset Type': 'typeId', // included in the export, unused but optional in the import
  Assesses: 'assesses',
  Question: 'question', // for importing question alignment
  '': 'blank', // we shall ignore
  // ignore any of the structure export columns  just to be nice
  'Assessment Template': 'assessmentTemplate',
  Points: 'pointsPossible',
  Layout: 'singlePage',
  'Show results': 'immediateFeedback',
  'Confidence Level': 'displayConfidenceIndicators',
  'Show answer on results': 'hideAnswerIfIncorrect',
  Type: 'assessmentType',
  'Number of attempts': 'maxAttempts',
  'Time Limit': 'maxMinutes',
  'Scoring Option': 'scoringOption',
  'Number of questions (Pool only)': 'numberOfQuestionsForAssessment',
  'Count for Credit': 'isForCredit',
  Instructions: 'instructions',
  'Gradebook Category': 'category',
};

const competencyGroups = ['assesses', 'teaches'] as const;

const validateRows = (
  data: Record<string, string>[],
  contentTree: ElementNodes,
  competenciesByTitle: Record<string, string>
): ImportResult => {
  const visited = new Set<string>();
  const ops = new Array<WriteOp>();
  const errors = new Array<ImportError>();
  const warnings = new Array<ImportError>();
  let l1Tree: ElementNodes | undefined = undefined;
  let l2Tree: ElementNodes | undefined = undefined;
  let l3Tree: ElementNodes | undefined = undefined;
  let rowNum = 1; // header
  let added = 0,
    removed = 0;
  for (const row of data) {
    ++rowNum;
    const { l1, l2, l3, question, typeId } = row;
    if (!l1 && !l2 && !l3 && !question) continue;
    if ((l1 && (l2 || l3 || question)) || (l2 && (l3 || question)) || (l3 && question)) {
      warnings.push({ rowNum, err: 'CONTENT_ALIGNMENT_IMPORT.TOO_MANY_TITLES' });
      continue;
    }
    const title = (l1 || l2 || l3 || question).trim();
    if ((l2 && !l1Tree) || (l3 && !l2Tree) || (question && !l2Tree && !l3Tree)) {
      warnings.push({ rowNum, err: 'CONTENT_ALIGNMENT_IMPORT.INVALID_LEVEL' });
      continue;
    }
    const tree = l1 ? contentTree : l2 ? l1Tree! : l3 ? l2Tree! : (l3Tree ?? l2Tree)!;
    const element = tree[title.toLowerCase()];
    if (!element) {
      warnings.push({ rowNum, err: 'CONTENT_ALIGNMENT_IMPORT.UNKNOWN_CONTENT' });
      continue;
    } else if (visited.has(element.node.name)) {
      warnings.push({ rowNum, err: 'CONTENT_ALIGNMENT_IMPORT.DUPLICATE_CONTENT' });
      continue;
    }

    for (const group of competencyGroups) {
      const competencyNames = (row[group] || '')
        .split('\n')
        .map(a => a.trim())
        .filter(a => !!a)
        .reduce<string | string[]>((o, a) => {
          if (typeof o === 'string') return o;
          const name = competenciesByTitle[a.toLowerCase()];
          return name == null ? a : [...o, name];
        }, new Array<string>());
      if (typeof competencyNames === 'string') {
        warnings.push({
          rowNum,
          err: 'CONTENT_ALIGNMENT_IMPORT.UNKNOWN_COMPETENCY',
          title: competencyNames,
        });
        continue;
      } else if (competencyNames.length && !edgeRules[element.node.typeId][group]) {
        warnings.push({
          rowNum,
          err: 'CONTENT_ALIGNMENT_IMPORT.INVALID_ALIGNMENT',
          typeId: element.node.typeId,
          group,
        });
        continue;
      } else if (typeId && typeId !== /*friendlyName()*/ element.node.typeId) {
        // it's the friendly name we care about which is in CompetencyWebController and friends.....
        // warnings.push({
        //   rowNum,
        //   err: 'CONTENT_ALIGNMENT_IMPORT.INVALID_TYPE_ID',
        //   typeId: element.node.typeId,
        //   expected: typeId,
        // });
        // continue;
      }
      const existingEdges = element[group] ?? [];
      if (
        existingEdges.length !== competencyNames.length ||
        existingEdges.some(edge => !competencyNames.includes(edge.targetName))
      ) {
        competencyNames.forEach(targetName => {
          const existingEdge = existingEdges.find(edge => edge.targetName === targetName);
          if (existingEdge != null) return existingEdge.name;
          ops.push({
            op: 'addEdge',
            name: crypto.randomUUID(),
            sourceName: element.node.name,
            targetName,
            group,
            traverse: false,
            position: 'end',
          });
          ++added;
        });
        existingEdges.forEach(edge => {
          if (!competencyNames.includes(edge.targetName)) {
            ops.push({
              op: 'deleteEdge',
              name: edge.name,
            });
            ++removed;
          }
        });
      }
    }

    visited.add(element.node.name);
    if (l1) {
      l1Tree = element.children;
      l2Tree = undefined;
      l3Tree = undefined;
    } else if (l2) {
      l2Tree = element.children;
      l3Tree = undefined;
    } else if (l3) {
      l3Tree = element.children;
    }
  }

  return { errors, warnings, ops, added, removed };
};
