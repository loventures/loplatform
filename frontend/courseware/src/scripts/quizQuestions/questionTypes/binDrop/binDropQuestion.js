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

import {
  each,
  every,
  filter,
  find,
  flatMap,
  includes,
  indexOf,
  isEmpty,
  keyBy,
  map,
  range,
  some,
} from 'lodash';
import uiLibInit from '../../../utilities/load-jqui.js';
import { SELECTION_TYPE_BIN_DROP } from '../../../utilities/questionTypes.js';

import template from './binDropQuestion.html';
import baseViewTemplate from './binDropQuestionBaseView.html';
import basePlayTemplate from './binDropQuestionBaseViewPlay.html';
import baseResultsTemplate from './binDropQuestionBaseViewResults.html';
import printViewTemplate from './binDropQuestionPrintView.html';
import printResultsTemplate from './binDropQuestionPrintViewResults.html';

const buildId = (question, index, prefix) => {
  return question.id + '-' + prefix + '-' + index;
};

const getChoices = question => {
  return question.choices
    ? question.choices
    : map(question.binOptions, (binOption, optionIndex) => {
        return {
          ...binOption,
          id: buildId(question, optionIndex, 'choice'),
        };
      });
};

const getBinMapping = question => {
  if (question.binMapping) {
    return question.binMapping;
  }

  let binMapping = {};
  each(question.bins, (bin, binIndex) => {
    binMapping[buildId(question, binIndex, 'bin')] = map(bin.correctOptionIndices, choiceIndex =>
      buildId(question, choiceIndex, 'choice')
    );
  });

  return binMapping;
};

const binDropResultsCtrl = function () {
  this.$onInit = () => {
    this.hasCorrectness = this.question.displayDetail.correctAnswer;
    this.hasScore = this.response ? !isEmpty(this.response.score) : false;
    const binMapping = getBinMapping(this.question);
    const choices = getChoices(this.question);

    this.displayBins = map(this.question.bins, (bin, binIndex) => {
      const binId = bin.id || buildId(this.question, binIndex, 'bin');
      const correctChoices = this.hasCorrectness
        ? map(binMapping[binId], choiceId => find(choices, { id: choiceId }))
        : {};

      const correctMap = keyBy(correctChoices, 'id');

      const selectedChoices =
        !this.response || !this.response.selection
          ? []
          : map(this.response.selection.elementIndexesByGroupIndex[binIndex], choiceIndex => {
              const choice = choices[choiceIndex];
              return {
                ...choice,
                correct: this.hasScore && !!correctMap[choice.id],
                incorrect: this.hasScore && !correctMap[choice.id],
              };
            });

      const selectedMap = keyBy(selectedChoices, 'id');

      return {
        id: binId,
        text: bin.text,
        selected: selectedChoices,
        missing: this.hasCorrectness
          ? filter(correctChoices, choice => !selectedMap[choice.id])
          : [],
      };
    });

    const choicesWithBin = flatMap(binMapping);

    this.choicesWithoutBin =
      this.response || this.hasCorrectness
        ? filter(choices, choice => {
            return choicesWithBin.indexOf(choice.id) === -1;
          })
        : choices;
  };
};

