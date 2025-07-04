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

import { mapValues, uniqueId } from 'lodash';

import edgeRules from '../../editor/EdgeRuleConstants';
import { generateImportWriteOps, processRows as realProcessRows } from '../importUtils2';
import { validateAssessmentSettings } from './templateImportAssessmentValidation';
import { error, warning } from './templateImportError';
import * as TEMPLATE_IMPORT from './templateImportTranslationKeys';

// TODO: The validation here is all shit, it should look at edge rules not magical depth

export function generateWriteOps(file, edges, courseNodeName, competencies, categories) {
  return generateImportWriteOps(file, devFriendlyHeaders, optionalHeaders, data =>
    validateRows(data, edges, courseNodeName, competencies, categories)
  ).then(({ ops, errors, warnings }) => ({
    ops: cleanOps(ops),
    errors: cleanErrors(errors),
    warnings,
  }));
}

// shamefully written for shameful tests
export function processRows(data, meta, edges, courseNodeName) {
  return realProcessRows(data, meta, devFriendlyHeaders, optionalHeaders, data => {
    const { ops, errors } = validateRows(data, edges, courseNodeName);
    return { ops: cleanOps(ops), errors: cleanErrors(errors) };
  });
}

export const devFriendlyHeaders = {
  'Level One': 'lvl1Title',
  'Level Two': 'lvl2Title',
  'Level Three': 'lvl3Title',
  'LO Asset Type': 'typeId',
  'Assessment Template': 'assessmentTemplate',
  Points: 'pointsPossible',
  Layout: 'singlePage',
  'Show results': 'immediateFeedback',
  'Confidence Level': 'displayConfidenceIndicators',
  'Show answer on results': 'hideAnswerIfIncorrect',
  Type: 'assessmentType',
  'Number of attempts': 'maxAttempts',
  'Scoring Option': 'scoringOption',
  'Number of questions (Pool only)': 'numberOfQuestionsForAssessment',
  'Count for Credit': 'isForCredit',
  Instructions: 'instructions',
};

const optionalHeaders = {
  LO: 'lo',
  Teaches: 'teaches',
  Assesses: 'assesses',
  'Gradebook Category': 'category',
  'Time Limit': 'maxMinutes',
  'Level Four': 'lvl4Title',
};

const human2TypeId = {
  unit: 'unit.1',
  module: 'module.1',
  lesson: 'lesson.1',
  'html page': 'html.1',
  'scorm activity': 'scorm.1',
  'legacy learning activity': 'resource.1',
  'discussion board': 'discussion.1',
  'file bundle': 'fileBundle.1',
  assessment: 'assessment.1',
  checkpoint: 'checkpoint.1',
  'lti activity': 'lti.1',
  diagnostic: 'diagnostic.1',
  pool: 'poolAssessment.1', // probably not real
  'pool assessment': 'poolAssessment.1',
  'observation assessment': 'observationAssessment.1',
  assignment: 'assignment.1',
  'internal course': 'courseLink.1',
  'course link': 'courseLink.1',
};

// These require more than just an empty JSON data blob
const unimportableTypeIds = new Set(['lti.1', 'fileBundle.1', 'scorm.1']);

function validateTitle(titleFields) {
  const titlesArr = titleFields.filter(t => t);
  if (titlesArr.length > 1) {
    return error(TEMPLATE_IMPORT.TOO_MANY_TITLES);
  } else if (titlesArr.length < 1) {
    return error(TEMPLATE_IMPORT.NO_TITLE);
  } else {
    const title = titlesArr.pop();
    if (title.length > 255) {
      return error(TEMPLATE_IMPORT.LONG_TITLE);
    } else {
      return title;
    }
  }
}

function validateLevel(lvl1Title, lvl2Title, lvl3Title, lvl4Title) {
  return lvl1Title
    ? 1
    : lvl2Title
      ? 2
      : lvl3Title
        ? 3
        : lvl4Title
          ? 4
          : error(TEMPLATE_IMPORT.NO_TITLE);
}

function validateTypeId(rawTypeId = '') {
  if (rawTypeId === '') {
    return error(TEMPLATE_IMPORT.MISSING_ASSET_TYPE);
  }
  const typeId = human2TypeId[rawTypeId.toLowerCase()];
  if (!typeId) {
    return error(TEMPLATE_IMPORT.UNSUPPORTED_ASSET_TYPE, { typeId: rawTypeId });
  } else {
    return typeId;
  }
}

