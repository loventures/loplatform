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

import { values } from 'lodash';

export default angular
  .module('lo.discussion.DiscussionScrollService', [])
  .constant('DiscussionScrollFlashClasses', {
    unread: 'flash-unread',
    new: 'flash-unread',
    bookmarked: 'flash-bookmarked',
    unresponded: 'flash-unresponded',
    'user-posts': 'flash-user-posts',
    search: 'flash-search',
    'reported-inappropriate-posts': 'flash-reported-inappropriate-posts',
  })
  .service('DiscussionScrollService', [
    'DiscussionScrollFlashClasses',
    '$q',
    '$document',
    '$location',
    '$timeout',
    function (DiscussionScrollFlashClasses, $q, $document, $location, $timeout) {
      var service = {
        allClasses: values(DiscussionScrollFlashClasses).join(' '),
      };

      service.scrollToAndFlash = function (postId, { flashType }) {
        var hash = 'discussion-item-' + postId;
        return service
          .waitForRendering(hash)
          .then(function (element) {
            return service.scrollTo(element, hash);
          })
          .then(function (element) {
            return service.flash(element, flashType);
          });
      };

      service.flash = function (element, type) {
        element.removeClass('flash ' + service.allClasses);
        var classes = DiscussionScrollFlashClasses[type] || '';
        return $timeout(function () {
          element.addClass('flash ' + classes);
        }, 500);
      };

      service.scrollTo = function (element, hash) {
        $location.hash(hash);
        return $document.duScrollToElementAnimated(element, 100).then(function () {
          return element;
        });
      };

      service.waitForRendering = function (hash) {
        var defer = $q.defer();

        service.continueRenderedCheck('.discussion-single-thread-view #' + hash, defer);

        return defer.promise;
      };

      service.continueRenderedCheck = function (selector, defer) {
        //need to re-fetch to check whether it is rendered
        var element = angular.element(selector);

        if (element[0]) {
          defer.resolve(element);
        } else {
          $timeout(function () {
            service.continueRenderedCheck(selector, defer);
          }, 100);
        }
      };

      return service;
    },
  ]);
