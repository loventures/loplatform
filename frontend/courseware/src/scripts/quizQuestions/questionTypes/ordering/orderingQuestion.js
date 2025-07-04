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

import { every, find, get, isEmpty, isNil, isObject, map, orderBy, range, shuffle } from 'lodash';
import uiLibInit from '../../../utilities/load-jqui.js';
import { SELECTION_TYPE_ORDERING } from '../../../utilities/questionTypes.js';

import questionResultModeToggle from '../../questionAddons/questionResultModeToggle.js';
import questionTemplate from '../../questionTemplates/basicQuestionTemplate.js';
import choiceRowTemplate from '../../questionTemplates/questionDistractorRowTemplate.js';
import orderingQuestion from './orderingQuestion.html';
import orderingQuestionBaseView from './orderingQuestionBaseView.html';
import orderingQuestionBaseViewPlay from './orderingQuestionBaseViewPlay.html';
import orderingQuestionBaseViewResults from './orderingQuestionBaseViewResults.html';
import orderingQuestionPrintView from './orderingQuestionPrintView.html';

const getChoices = question =>
  question.renderOrderChoices ? question.renderOrderChoices : question.choices;

const getChoiceText = choice => (isObject(choice.text) ? choice.text.html : choice.text);

const getDisplayAnswer = question => {
  if (isEmpty(question.correctOrder)) {
    return [];
  }

  const choices = getChoices(question);
  const transformed = map(choices, (choice, originalIndex) => {
    const correctIndex = question.correctOrder[originalIndex];
    return {
      ...choice,
      text: getChoiceText(choice),
      correctIndex,
      showAsCorrect: true,
    };
  });

  return orderBy(transformed, 'correctIndex');
};

const getDisplayOrder = (question, response) => {
  const order = get(response, 'selection.order', []);
  const choices = getChoices(question);
  if (isEmpty(order)) {
    return range(0, choices.length);
  }
  return response.selection.order;
};

const responseToSelection = (question, response, displayedChoices) => {
  const order = getDisplayOrder(question, response);
  const choices = getChoices(question);
  return map(choices, (choice, originalIndex) => {
    const displayIndex = order.indexOf(originalIndex);
    const displayedChoice = find(displayedChoices, {
      originalIndex: originalIndex,
    });
    const text = getChoiceText(choice);
    const selected = get(displayedChoice, 'selected', false);
    return {
      ...choice,
      text,
      selected,
      originalIndex,
      displayIndex,
    };
  });
};

const getDisplayResponse = (question, response) => {
  if (isEmpty(question.correctOrder)) {
    return [];
  }

  const choices = getChoices(question);
  const transformed = map(choices, (choice, originalIndex) => {
    /**
     * The selection order array is not a guess at the correct order. It is the index of choice in the original display
     * order. Therefore we must find the index at which the displayed choice was eventually placed.
     */
    const answerIndex = response?.selection?.order.indexOf(originalIndex) ?? originalIndex;
    const correctIndex = question.correctOrder[originalIndex];
    return {
      ...choice,
      text: getChoiceText(choice),
      correctIndex,
      answerIndex,
      showAsCorrect: correctIndex === answerIndex,
    };
  });

  return orderBy(transformed, 'answerIndex');
};