function validatePath(currentPath, currentLevel, newNodeName) {
  if (currentPath.length < currentLevel) {
    const lvlX = currentLevel;
    const lvlY = currentPath.length - 1;

    if (lvlY === 0) {
      // no modules have been successfully parsed in this document yet
      return error(TEMPLATE_IMPORT.NO_MODULE);
    } else {
      // a level was skipped
      // Note: At present it appears that the only error this ever produces is L1 -> L3
      const levels = [
        TEMPLATE_IMPORT.LEVEL_ONE,
        TEMPLATE_IMPORT.LEVEL_TWO,
        TEMPLATE_IMPORT.LEVEL_THREE,
        TEMPLATE_IMPORT.LEVEL_FOUR,
      ];

      return error(TEMPLATE_IMPORT.LVLX_CANNOT_FOLLOW_LVLY, {
        lvlX: levels[lvlX - 1],
        lvlY: levels[lvlY - 1],
      });
    }
  } else {
    return [...currentPath.slice(0, currentLevel), newNodeName];
  }
}

function validateSourceNode(currentLevel, currentAssetType, sourceNode) {
  if (
    (currentLevel === 2 && sourceNode.typeId !== 'module.1') ||
    (currentLevel === 3 && sourceNode.typeId !== 'lesson.1')
  ) {
    return error(TEMPLATE_IMPORT.TYPEX_CANNOT_FOLLOW_TYPEY, {
      typeX: currentAssetType,
      typeY: sourceNode.typeId,
    });
  } else {
    return true;
  }
}

function validateStructure(currentLevel, typeId) {
  return currentLevel === 1
    ? validateLvl1(typeId)
    : currentLevel === 2
      ? validateLvl2(typeId)
      : currentLevel === 3
        ? validateLvl3(typeId)
        : error(TEMPLATE_IMPORT.NO_TITLE);
}

function validateLvl1(typeId) {
  // only modules can exist on level 1
  if (typeId !== 'module.1') {
    return error(TEMPLATE_IMPORT.BAD_LVL, { typeId, currentLevel: TEMPLATE_IMPORT.LEVEL_ONE });
  } else {
    return true;
  }
}

function validateLvl2(typeId) {
  // most things can exist on level 2 EXCEPT modules
  if (typeId === 'module.1') {
    return error(TEMPLATE_IMPORT.BAD_LVL, { typeId, currentLevel: TEMPLATE_IMPORT.LEVEL_TWO });
  } else {
    return true;
  }
}

function validateLvl3(typeId) {
  const validLvl3Types = [
    'html.1',
    'scorm.1',
    'resource.1',
    'discussion.1',
    'assessment.1',
    'checkpoint.1',
    'diagnostic.1',
    'poolAssessment.1',
    'assignment.1',
    'lti.1',
    'observationAssessment.1',
    'fileBundle.1',
  ];

  if (!validLvl3Types.includes(typeId)) {
    return error(TEMPLATE_IMPORT.BAD_LVL, {
      typeId,
      currentLevel: TEMPLATE_IMPORT.LEVEL_THREE,
    });
  } else {
    return true;
  }
}

const NonAlpha = /[^a-zA-Z0-9]/g;

function validateCompetencies(typeId, group, titles, competenciesByTitle) {
  const competencyTitles = (titles ?? '')
    .split('\n')
    .map(t => t.trim())
    .filter(t => !!t);
  if (!competencyTitles.length) {
    return [];
  } else if (!edgeRules[typeId][group]) {
    return error(TEMPLATE_IMPORT.BAD_GROUP, { typeId, group });
  } else {
    return competencyTitles.map(
      title =>
        competenciesByTitle[title.replaceAll(NonAlpha, '').toLowerCase()] ||
        warning(TEMPLATE_IMPORT.UNKNOWN_COMPETENCY, { competency: title })
    );
  }
}

const competencyGroups = ['teaches', 'assesses'];

function validateCategory(typeId, title, categoriesByTitle) {
  if (!title?.trim()) {
    return '';
  } else if (!edgeRules[typeId].gradebookCategory) {
    return error(TEMPLATE_IMPORT.BAD_CATEGORY, { typeId });
  } else {
    return (
      categoriesByTitle[title.replaceAll(NonAlpha, '').toLowerCase()] ||
      warning(TEMPLATE_IMPORT.UNKNOWN_CATEGORY, { category: title })
    );
  }
}

