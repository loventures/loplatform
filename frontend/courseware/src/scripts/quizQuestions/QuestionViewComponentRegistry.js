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

import renderComponent from '../utilities/renderComponent.jsx';

import {
  QUESTION_TYPE_MULTIPLE_CHOICE,
  QUESTION_TYPE_LEGACY_MULTIPLE_CHOICE,
  QUESTION_TYPE_TRUE_FALSE,
  QUESTION_TYPE_LEGACY_TRUE_FALSE,
  QUESTION_TYPE_MULTIPLE_SELECT,
  QUESTION_TYPE_LEGACY_MULTIPLE_SELECT,
  QUESTION_TYPE_ORDERING,
  QUESTION_TYPE_LEGACY_ORDERING,
  QUESTION_TYPE_BIN_DROP,
  QUESTION_TYPE_LEGACY_BIN_DROP,
  QUESTION_TYPE_MATCHING,
  QUESTION_TYPE_LEGACY_MATCHING,
  QUESTION_TYPE_FILL_BLANK,
  QUESTION_TYPE_LEGACY_FILL_BLANK,
  QUESTION_TYPE_ESSAY,
  QUESTION_TYPE_LEGACY_ESSAY,
  QUESTION_TYPE_HOTSPOT,
  QUESTION_TYPE_LEGACY_HOTSPOT,
} from '../utilities/questionTypes.js';

export default angular
  .module('lo.questions.QuestionViewComponentRegistry', [])
  .constant('QuestionViewComponentMapping', {
    //simple choice questions
    [QUESTION_TYPE_MULTIPLE_CHOICE]: 'multiple-choice-question',
    [QUESTION_TYPE_LEGACY_MULTIPLE_CHOICE]: 'multiple-choice-question',
    [QUESTION_TYPE_TRUE_FALSE]: 'multiple-choice-question',
    [QUESTION_TYPE_LEGACY_TRUE_FALSE]: 'multiple-choice-question',
    [QUESTION_TYPE_MULTIPLE_SELECT]: 'multiple-select-question',
    [QUESTION_TYPE_LEGACY_MULTIPLE_SELECT]: 'multiple-select-question',
    //fancy ui choice questions
    [QUESTION_TYPE_ORDERING]: 'ordering-question',
    [QUESTION_TYPE_LEGACY_ORDERING]: 'ordering-question',
    [QUESTION_TYPE_BIN_DROP]: 'bin-drop-question',
    [QUESTION_TYPE_LEGACY_BIN_DROP]: 'bin-drop-question',
    [QUESTION_TYPE_MATCHING]: 'matching-question',
    [QUESTION_TYPE_LEGACY_MATCHING]: 'matching-question',
    //other
    [QUESTION_TYPE_FILL_BLANK]: 'fill-blank-question',
    [QUESTION_TYPE_LEGACY_FILL_BLANK]: 'fill-blank-question',
    [QUESTION_TYPE_ESSAY]: 'essay-question',
    [QUESTION_TYPE_LEGACY_ESSAY]: 'essay-question',
    [QUESTION_TYPE_HOTSPOT]: 'hotspot-question',
    [QUESTION_TYPE_LEGACY_HOTSPOT]: 'hotspot-question',
  })
  .provider('QuestionViewComponentRegistry', [
    'QuestionViewComponentMapping',
    function (QuestionViewComponentMapping) {
      this.registry = QuestionViewComponentMapping;

      this.$get = function () {
        var service = {};

        service.getRenderedTemplate = (question, componentArgs) => {
          const componentName = this.registry[question._type];

          return renderComponent(componentName, componentArgs);
        };

        return service;
      };
    },
  ]);
