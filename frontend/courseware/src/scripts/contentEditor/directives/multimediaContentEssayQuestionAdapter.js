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

import { get, filter, map, forEach } from 'lodash';

import tmpl from './multimediaContentEssayQuestionAdapter.html';

import richTextEditor from './richTextEditor.jsx';
import feedback from '../../assignmentFeedback/index.js';

export default angular
  .module('lo.contentEditor.multimediaContentEssayQuestionAdapter', [
    richTextEditor.name,
    feedback.name,
  ])
  .directive('multimediaContentEssayQuestionAdapter', [
    '$q',
    'Request',
    'FeedbackManager',
    function ($q, Request, FeedbackManager) {
      return {
        restrict: 'A',
        replace: true,
        scope: {
          questionScope: '=',
        },
        template: tmpl,
        link: function (scope, element) {
          scope.contentLoaded = false;

          scope.richText = {
            answerText: '',
          };

          scope.init = function () {
            //TODO the one on essay question linker looses reference
            scope.questionScope.question.selfControlled = true;

            if (scope.questionScope.question.answerText) {
              scope.richText.answerText = scope.questionScope.question.answerText;
            }

            scope.questionScope.submittedAnswerText = scope.richText.answerText;

            scope.showFileUpload = scope.questionScope.showFileUpload;

            if (!scope.showFileUpload) {
              scope.contentLoaded = true;
              return;
            }

            //only if file upload is enabled
            scope.feedbackManager = new FeedbackManager(scope.questionScope.files);

            scope.questionScope.answer.selfControlled = true;

            scope.questionScope.answer.hasStagedFiles = function () {
              return scope.feedbackManager.hasStagedOrOngoing();
            };

            scope.questionScope.answer.beforeAnswering = function (done) {
              return scope.feedbackManager.confirmResetByModal().then(function () {
                var files = filter(scope.feedbackManager.files, function (file) {
                  return get(file, 'info.guid');
                });
                var uploadInfo = map(files, scope.parseFile);
                return $q
                  .all([
                    scope.feedbackManager.commitRemovingFiles(),
                    scope.uploadStagedFiles(uploadInfo).then(function () {
                      forEach(files, function (file) {
                        file.setMovedFromStaging();
                      });
                    }),
                  ])
                  .then(
                    function () {
                      scope.questionScope.submittedAnswerText =
                        scope.questionScope.question.answerText;
                      done();
                    },
                    function (error) {
                      //TODO have UI to show error
                      console.error(error);
                    }
                  );
              });
            };

            scope.contentLoaded = true;
          };

          //Since quiz player is using one controller with multiple linkers,
          //it is not possible to communicate through normal means using require
          //and since linkers are not run in any particular order
          //we need the watch to ensure that we actually have the scope ready
          scope.$watch('questionScope.loaded', function (loaded) {
            console.log('loaded', loaded);
            if (loaded) {
              scope.init();
            } else {
              scope.contentLoaded = false;
            }
          });

          scope.getContent = function () {
            return scope.richText.answerText;
          };

          scope.richContentChanged = function (content) {
            if (scope.contentChanged()) {
              scope.questionScope.changed();
            }
            scope.richText.answerText = content;
            scope.questionScope.question.answerText = scope.richText.answerText;
          };

          scope.uploadStagedFiles = function (files) {
            return Request.promiseRequest(scope.questionScope.batchUploadUrl, 'post', {
              uploads: files,
            });
          };

          scope.parseFile = function (file) {
            return {
              guid: file.info.guid,
            };
          };

          scope.contentChanged = function () {
            if (scope.questionScope.page.feedback) {
              return false;
            }

            if (scope.feedbackManager.hasStagedOrOngoing()) {
              return scope.feedbackManager.isReady();
            }

            return scope.questionScope.submittedAnswerText !== scope.richText.answerText;
          };

          element.on('keypress', function (event) {
            event.stopPropagation();
          });

          scope.$emit('MultimediaInputInstanceCreated', scope);
          scope.$on('$destroy', function () {
            scope.$emit('MultimediaInputInstanceDestroyed', scope);
          });
        },
      };
    },
  ]);
