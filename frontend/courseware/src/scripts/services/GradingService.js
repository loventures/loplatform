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

import Course from '../bootstrap/course.js';
import { loConfig } from '../bootstrap/loConfig.js';
import dayjs from 'dayjs';
import { each, groupBy, last, mapValues, sortBy } from 'lodash';
import GradebookAPI from './GradebookAPI.js';
import Request from '../utilities/Request.js';
import UrlBuilder from '../utilities/UrlBuilder.js';

export default angular
  .module('lo.services.GradingService', [Request.name, GradebookAPI.name])
  .service('GradingService', [
    'Request',
    '$rootScope',
    /**
     * @ngdoc service
     * @alias GradingService
     * @memberof lo.assignmentGrader
     * @description
     *   API wrapper for grading related calls
     */
    function GradingService(Request, $rootScope) {
      /** @alias GradingService **/
      var GradingService = {};

      GradingService.updateDiscussionGrade = function (discussionId, studentId, grade) {
        var url;
        var method;
        if (grade.id) {
          url = new UrlBuilder(loConfig.discussion.rubric.updateGrade, {
            courseId: Course.id,
            discussionId: discussionId,
            studentId: studentId,
            scoreId: grade.id,
          });
          grade = angular.copy(grade);
          method = 'put';
        } else {
          url = new UrlBuilder(loConfig.discussion.rubric.setGrade, {
            courseId: Course.id,
            discussionId: discussionId,
            studentId: studentId,
          });
          method = 'post';
        }

        return Request.promiseRequest(url, method, grade);
      };

      GradingService.getDiscussionGrades = function (discussionId, studentIds) {
        var url = new UrlBuilder(loConfig.discussion.rubric.getGrades, {
          courseId: Course.id,
          discussionId: discussionId,
        });

        var gradeParentUrl = url.toString();

        url.query.setEmbeds(['attachments.integrations']);

        if (studentIds) {
          url.query.setFilter('user', 'in', studentIds);
        }

        return Request.promiseRequest(url, 'get').then(function (grades) {
          const contextMatrixParam = /(.*)(;context=\d+)(.*)$/;

          const gradesMap = mapValues(groupBy(grades, 'userId'), function (gradesForOne) {
            return last(
              sortBy(gradesForOne, function (grade) {
                return dayjs(grade.createDate);
              })
            );
          });

          each(gradesMap, function (grade) {
            var attachmentParentUrl = gradeParentUrl;
            if (grade.attachments) {
              each(grade.attachments, function (file) {
                if (contextMatrixParam.test(attachmentParentUrl)) {
                  const parentUrlParts = contextMatrixParam.exec(attachmentParentUrl);
                  const [attachmentUrlRoot, urlContext, additionalParams] = parentUrlParts.slice(1);
                  file.url = `${attachmentUrlRoot}/${grade.id}/attachments/${file.id}${urlContext}${additionalParams}`;
                  file.viewUrl = `${attachmentUrlRoot}/${grade.id}/attachments/${file.id}/view${urlContext}${additionalParams}`;
                } else {
                  file.url = attachmentParentUrl + '/' + file.id;
                  file.viewUrl = file.url + '/view';
                }
              });
            }
          });

          return gradesMap;
        });
      };

      //TODO: pretty sure this isn't a thing anymore...CBLPROD-9656?
      const emitAttemptUpdated = attemptId => {
        return data => {
          $rootScope.$emit('attemptGradeUpdated', attemptId);
          return data;
        };
      };

      GradingService.updateQuestionGrade = function (
        attemptId,
        responseId,
        grade,
        markAsSubmitted
      ) {
        return GradingService.grade(
          attemptId,
          {
            [responseId]: grade,
          },
          markAsSubmitted
        ).then(emitAttemptUpdated(attemptId));
      };

      return GradingService;
    },
  ]);
