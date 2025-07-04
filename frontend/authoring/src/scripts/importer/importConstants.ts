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

import { fromPairs } from 'lodash';

import { TypeId } from '../types/asset';

export const importExtensions = {
  qti: ['.zip'],
  csv: ['.csv'],
  loqi: ['.docx'], // loqi: ['.docx', '.txt'],
  zip: ['.zip'],
  html: ['.html', '.htm'],
};

export const QUIZ_TYPE_IDS: TypeId[] = [
  'assessment.1',
  'checkpoint.1',
  'diagnostic.1',
  'poolAssessment.1',
  'survey.1',
];

const dropZip = {
  'application/zip': importExtensions.zip,
};

const dropHtml = {
  'text/html': importExtensions.html,
};

const dropCsv = {
  'text/csv': importExtensions.csv,
};

const dropDocx = {
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': importExtensions.loqi,
};

const questionImportExtensions = {
  ...dropZip,
  ...dropDocx,
};

export const assetToExtensions = {
  ...fromPairs(QUIZ_TYPE_IDS.map(tid => [tid, questionImportExtensions])),
  'lesson.1': { ...dropZip, ...dropHtml },
  'module.1': { ...dropZip, ...dropHtml },
  'unit.1': { ...dropZip, ...dropHtml },
  'course.1': { ...dropCsv, ...dropZip },
};
