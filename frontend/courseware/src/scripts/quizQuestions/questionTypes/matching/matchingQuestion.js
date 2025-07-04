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
  compact,
  each,
  every,
  filter,
  find,
  first,
  invert,
  isEmpty,
  isNil,
  map,
  mapValues,
  pick,
  range,
  some,
} from 'lodash';
import uiLibInit from '../../../utilities/load-jqui.js';
import { SELECTION_TYPE_MATCHING } from '../../../utilities/questionTypes.js';

import questionTemplate from '../../questionTemplates/basicQuestionTemplate.js';
import choiceRowTemplate from '../../questionTemplates/questionDistractorRowTemplate.js';
import template from './matchingQuestion.html';
import baseViewTemplate from './matchingQuestionBaseView.html';
import basePlayTemplate from './matchingQuestionBaseViewPlay.html';
import baseResultsTemplate from './matchingQuestionBaseViewResults.html';
import rowsTemplate from './matchingQuestionRows.html';

const buildChoiceId = (question, index, prefix) => {
  return question.id + '-' + prefix + '-' + index;
};

const matchingQuestionBaseViewCtrl = function () {
  const createQuestionView = question => {
    //we are creating these because we want
    //them to remain the same throughout
    //so that animation works
    const rowRange = range(0, Math.max(question.terms.length, question.definitions.length));
    const rows = map(rowRange, rowIndex => ({ rowIndex }));

    const terms = map(question.terms, (term, termIndex) => {
      return {
        ...term,
        id: term.id || buildChoiceId(question, termIndex, 'term'),
        termIndex,
      };
    });

    const defs = map(question.definitions, (def, defIndex) => {
      return {
        ...def,
        id: def.id || buildChoiceId(question, defIndex, 'def'),
        defIndex,
      };
    });

    return {
      rows,
      terms,
      defs,
    };
  };

  this.$onChanges = ({ response, question }) => {
    if (question && question.isFirstChange()) {
      const { rows, terms, defs } = createQuestionView(question.currentValue);
      this.rows = rows;
      this.terms = terms;
      this.defs = defs;
    }

    if (response) {
      this.responseToSelection(response.currentValue);
    }
  };

  this.responseToSelection = response => {
    const termToDefMatches =
      response && response.selection
        ? mapValues(response.selection.elementIndexesByGroupIndex, first)
        : {};

    const defToTermMatches = invert(termToDefMatches);

    each(this.terms, term => {
      const matchedDefIndex = termToDefMatches[term.termIndex];
      term.matchedDef = isNil(matchedDefIndex) ? null : this.defs[matchedDefIndex];
    });

    each(this.defs, def => {
      const matchedTermIndex = defToTermMatches[def.defIndex];
      def.matchedTerm = isNil(matchedTermIndex) ? null : this.terms[matchedTermIndex];
    });

    //first fill ones that are matched from terms
    each(this.rows, row => {
      row.term = this.terms[row.rowIndex];
      if (row.term) {
        row.term.rowIndex = row.rowIndex;
      }
      row.def = row.term ? row.term.matchedDef : null;
      if (row.def) {
        row.def.rowIndex = row.rowIndex;
      }
      row.isMatched = row.term && row.def;
    });

    const unmatchedDefs = filter(this.defs, def => !def.matchedTerm);
    each(this.rows, row => {
      if (!row.isMatched && !row.def && unmatchedDefs.length) {
        row.def = unmatchedDefs.shift();
        row.def.rowIndex = row.rowIndex;
      }
    });

    this.responseReady = true;
  };
};

const getTermMapping = question => {
  if (question.termMapping) {
    return question.termMapping;
  } else if (question.correctDefinitionForTerm) {
    let termMapping = {};
    each(question.correctDefinitionForTerm, (defIndex, termIndex) => {
      termMapping[buildChoiceId(question, termIndex, 'term')] = buildChoiceId(
        question,
        defIndex,
        'def'
      );
    });
    return termMapping;
  }
  return {};
};

const createResponseRows = (question, rows) => {
  const termMapping = getTermMapping(question);
  const defMapping = invert(termMapping);
  return map(rows, ({ rowIndex, term, def, isMatched }) => {
    const responseRow = {
      rowIndex,
      term: term && pick(term, ['id', 'text']),
      def: def && pick(def, ['id', 'text']),
      isMatched: !!isMatched,
    };

    if (term && def) {
      responseRow.isCorrect = isMatched && termMapping[term.id] === def.id;
    } else if (term && !def) {
      responseRow.isCorrect = !termMapping[term.id];
    } else if (!term && def) {
      responseRow.isCorrect = !defMapping[def.id];
    }
    return responseRow;
  });
};

const createCorrectAnswerRows = (question, rows, defs) => {
  const termMapping = getTermMapping(question);
  const correctAnswerRows = compact(
    map(rows, ({ rowIndex, term }) => {
      if (!termMapping) {
        return null;
      }
      const def = term && find(defs, { id: termMapping[term.id] });
      return {
        rowIndex,
        term: term && pick(term, ['id', 'text']),
        def: def && pick(def, ['id', 'text']),
        isCorrect: true,
        isMatched: !!(term && def),
      };
    })
  );

  const defMapping = invert(termMapping);
  const unmatchedDefs = filter(defs, def => !defMapping[def.id]);
  each(correctAnswerRows, row => {
    if (!row.def && unmatchedDefs.length) {
      row.def = pick(unmatchedDefs.pop(), ['id', 'text']);
    }
  });
  return correctAnswerRows;
};

