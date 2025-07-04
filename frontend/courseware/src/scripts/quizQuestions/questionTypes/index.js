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

import multipleChoice from './multipleChoice/multipleChoiceQuestion.js';
import multipleSelect from './multipleSelect/multipleSelectQuestion.js';
import ordering from './ordering/orderingQuestion.js';
import binDrop from './binDrop/binDropQuestion.js';
import matching from './matching/matchingQuestion.js';
import fillBlank from './fillBlank/fillBlankQuestion.jsx';
import essay from './essay/essayQuestion.js';
import hotspot from './hotspot/hotspotQuestion.js';

angular.module('lo.quiz.questionTypes', [
  multipleChoice.name,
  multipleSelect.name,
  ordering.name,
  binDrop.name,
  matching.name,
  fillBlank.name,
  essay.name,
  hotspot.name,
]);

export default angular.module('lo.quiz.questionTypes');
