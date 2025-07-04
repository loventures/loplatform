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

import generateBinDrop from './generators/binDrop-generator';
import generateEssay from './generators/essay-generator';
import generateFillInTheBlank from './generators/fillInTheBlank-generator';
import generateMatching from './generators/matching-generator';
import generateMultipleChoice from './generators/multipleChoice-generator';
import generateMultipleSelect from './generators/multipleSelect-generator';
import generateOrdering from './generators/ordering-generator';
import generateSurveyChoice from './generators/surveyChoiceQuestion-generator';
import generateSurveyEssay from './generators/surveyEssayQuestion-generator';
import generateTrueFalse from './generators/trueFalse-generator';

export const getDefaultEdgeData = () => ({
  performanceGate: {
    threshold: 0.8,
  },
});
const generateRatingScale = asset => {
  return {
    max: 10,
    lowRatingText: 'Not at all likely',
    highRatingText: 'Very likely',
    ...asset,
  };
};

const assetGenerators = {
  'binDropQuestion.1': generateBinDrop,
  'essayQuestion.1': generateEssay,
  'fillInTheBlankQuestion.1': generateFillInTheBlank,
  'matchingQuestion.1': generateMatching,
  'multipleChoiceQuestion.1': generateMultipleChoice,
  'multipleSelectQuestion.1': generateMultipleSelect,
  'orderingQuestion.1': generateOrdering,
  'ratingScaleQuestion.1': generateRatingScale,
  'trueFalseQuestion.1': generateTrueFalse,
  'surveyEssayQuestion.1': generateSurveyEssay,
  'surveyChoiceQuestion.1': generateSurveyChoice,
};
const edgeDataGenerators = {
  gates: getDefaultEdgeData,
};
const schemaValidAsset = {
  title: '(No Title)',
  archived: false,
};

const generateCompleteAsset = (asset, typeId) => {
  const schemaValidDefaultAsset = {
    ...schemaValidAsset,
    ...asset,
  };

  if (assetGenerators[typeId]) {
    return assetGenerators[typeId](schemaValidDefaultAsset);
  }

  return schemaValidDefaultAsset;
};

const generateEdgeData = group => {
  if (edgeDataGenerators[group]) {
    return edgeDataGenerators[group]();
  }
  return {};
};

export { generateCompleteAsset, generateEdgeData };