/**
 * Validates parsed rows from the csv.
 * @param rows   parsed row objects. ex [{lvl1Title: 'module number 1', typeId: 'module.1'}, {...row 2}]
 * @param edges   structure edges object for siblings for setEdgeOrder (can't we refactor this away?)
 * @param courseNodeName   course node used to build edge ops from.
 * @param competencies     course competency nodes
 * @param categories       course gradebook category nodes
 * @return object ops array and errors array: {ops: [addNode1, addEdge...], errors: [err1, err2]}
 * */
export function validateRows(rows, edges, courseNodeName, competencies, categories) {
  const competenciesByTitle = {};
  for (const competency of competencies ?? []) {
    competenciesByTitle[competency.data.title.replaceAll(NonAlpha, '').toLowerCase()] = competency;
  }
  const categoriesByTitle = {};
  for (const category of categories ?? []) {
    categoriesByTitle[category.data.title.replaceAll(NonAlpha, '').toLowerCase()] = category;
  }
  const { ops, errors, warnings } = rows.reduce(
    (acc, row, idx) => {
      const newNodeName = uniqueId();
      const newEdgeName = uniqueId();

      const titleFields = [row.lvl1Title, row.lvl2Title, row.lvl3Title, row.lvl4Title];

      const title = validateTitle(titleFields);

      const currentLevel = validateLevel(...titleFields);

      const typeId = validateTypeId(row.typeId);

      /*
       * If there're any errors with the fields that set a row's level, use the last known valid path to guess
       * what the current row's source is.
       */
      const newPath =
        title.err || currentLevel.err
          ? acc.path
          : validatePath(acc.path, currentLevel, newNodeName);

      const sourceNodeName = newPath[newPath.length - 2];

      /*
       * We ignore validation for some future rows if there were previous errors.
       * e.g. if we don't have a valid type, why do any source, structure, or assessment validation.
       */
      const ignoreSourceValidation = currentLevel.err || !acc.nodes[sourceNodeName] || typeId.err;
      const sourceNode =
        !ignoreSourceValidation &&
        validateSourceNode(currentLevel, typeId, acc.nodes[sourceNodeName]);

      const ignoreStructureValidation =
        currentLevel.err || sourceNode.err || title.err || typeId.err;
      const structure = !ignoreStructureValidation && validateStructure(currentLevel, typeId);

      const assessmentSettings = !typeId.err && validateAssessmentSettings(row, typeId);

      // if the typeid supports teaches then the optional "LO" column means teaches else assesses
      const loDefault = !typeId.err && edgeRules[typeId]['teaches'] ? 'teaches' : 'assesses';

      const groupCompetencies = Object.fromEntries(
        competencyGroups.map(g => [
          g,
          !typeId.err &&
            validateCompetencies(
              typeId,
              g,
              row[g] || row[g === loDefault ? 'lo' : g],
              competenciesByTitle
            ),
        ])
      );

      const gradebookCategory =
        !typeId.err && validateCategory(typeId, row.category, categoriesByTitle);

      if (
        title.err ||
        currentLevel.err ||
        typeId.err ||
        newPath.err ||
        sourceNode.err ||
        structure.err ||
        Object.values(groupCompetencies).some(v => v.err) ||
        assessmentSettings.errorCollection ||
        gradebookCategory.err
      ) {
        const assessmentErrors = assessmentSettings.errorCollection || [];
        const newErrors = [
          title,
          typeId,
          newPath,
          sourceNode,
          structure,
          ...Object.values(groupCompetencies),
          gradebookCategory,
          ...assessmentErrors,
        ]
          // only collect the fields with errors
          .filter(field => field.err)
          // rowNum = line number on the spreadsheet corresponding to the current row
          .map(error => ({ ...error, rowNum: idx + 2 }));

        acc.errors.push(...newErrors);
        acc.path = newPath.err ? acc.path : newPath;
      } else {
        const orderedSiblingEdgeNames = Object.values({ ...acc.edges, ...edges })
          .filter(e => e.sourceName === sourceNodeName && e.group === 'elements')
          .sort((a, b) => a.position - b.position)
          .map(e => e.name);

        const newNode = {
          name: newNodeName,
          typeId,
          data: { title, ...assessmentSettings },
        };

        const newEdge = {
          name: newEdgeName,
          sourceName: sourceNodeName,
          targetName: newNodeName,
          position: orderedSiblingEdgeNames.length,
          group: 'elements',
        };

        const groupCompetencyEdges = mapValues(groupCompetencies, (competencies, group) =>
          competencies
            .filter(entry => !entry.warn)
            .map(competency => ({
              op: 'addEdge',
              name: uniqueId(),
              sourceName: newNodeName,
              targetName: competency.name,
              group,
              traverse: false,
            }))
        );

        const catName = gradebookCategory.typeId ? uniqueId() : '';
        const catEdge = gradebookCategory.typeId
          ? [
              {
                op: 'addEdge',
                name: catName,
                sourceName: newNodeName,
                targetName: gradebookCategory.name,
                group: 'gradebookCategory',
                traverse: false,
              },
              {
                op: 'setEdgeOrder',
                sourceName: newNodeName,
                group: 'gradebookCategory',
                ordering: [catName],
              },
            ]
          : [];

        const typeIdWarnings = unimportableTypeIds.has(typeId)
          ? [warning(TEMPLATE_IMPORT.UNSUPPORTED_ASSET_TYPE, { typeId })]
          : [];

        const newWarnings = [
          ...typeIdWarnings,
          ...Object.values(groupCompetencies).flat(),
          gradebookCategory,
        ]
          .filter(field => field.warn)
          .map(warning => ({ ...warning, rowNum: idx + 2 }));

        const ops = [
          {
            op: 'addNode',
            name: newNodeName,
            data: newNode.data,
            typeId,
          },
          {
            op: 'addEdge',
            name: newEdgeName,
            sourceName: sourceNodeName,
            targetName: newNode.name,
            group: 'elements',
            traverse: true,
          },
          {
            op: 'setEdgeOrder',
            sourceName: sourceNodeName,
            group: 'elements',
            ordering: orderedSiblingEdgeNames.length
              ? [...orderedSiblingEdgeNames, newEdgeName]
              : [newEdgeName],
          },
          ...Object.entries(groupCompetencyEdges).flatMap(([group, edges]) =>
            edges.length
              ? [
                  ...edges,
                  {
                    op: 'setEdgeOrder',
                    sourceName: newNodeName,
                    group: group,
                    ordering: edges.map(edge => edge.name),
                  },
                ]
              : []
          ),
          ...catEdge,
        ];

        if (!typeIdWarnings.length) {
          acc.nodes[newNodeName] = newNode;
          acc.edges[newEdgeName] = newEdge;
          acc.ops.push(...ops);
          acc.path = newPath;
        }
        acc.warnings.push(...newWarnings);
      }
      return acc;
    },
    {
      nodes: {},
      edges: {},
      ops: [],
      path: [courseNodeName],
      errors: [],
      warnings: [],
    }
  );

  return { ops, errors, warnings };
}