export default angular
  .module('lo.questions.binDropQuestion', [])
  .component('binDropQuestion', {
    bindings: {
      index: '<',
      focusOnRender: '<',
      assessment: '<?',
      questionCount: '<?',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template,
    controller: [
      '$window',
      function ($window) {
        this.inPrintMode = () => $window.inPrintMode;
      },
    ],
  })
  .component('binDropQuestionBaseView', {
    bindings: {
      index: '<',
      focusOnRender: '<',
      assessment: '<?',
      questionCount: '<?',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template: baseViewTemplate,
    controller: function () {},
  })
  .component('binDropQuestionBaseViewPlay', {
    bindings: {
      index: '<',
      focusOnRender: '<',
      assessment: '<?',
      questionCount: '<?',
      question: '<',
      response: '<',
      changeAnswer: '<',
    },
    template: basePlayTemplate,
    controller: function () {
      this.$onInit = () => {
        this.choices = getChoices(this.question);
      };

      this.$onChanges = ({ response }) => {
        if (response) {
          this.selection = this.responseToSelection(response.currentValue);
        }
      };

      this.responseToSelection = response => {
        if (!response || !response.selection || !response.selection.elementIndexesByGroupIndex) {
          return {
            selected: {},
            unselected: range(0, getChoices(this.question).length),
          };
        }

        return {
          selected: { ...response.selection.elementIndexesByGroupIndex },
          unselected: filter(range(0, getChoices(this.question).length), choiceIndex =>
            every(
              response.selection.elementIndexesByGroupIndex,
              selectedForBin => indexOf(selectedForBin, choiceIndex) === -1
            )
          ),
        };
      };

      this.selectionToResponse = () => {
        const response = this.response || {};
        let selection = response.selection || {
          responseType: SELECTION_TYPE_BIN_DROP,
        };
        const hasSelected = some(this.selection.selected, bin => !isEmpty(bin));

        if (hasSelected) {
          selection.elementIndexesByGroupIndex = this.selection.selected;
        } else {
          selection = null;
        }

        return {
          ...response,
          selection,
        };
      };

      this.activateChoice = (choiceIndex, $event) => {
        if ($event) {
          $event.stopPropagation();
        }

        if (this.activeChoiceIndex === choiceIndex) {
          this.activeChoiceIndex = null;
        } else {
          this.activeChoiceIndex = choiceIndex;
        }
      };

      this.dropToBin = binIndex => {
        if (this.activeChoiceIndex !== null && this.activeChoiceIndex !== void 0) {
          this.moveChoice(this.activeChoiceIndex, binIndex);
        }
      };

      this.moveChoice = (choiceIndex, binIndex) => {
        each(this.selection.selected, selectedForBin => {
          const index = indexOf(selectedForBin, choiceIndex);
          if (index !== -1) {
            selectedForBin.splice(index, 1);
          }
        });

        if (binIndex !== -1) {
          if (!this.selection.selected[binIndex]) {
            this.selection.selected[binIndex] = [];
          }
          this.selection.selected[binIndex].push(choiceIndex);
        } else if (!includes(this.selection.unselected, choiceIndex)) {
          this.selection.unselected.push(choiceIndex);
        }

        this.changeAnswer(this.index, this.selectionToResponse());

        this.activeChoiceIndex = null;
      };
    },
  })
  .directive('binDropDraggable', function () {
    return {
      restrict: 'A',
      controller: [
        '$element',
        function ($element) {
          uiLibInit(() =>
            $element.draggable({
              revert: 'invalid',
              helper: 'clone',
              start: () => {
                $element.addClass('active');
              },
              stop: () => {
                $element.removeClass('active');
              },
            })
          );
        },
      ],
    };
  })
  .directive('binDropDroppable', function () {
    return {
      restrict: 'A',
      scope: {
        binDropDroppable: '<',
      },
      controller: [
        '$scope',
        '$element',
        '$timeout',
        function ($scope, $element, $timeout) {
          uiLibInit(() =>
            $element.droppable({
              hoverClass: 'bin-hover',
              tolerance: 'pointer',
              drop: (e, ui) => {
                const choiceIndex = +ui.draggable.attr('choice-index');
                const binIndex = +e.target.getAttribute('bin-index');
                $timeout(() => {
                  $scope.binDropDroppable(choiceIndex, binIndex);
                });
              },
            })
          );
        },
      ],
    };
  })
  .component('binDropQuestionBaseViewResults', {
    template: baseResultsTemplate,
    bindings: {
      question: '<',
      response: '<',
    },
    controller: binDropResultsCtrl,
  })
  .component('binDropQuestionPrintView', {
    bindings: {
      index: '<',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template: printViewTemplate,
    controller: function () {},
  })
  .component('binDropQuestionPrintViewResults', {
    bindings: {
      index: '<',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template: printResultsTemplate,
    controller: binDropResultsCtrl,
  });
