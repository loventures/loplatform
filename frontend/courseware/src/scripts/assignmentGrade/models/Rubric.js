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
  defaults,
  each,
  every,
  extend,
  get,
  isEqual,
  isString,
  map,
  reduce,
  some,
  uniq,
  isNumber,
} from 'lodash';

export default angular
  .module('lo.assignmentGrade.Rubric', [])
  .service('RubricService', function () {
    var service = {};

    service.defensivelyParseRubric = function (rubric) {
      // TODO: sections.sections seems to be a bug
      // from some weird json marshalling from the server,
      // why does it come back as a string anyway?
      let criterion = rubric.criteria || rubric.sections;
      each(criterion, function (section) {
        let sectionPoints = 0;
        each(section.levels, function (level) {
          level.points = parseFloat(level.points);
          if (level.points > sectionPoints) {
            sectionPoints = level.points;
          }
        });
        section.points = parseFloat(section.points) || sectionPoints;
        section.title = section.title || section.name;
      });
      return criterion;
    };

    return service;
  })
  .service('Rubric', [
    'RubricService',
    'RubricSection',
    function (RubricService, RubricSection) {
      var Rubric = function (rubric, response, onChange) {
        if (isString(rubric)) {
          try {
            rubric = angular.fromJson(rubric);
          } catch (error) {
            console.error('Could not parse rubric JSON!', error);
            return;
          }
        }

        this.title = rubric.title;
        this.subtitle = rubric.subtitle;
        this.description = rubric.description;
        this.onChange = onChange;

        var sections = RubricService.defensivelyParseRubric(rubric);

        response = response || {};
        this.sections = map(
          sections,
          function (section, index) {
            section.index = index;
            return new RubricSection(
              section,
              response[index],
              function () {
                this.onChange(this);
              }.bind(this)
            );
          }.bind(this)
        );
      };

      Rubric.prototype.getScore = function () {
        return reduce(
          this.sections,
          function (score, section) {
            var sectionScore = section.getScore();
            return {
              pointsAwarded: score.pointsAwarded + sectionScore.pointsAwarded,
              pointsPossible: score.pointsPossible + sectionScore.pointsPossible,
            };
          },
          {
            pointsAwarded: 0,
            pointsPossible: 0,
          }
        );
      };

      Rubric.prototype.getCompetencies = function () {
        return uniq(
          reduce(
            this.sections,
            (competencies, section) => [...competencies, ...section.competencies],
            []
          )
        );
      };

      Rubric.prototype.setClean = function () {
        each(this.sections, function (section) {
          section.dirty = false;
        });
      };

      Rubric.prototype.getResponse = function () {
        var response = {};

        each(this.sections, function (section, index) {
          response[index] = section.getResponse();
        });

        return response;
      };

      Rubric.prototype.getCriterionResponse = function (forLWC) {
        var response = {};

        each(this.sections, function (section, index) {
          const criterionResponse = section.getCriterionResponse(forLWC);
          if (criterionResponse) {
            if (forLWC) {
              response[section.name] = criterionResponse;
            } else {
              response[index] = criterionResponse;
            }
          }
        });

        return response;
      };

      Rubric.prototype.getCriterionFeedback = function () {
        var criterionFeedback = {};
        each(this.sections, function (section) {
          if (section.feedback) {
            criterionFeedback[section.name] = section.feedback;
          }
        });

        return criterionFeedback;
      };

      Rubric.prototype.isDirty = function () {
        return some(this.sections, function (section) {
          return section.dirty;
        });
      };

      Rubric.prototype.isValid = function () {
        return every(this.sections, function (section) {
          return section.isValid();
        });
      };

      return Rubric;
    },
  ])
  .service('RubricSection', function () {
    var RubricSection = function (section, response = {}, onChange) {
      extend(this, section);

      this.onChange = onChange;

      each(this.levels, function (level, index) {
        level.index = index;
      });

      response = defaults(
        {},
        response,
        {
          //for new shapes of rubric response in questions
          levelIndex: get(response, 'index'),
          levelGrade: get(response, 'points'),
          manual: response.type ? response.type === 'manual' : response.manual,
        },
        {
          //defaults
          manual: false,
          feedback: '',
        }
      );

      this.selectionLevelIndex = response.levelIndex;
      this.isSelectionManual = response.manual;
      this.selectedPoints = response.levelGrade;
      this.feedback = response.feedback || response.criterionFeedback;
      this.feedbackStatus = !!this.feedback;

      this.dirty = false;

      this.initial = {
        response: {
          levelIndex: response.levelIndex,
          manual: response.manual,
          levelGrade: response.levelGrade,
          feedback: response.feedback,
        },
      };
    };

    RubricSection.prototype.getScore = function () {
      return {
        pointsAwarded: isNaN(this.selectedPoints) ? 0 : this.selectedPoints,
        pointsPossible: this.points,
      };
    };

    RubricSection.prototype.getResponse = function () {
      return {
        levelGrade: this.selectedPoints,
        levelIndex: this.selectionLevelIndex,
        manual: this.isSelectionManual,
        feedback: this.feedback,
      };
    };

    RubricSection.prototype.getCriterionResponse = function (forLWC) {
      if (!isNumber(this.selectedPoints)) {
        return null;
      }

      if (forLWC) {
        return this.getScore();
      }

      const response = {
        points: this.selectedPoints,
        criterionFeedback: this.feedback,
      };

      if (this.isSelectionManual) {
        return {
          ...response,
          type: 'manual',
        };
      } else {
        return {
          ...response,
          type: 'byIndex',
          index: this.selectionLevelIndex,
        };
      }
    };

    RubricSection.prototype.setFeedback = function (feedback) {
      this.feedback = feedback;
      this.dirty = !isEqual(this.getResponse(), this.initial.response);
    };

    RubricSection.prototype.reset = function (manual) {
      this.set(manual, -1, void 0);
    };

    RubricSection.prototype.set = function (manual, index, points) {
      this.isSelectionManual = manual;
      this.selectedPoints = points;
      this.selectionLevelIndex = index;
      this.dirty = !isEqual(this.getResponse(), this.initial.response);
      this.onChange(this);
    };

    RubricSection.prototype.setManual = function (manualPoints) {
      if (this.isValidManualScore(manualPoints)) {
        this.isSelectionManual = true;
        this.set(true, -1, manualPoints);
      } else {
        this.reset(true);
      }
    };

    RubricSection.prototype.setSelection = function (levelIndex) {
      return this.isValidSelection(levelIndex)
        ? this.set(false, levelIndex, this.levels[levelIndex].points)
        : this.reset(false);
    };

    RubricSection.prototype.isValid = function () {
      return this.isSelectionManual
        ? this.isValidManualScore(this.selectedPoints)
        : this.isValidSelection(this.selectionLevelIndex);
    };

    RubricSection.prototype.isValidManualScore = function (selectedPoints) {
      return (
        !isNaN(parseFloat(selectedPoints)) && selectedPoints >= 0 && selectedPoints <= this.points
      );
    };

    RubricSection.prototype.isValidSelection = function (index) {
      return !!this.levels[index];
    };

    return RubricSection;
  })
  .service('ViewRubric', function () {
    /*
    @TODO
    temp solution until we get the grade to completely turn over to use new api
    when that happens we can probably just make the rubric use current names from the server
    */
    const ViewRubric = function (rubricData, criterionResponses) {
      const criterion = rubricData.criteria || rubricData.sections;
      this.sections = map(criterion, (criteria, index) => {
        const criteriaResponse = criterionResponses ? criterionResponses[index] : {};
        return {
          ...criteria,
          title: criteria.title || criteria.name,
          index,
          levels: map(criteria.levels, (level, index) => ({ ...level, index })),
          isSelectionManual: criteriaResponse.type === 'manual',
          selectionLevelIndex: criteriaResponse.index,
          selectedPoints: criteriaResponse.points,
          feedback: criteriaResponse.criterionFeedback,
        };
      });
    };

    return ViewRubric;
  });