export default angular
  .module('lo.questions.orderingQuestion', [
    questionResultModeToggle.name,
    questionTemplate.name,
    choiceRowTemplate.name,
  ])
  .component('orderingQuestion', {
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
    template: orderingQuestion,
    controller: [
      '$window',
      function ($window) {
        this.inPrintMode = () => $window.inPrintMode;
      },
    ],
  })
  .component('orderingQuestionBaseView', {
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
    template: orderingQuestionBaseView,
    controller: function () {},
  })
  .component('orderingQuestionBaseViewPlay', {
    bindings: {
      index: '<',
      focusOnRender: '<',
      question: '<',
      response: '<',
      changeAnswer: '<',
    },
    template: orderingQuestionBaseViewPlay,
    controller: [
      '$element',
      '$timeout',
      function ($element, $timeout) {
        this.$onInit = () => {
          uiLibInit(() => this.initSortableUI());
          this.savedNodes = [];
          this.initFocusOnRender = false;
        };

        this.initSortableUI = () => {
          $element.find('.question-distractor-list').sortable({
            stop: (e, ui) => {
              const newIndex = +ui.item.index();
              const oldIndex = +ui.item.attr('display-index');
              this.moveTo(oldIndex, newIndex);
              $timeout(() => {});
            },
          });
        };

        this.$onChanges = ({ response }) => {
          if (response) {
            this.displayChoices = responseToSelection(
              this.question,
              response.currentValue,
              this.displayChoices
            );
            this.recalculatePositions();
          }
        };

        this.selectionToResponse = order => {
          const response = this.response || {};
          const selection = response.selection || {
            responseType: SELECTION_TYPE_ORDERING,
          };
          return {
            ...response,
            selection: {
              ...selection,
              order,
            },
          };
        };

        this.moveTo = (from, to, refocus) => {
          if (to < 0 || to >= this.displayChoices.length) {
            return;
          }
          if (refocus && from > to) {
            this.refocusAfterMoveUp = to;
          }
          const order = map(orderBy(this.displayChoices, 'displayIndex'), 'originalIndex');
          const [choiceIndex] = order.splice(from, 1);
          order.splice(to, 0, choiceIndex);

          this.changeAnswer(this.index, this.selectionToResponse(order));
        };

        /*
            similar to matching question
        */
        this.choiceRendered = choice => {
          $timeout(() => {
            this.savedNodes[choice.originalIndex] = $element.find(
              `[original-index="${choice.originalIndex}"]`
            );
            this.allChoicesRendered = every(
              this.displayChoices,
              choice =>
                this.savedNodes[choice.originalIndex] &&
                this.savedNodes[choice.originalIndex].length
            );
            this.recalculatePositions();
          });
        };

        this.recalculatePositions = () => {
          if (!this.allChoicesRendered) {
            return;
          }

          //keep this log so we can monitor how often we do this
          console.log('ordering question position recalculate');

          this.cumulativeHeight = 0;
          this.tops = [];

          const offset = 20;

          for (let row = 0; row < this.displayChoices.length; row++) {
            const rowOriginalIndex = find(this.displayChoices, {
              displayIndex: row,
            }).originalIndex;
            let rowHeight = this.savedNodes[rowOriginalIndex].height();
            this.tops[row] = this.cumulativeHeight;
            this.cumulativeHeight += rowHeight + offset;
          }

          if (!this.initFocusOnRender && this.focusOnRender) {
            const buttons = $element.find('[display-index="0"] button');
            buttons.get(1).focus();
            this.initFocusOnRender = true;
          }

          if (!isNil(this.refocusAfterMoveUp)) {
            $timeout(() => {
              const choice = $element.find(`[display-index="${this.refocusAfterMoveUp}"]`);
              choice.find('button').first().focus();
              this.refocusAfterMoveUp = null;
            });
          }
        };
      },
    ],
  })
  .component('orderingQuestionBaseViewResults', {
    bindings: {
      question: '<',
      response: '<',
    },
    template: orderingQuestionBaseViewResults,
    controller: function () {
      this.$onInit = () => {
        if (this.response) {
          this.showResponse();
        } else {
          this.showAnswer();
        }
      };

      this.showAnswer = () => {
        this.displayChoices = getDisplayAnswer(this.question);
      };

      this.showResponse = () => {
        this.displayChoices = getDisplayResponse(this.question, this.response);
      };
    },
  })
  .component('orderingQuestionPrintView', {
    bindings: {
      index: '<',
      assessment: '<?',
      questionCount: '<?',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template: orderingQuestionPrintView,
    controller: function () {
      this.$onInit = () => {
        this.displayChoices = getDisplayResponse(this.question, this.response);
        this.correctChoices = getDisplayAnswer(this.question);
        if (!this.response && !this.question.displayDetail.correctAnswer)
          this.displayChoices = shuffle(this.displayChoices);
      };

      this.$onChanges = ({ response }) => {
        if (response && response.currentValue) {
          this.displayChoices = responseToSelection(
            this.question,
            response.currentValue,
            this.displayChoices
          );
        }
      };
    },
  });