function cleanOps(ops) {
  const compressedSeos = ops.reduce((acc, op) => {
    if (op.op === 'setEdgeOrder') {
      const opIndexToUpdate = acc.findIndex(
        fop =>
          fop.op === 'setEdgeOrder' && fop.sourceName === op.sourceName && fop.group === op.group
      );
      if (opIndexToUpdate >= 0) {
        acc[opIndexToUpdate].ordering = op.ordering;
        return acc;
      } else {
        return [...acc, op];
      }
    } else {
      return [...acc, op];
    }
  }, []);

  const notSeos = [];
  const seos = [];
  for (const op of compressedSeos) {
    op.op === 'setEdgeOrder' ? seos.push(op) : notSeos.push(op);
  }

  return notSeos.concat(seos);
}

function cleanErrors(errors) {
  return errors
    .reduce((acc, error, idx) => {
      // errors that can be safely ignored if they appear on adjacent rows
      const noisyErrors = [
        TEMPLATE_IMPORT.NO_MODULE,
        TEMPLATE_IMPORT.LVLX_CANNOT_FOLLOW_LVLY,
        TEMPLATE_IMPORT.TYPEX_CANNOT_FOLLOW_TYPEY,
      ];
      const prevError = idx > 0 ? acc[idx - 1] : {};

      if (
        noisyErrors.includes(error.err) &&
        prevError.err === error.err &&
        prevError.rowNum === error.rowNum - 1
      ) {
        return [...acc, { ...error, hidden: true }];
      } else {
        return [...acc, error];
      }
    }, [])
    .filter(error => !error.hidden);
}