export default angular
  .module('lo.questions.matchingQuestion', [questionTemplate.name, choiceRowTemplate.name])
  .component('matchingQuestion', {
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
  .component('matchingQuestionBaseView', {
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
    controller: matchingQuestionBaseViewCtrl,
  })
  .component('matchingQuestionBaseViewPlay', {
    bindings: {
      index: '<',
      focusOnRender: '<',
      question: '<',
      rows: '<',
      terms: '<',
      defs: '<',
      response: '<',
      changeAnswer: '<',
    },
    template: basePlayTemplate,
    controller: [
      '$element',
      '$timeout',
      function ($element, $timeout) {
        this.$onInit = () => {
          this.allItemsRendered = false;
          uiLibInit(() =>
            $timeout(() => {
              this.uiLibInitDone = true;
            })
          );
        };

        this.$onChanges = ({ response }) => {
          if (response) {
            $timeout(() => this.recalculatePositions(), 160);
          }
        };
        this.selectionToResponse = matches => {
          let selection = this.response.selection || {
            responseType: SELECTION_TYPE_MATCHING,
          };
          const hasMatches = some(matches, match => !isEmpty(match));

          if (hasMatches) {
            selection.elementIndexesByGroupIndex = matches;
          } else {
            selection = null;
          }

          return {
            ...this.response,
            selection,
          };
        };

        this.activeTerm = null;
        this.activeDef = null;

        this.setActiveTerm = termIndex => {
          if (this.activeTerm === termIndex) {
            this.activeTerm = null;
            return;
          }

          this.activeTerm = termIndex;
          this.match(this.activeTerm, this.activeDef);
        };

        this.setActiveDef = defIndex => {
          if (this.activeDef === defIndex) {
            this.activeDef = null;
            return;
          }

          this.activeDef = defIndex;
          this.match(this.activeTerm, this.activeDef);
        };

        this.match = (term, def) => {
          if (term === null || def === null) {
            //missing stuff
            return;
          }

          const matches = {};
          each(this.rows, row => {
            if (row.term) {
              matches[row.term.termIndex] = row.isMatched ? [row.def.defIndex] : [];
            }
          });

          if (matches[term.termIndex][0] === def.defIndex) {
            //unmatch for already matched term/def
            matches[term.termIndex] = [];
          } else {
            if (def.matchedTerm) {
              matches[def.matchedTerm.termIndex] = [];
            }
            matches[term.termIndex][0] = def.defIndex;
          }

          this.changeAnswer(this.index, this.selectionToResponse(matches));

          if (this.highlightedRow !== -1) {
            //because terms never move
            this.highlightedRow = term.rowIndex;
          }

          this.activeTerm = null;
          this.activeDef = null;
        };

        /*
            things for variable heights
            needs all things to render before calculating
            the timeouts is because ng-init triggers during the cycle of rendering
            and it actually shows up on the next cycle
        */
        this.termRendered = term => {
          $timeout(() => {
            term.savedNode = $element.find('#match-term-' + term.id);
            this.checkRenderingStatus();
            this.recalculatePositions();
            if (this.focusOnRender && term.rowIndex === 0) {
              term.savedNode.focus();
            }
          });
        };

        this.defRendered = def => {
          $timeout(() => {
            def.savedNode = $element.find('#match-def-' + def.id);
            this.checkRenderingStatus();
            this.recalculatePositions();
          });
        };

        this.checkRenderingStatus = () => {
          this.allItemsRendered = every(
            this.rows,
            row =>
              (row.term ? !!row.term.savedNode : true) && (row.def ? !!row.def.savedNode : true)
          );
        };

        this.recalculatePositions = () => {
          if (!this.allItemsRendered) {
            return;
          }

          //keep this log so we can monitor how often we do this
          console.log('matching question position recalculate', this.question.id);

          this.cumulativeHeight = 10; //this is linked to the top space in css
          this.heights = [];
          this.tops = [];

          const offset = 20;

          for (let rowIndex = 0; rowIndex < this.rows.length; rowIndex++) {
            const row = this.rows[rowIndex];
            let termHeight = row.term ? row.term.savedNode.height() : 0;
            let defHeight = row.def ? row.def.savedNode.height() : 0;

            let rowHeight = Math.max(termHeight, defHeight);

            this.heights[rowIndex] = rowHeight;
            this.tops[rowIndex] = this.cumulativeHeight;
            this.cumulativeHeight += rowHeight + offset;
          }

          this.cumulativeHeight += 10; //again, linked to the bottom space in css
        };

        this.highlightedRow = -1;
      },
    ],
  })
  .component('matchingQuestionBaseViewResults', {
    template: baseResultsTemplate,
    bindings: {
      question: '<',
      rows: '<',
      terms: '<',
      defs: '<',
      response: '<',
    },
    controller: function () {
      this.$onInit = () => {
        this.hasScore = false;
        if (this.response) {
          this.hasScore = !isEmpty(this.response.score);
          this.displayRows = this.responseRows;
        } else {
          this.displayRows = this.correctAnswerRows;
        }
      };

      this.$onChanges = ({ rows }) => {
        if (rows && rows.currentValue) {
          this.responseRows = createResponseRows(this.question, rows.currentValue);
          this.correctAnswerRows = createCorrectAnswerRows(
            this.question,
            rows.currentValue,
            this.defs
          );
        }
      };
    },
  })
  .component('matchingQuestionRows', {
    bindings: {
      question: '<',
      displayRows: '<',
      correctRows: '<',
      hasScore: '<',
    },
    template: rowsTemplate,
    controller: function () {},
  });
