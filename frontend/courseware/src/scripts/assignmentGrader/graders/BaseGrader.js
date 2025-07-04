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

import { get, set, isFunction } from 'lodash';

import NavBlockerService from '../../services/NavBlockerService.js';
import Roles from '../../utilities/Roles.js';

export default angular
  .module('lo.assignmentGrader.BaseGrader', [NavBlockerService.name, Roles.name])
  .factory('BaseGrader', [
    '$q',
    'Roles',
    'NavBlockerService',
    function ($q, Roles, NavBlockerService) {
      /**
       * @ngdoc object
       * @memberof lo.assessmentGrader
       * @description
       *   Container for all users who have submitted anything
       */
      class BaseGrader {
        constructor(assignmentId, contentItemId) {
          this.assignmentId = assignmentId;
          this.contentItemId = contentItemId;

          this.state = {};

          this.status = {};

          this.activeUser = null;

          this.activeGrade = null;

          this.nextItemToGrade = null;
          this.prevItemToGrade = null;

          this.unpostedCount = null;

          this.isUserInstructor = Roles.isStrictlyInstructor();

          this.detailedGradeExists = true;
        }

        getSubstate(path) {
          return get(this.state, path) || (set(this.state, path, {}) && get(this.state, path));
        }

        resolveData(substate, loader, reload) {
          if (substate.data && !reload) {
            return $q.when(substate.data);
          }

          if (!substate.promise || reload) {
            substate.error = null;
            substate.data = null;
            substate.promise = loader()
              .then(data => (substate.data = data), substate.data)
              .catch(error => (substate.error = error), $q.reject(substate.error))
              .finally(() => (substate.promise = null));
          }

          return substate.promise;
        }

        hasUnsavedChanges() {
          return this.activeGrade && this.activeGrade.isDirty();
        }

        blockNavForUnsavedChanges() {
          let navBlockCondition = this.hasUnsavedChanges.bind(this);
          this.removeNavBlocker = NavBlockerService.register(
            navBlockCondition,
            'GRADER_CONFIRM_MOVE_UNSAVED_CHANGES'
          );
        }

        confirmDiscardChanges() {
          if (this.hasUnsavedChanges()) {
            return NavBlockerService.confirmNavByModal(['GRADER_CONFIRM_MOVE_UNSAVED_CHANGES']);
          } else {
            return $q.when();
          }
        }

        saveChanges(isReleasing) {
          return this.activeGrade.saveChanges(isReleasing);
        }

        changeByInfo(info) {
          if (isFunction(info.changeToThis)) {
            return info.changeToThis();
          } else {
            return $q.reject();
          }
        }

        findNext(dir) {
          console.error('You must implement findNext for direction', dir);
          return $q.reject('You must implement findNext');
        }

        calcItemsToGrade() {
          this.findNext(-1).then(prev => {
            this.prevItemToGrade = prev && {
              ...prev,
              text: 'GRADER_CONTROL_PREV_' + prev.key,
            };
          });
          this.findNext(1).then(next => {
            this.nextItemToGrade = next && {
              ...next,
              text: 'GRADER_CONTROL_NEXT_' + next.key,
            };
          });
        }
      }

      return BaseGrader;
    },
  ]);
