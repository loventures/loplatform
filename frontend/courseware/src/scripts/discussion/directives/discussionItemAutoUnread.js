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

// import $ from 'jquery';
import { keys, filter, flow, mapValues, map, groupBy } from 'lodash';

import angularInview from 'angular-inview';

import discussionItemAutoUnread from './discussionItemAutoUnread.html';

import actions from '../actions/index.js';

import CallAggregator from '../../utilities/CallAggregator.js';

export default angular
  .module('lo.discussion.discussionAutoUnread', [angularInview, CallAggregator.name, actions.name])
  .service('DiscussionAutoUnreadService', [
    'DiscussionPostActions',
    'CallAggregatorsSet',
    'Settings',
    '$ngRedux',
    function (DiscussionPostActions, CallAggregatorsSet, Settings, $ngRedux) {
      var service = {};

      const isInstructor = Settings.isFeatureEnabled('TeachCourseRight');

      service._updateTracking = function (discussionId, postIds, threadIdMap) {
        if (isInstructor) {
          $ngRedux.dispatch(
            DiscussionPostActions.createAutoSetViewedAction(discussionId, postIds, threadIdMap)
          );
        } else {
          $ngRedux.dispatch(
            DiscussionPostActions.createAutoSetNotNewAction(discussionId, postIds, threadIdMap)
          );
        }
      };

      service.aggregators = new CallAggregatorsSet(function (discussionId) {
        return function (idMap) {
          const threadIdMap = flow(
            values => filter(values, (tid, pid) => tid !== +pid),
            values => groupBy(values, threadId => threadId),
            values => mapValues(values, 'length')
          )(idMap);
          service._updateTracking(
            discussionId,
            map(keys(idMap), a => +a),
            threadIdMap
          );
        };
      }, 1000);

      service.updateViewed = function (discussionId, threadId, postId) {
        var aggregator = service.aggregators.getOrCreate(discussionId);
        return aggregator.queueCalls({
          [postId]: threadId,
        });
      };

      return service;
    },
  ])
  .directive('discussionAutoUnread', function () {
    var inviewThreshold = 2000;
    return {
      restrict: 'E',
      template: discussionItemAutoUnread,
      scope: {
        postId: '=',
        threadId: '=',
        discussionId: '=',
        item: '=',
      },
      controller: [
        '$scope',
        '$timeout',
        '$window',
        'DiscussionAutoUnreadService',
        'Settings',
        function ($scope, $timeout, $window, DiscussionAutoUnreadService, Settings) {
          const isInstructor = Settings.isFeatureEnabled('TeachCourseRight');

          $scope.queuedForViewing = false;

          // If we always do half the screen height, some posts can never be automarked on large screens.
          const height = $($window).innerHeight();
          $scope.screenCenterOffset = Math.min(height * 0.5, 100);

          $scope.updateViewStatus = function (inview) {
            if (inview && !$scope.viewingTimeout) {
              //just came into view
              $scope.viewingTimeout = $timeout(function () {
                //stayed enough before getting cancelled
                $scope.viewingTimeout = null;
                $scope.viewingTimeoutCompleted = true;
                $scope.autoUpdateViewed();
              }, inviewThreshold);
            } else if (!inview && $scope.viewingTimeout) {
              //left view too early
              $timeout.cancel($scope.viewingTimeout);
              $scope.viewingTimeout = null;
            }
          };

          $scope.updateMidpointStatus = function (inview) {
            $scope.midPointReached = $scope.midPointReached || inview;
            $scope.autoUpdateViewed();
          };

          $scope.autoUpdateViewed = function () {
            if (!$scope.midPointReached || !$scope.viewingTimeoutCompleted) {
              return;
            }

            //since we defer and batch the calls to update view, don't want to fire it off again
            if ($scope.queuedForUpdate) {
              return;
            }

            const shouldUpdate = isInstructor ? $scope.item.isUnread : $scope.item.isNew;
            if (shouldUpdate) {
              DiscussionAutoUnreadService.updateViewed(
                $scope.discussionId,
                $scope.threadId,
                $scope.postId
              );
              $scope.queuedForUpdate = true;
            }
          };

          $scope.$on('$destroy', () => {
            if ($scope.viewingTimeout) {
              $timeout.cancel($scope.viewingTimeout);
            }
          });
        },
      ],
    };
  });
