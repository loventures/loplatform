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

import { isEmpty, mapValues, each } from 'lodash';

/**
 * @ngdoc service
 * @alias CallAggregator
 * @memberOf lo.utilities
 */
export default angular
  .module('lo.utilities.CallAggregator', [])
  .service('CallAggregatorsSet', [
    'CallAggregator',
    function (CallAggregator) {
      class CallAggregatorsSet {
        constructor(actionCreator, delay) {
          this.aggregators = {};
          this.delay = delay;
          this.actionCreator = actionCreator;
        }

        getOrCreate(key) {
          if (!this.aggregators[key]) {
            var callAction = this.actionCreator(key);
            this.aggregators[key] = new CallAggregator(callAction);
          }

          return this.aggregators[key];
        }
      }

      return CallAggregatorsSet;
    },
  ])
  .service('CallAggregator', [
    '$q',
    '$timeout',
    function ($q, $timeout) {
      /**
       * @alias CallAggregator
       * @description
       *      create a new CallAggregator
       * @param {function} callAction
       *      a function to invoke for aggregated arguments
       *
       *      this function should accept a single arguments
       *      that is a map that comes as the result of
       *      putting together the argumentMap of all calls to queueCalls
       *      since the last time this function was triggered
       *
       *      this function should return a promise that resolves
       *      a map of key:value pairs where the key is the same key
       *      as in the argumentMap passed into queueCalls
       *      and the value is what this function thinks is
       *      corresponding to the key.
       * @returns {CallAggregator}
       *      CallAggregator object
       */
      var CallAggregator = class CallAggregator {
        constructor(callAction, delay) {
          this.waiting = {};
          this.ongoing = null;

          this.callAction = callAction;

          this.delay = delay || 500;
        }

        _promiseCall(argsMap) {
          const promise = this.callAction(argsMap);

          return promise && promise.then ? promise : $q.when(argsMap);
        }

        _doCall() {
          if (isEmpty(this.waiting)) {
            return;
          }

          this.ongoing = this.waiting;
          this.waiting = {};

          var argsMap = mapValues(this.ongoing, info => {
            return info.args;
          });

          this._promiseCall(argsMap).then(resultsMap => {
            each(this.ongoing, (result, key) => {
              if (resultsMap[key]) {
                this.ongoing[key].defer.resolve(resultsMap[key]);
              } else {
                this.ongoing[key].defer.resolve(null);
              }
            });

            this.ongoing = null;

            this._tryCall();
          });
        }

        _tryCall() {
          $timeout.cancel(this.tryCallTimeout);

          this.tryCallTimeout = $timeout(() => {
            if (this.ongoing === null) {
              this._doCall();
            }
          }, this.delay);
        }

        /**
         * @description
         *      add some key and argument to the aggregator
         * @param {object} argumentMap
         *      a map of key:value where
         *      the key will be the key to the result once resolved
         *      and the value would be additional info that
         *      the callAction function (passed via constructor)
         *      will use to take action
         * @returns {Promise}
         *      resolves a map of key:value,
         *      with keys the same as in argumentMap
         *      and value being what callAction think
         *      is corresponding to the key
         */
        queueCalls(argumentMap) {
          var promises = mapValues(argumentMap, (args, key) => {
            this.waiting[key] = this.waiting[key] || {
              defer: $q.defer(),
              args: args,
            };

            this._tryCall();

            return this.waiting[key].defer.promise;
          });

          return $q.all(promises);
        }
      };

      return CallAggregator;
    },
  ]);
