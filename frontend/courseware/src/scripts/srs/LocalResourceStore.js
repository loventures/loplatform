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
  isFunction,
  map,
  filter,
  every,
  some,
  get,
  orderBy,
  includes,
  identity,
  each,
  reverse,
} from 'lodash';

import ResourceStore from './ResourceStore.js';
import { DEFAULT_PAGE_SIZE } from '../components/PaginateWithMax.js';
import Reverse from '../utilities/Reverse.js';
import UrlBuilder from '../utilities/UrlBuilder.js';

export default angular
  .module('lo.srs.LocalResourceStore', [Reverse.name, ResourceStore.name])
  .factory('LocalResourceStore', [
    '$q',
    'ResourceStore',
    'Settings',
    function ($q, ResourceStore, Settings) {
      /**
       * @ngdoc type
       * @memberof lo.srs
       * @description
       *      A store that we use for lists of data that may be
       *      too large for the UI but for one reason or another
       *      cannot be paginated/sorted/filtered using API
       */

      var LocalResourceStore = function (resolver) {
        //temporary backward compatibility
        if (!isFunction(resolver)) {
          const data = resolver;
          resolver = () => data;
        }

        this.resolver = resolver;

        this.query = {
          filters: {},
        };

        //this.data is the filtered and paged data,
        //named data for consistency with normal stores
        this.data = [];

        this.clearAllOrders();

        this.pageSize = DEFAULT_PAGE_SIZE;
      };

      LocalResourceStore.urlResolver = function (url, params, query) {
        return function lrsResolver() {
          return Request.promiseRequest(new UrlBuilder(url, params, query), 'get');
        };
      };

      LocalResourceStore.prototype.enableSaveSettings = function (key) {
        this.lsKey = key;
        const saved = Settings.getCourseLocalValue(this.lsKey);

        if (saved && saved.orders) {
          //reverse order to maintain priority
          for (let i = saved.orders.length - 1; i >= 0; i--) {
            let config = saved.orders[i];
            this.updateSortInfo(config);
          }
        }
      };

      LocalResourceStore.prototype.saveSettings = function () {
        if (!this.lsKey) {
          return;
        }

        const config = {};

        //this will ignore any custom sort function
        //until we do something like make sort functions injectable
        config.orders = map(this.orderPriority, orderProp => {
          return {
            property: orderProp,
            order: this.orderDirections[orderProp],
          };
        });

        Settings.setCourseLocalValue(this.lsKey, config);
      };

      LocalResourceStore.prototype.sortByProps = {};
      LocalResourceStore.prototype.searchByProps = {};

      LocalResourceStore.prototype.doRemoteLoad = function () {
        return $q.when(this.resolver());
      };

      LocalResourceStore.prototype.remoteLoad = function () {
        if (!this.resolverPromise) {
          this.resolverPromise = this.doRemoteLoad();
        }
        return this.resolverPromise;
      };

      //These methods are copy/similar to ResourceStore
      //the only reason we are not extending ResourceStore here
      //is that we don't have a local version of UrlQuery
      LocalResourceStore.prototype.load = ResourceStore.prototype.load;

      LocalResourceStore.prototype.beforeLoad = function () {
        return $q.when();
      };

      LocalResourceStore.prototype.doLoad = function () {
        return this.remoteLoad().then(data => {
          return this.applyQuery(data);
        });
      };

      LocalResourceStore.prototype.postLoad = ResourceStore.prototype.postLoad;

      LocalResourceStore.prototype.loadError = ResourceStore.prototype.loadError;

      LocalResourceStore.prototype.deserialize = identity;

      LocalResourceStore.prototype.setPageSize = function (size) {
        this.pageSize = size;
        this.gotoPage(1);
      };

      LocalResourceStore.prototype.applyQuery = function (allData) {
        const filteredData = filter(allData, item => {
          return every(this.query.filters, filter => {
            return some(filter.props, prop => {
              return filter.op(get(item, prop), filter.value);
            });
          });
        });

        const iteratees = this.getSortIterateesByPriority();
        const directions = this.getSortDirectionsByPriority();
        const orderedData = orderBy(filteredData, iteratees, directions);

        const offset = (this.query.page - 1) * this.pageSize;
        const results = orderedData.slice(offset, offset + this.pageSize);

        results.count = results.length;
        results.filterCount = filteredData.length;
        results.totalCount = allData.length;

        return results;
      };

      LocalResourceStore.prototype.gotoPage = function (page) {
        this.query.page = page;
        return this.load().then(data => {
          this.currentPage = this.query.page;
          this.saveSettings();
          return data;
        });
      };

      LocalResourceStore.prototype.search = function (str, props) {
        if (str) {
          this.query.filters.search = {
            op: function (value, search) {
              value = String(value).toLowerCase();
              search = String(search).toLowerCase();
              return includes(value, search);
            },
            props: props,
            value: str,
          };
        } else {
          delete this.query.search;
          delete this.query.filters.search;
        }

        return this.gotoPage(1);
      };

      LocalResourceStore.prototype.sort = function (startNew, clearAfter, ...configs) {
        if (startNew || this.clearOnNextOrder) {
          this.clearAllOrders();
          this.clearOnNextOrder = false;
        }
        // reverse order to maintain priority
        for (let i = configs.length - 1; i >= 0; i--) {
          this.updateSortInfo(configs[i]);
        }

        if (clearAfter) {
          this.clearOnNextOrder = true;
        }

        return this.gotoPage(1);
      };

      /**
       * Reverses the order of the provided properties or the most recent property if none
       * @param props {object} An array of properties to reverse the order of.
       */
      LocalResourceStore.prototype.switchDirection = function (props) {
        props = props || [this.orderPriority[0]];
        each(props, prop => {
          if (this.orderDirections[prop] != null) {
            this.orderDirections[prop] *= -1;
          }
        });
      };

      LocalResourceStore.prototype.clearAllOrders = function () {
        this.orderPriority = [];
        this.orderIteratees = {};
        this.orderDirections = {};
      };

      LocalResourceStore.prototype.clearOrder = function (prop) {
        var index = this.orderPriority.indexOf(prop);
        if (index !== -1) {
          this.orderPriority.splice(index, 1);
        }
      };

      LocalResourceStore.prototype.updateSortInfo = function (config) {
        let property = config.property,
          order = config.order,
          iteratee = config.iteratee;

        if (this.orderPriority[0] !== property) {
          this.clearOrder(property);
          this.orderPriority.unshift(property);
        }

        this.orderDirections[property] = order;

        this.orderIteratees[property] = function (data) {
          var val = iteratee ? iteratee.apply(null, arguments) : get(data, property);

          if (val == null) {
            return -Infinity;
          } else if (typeof val === 'string') {
            return val.toLowerCase();
          } else {
            return val;
          }
        };
      };

      LocalResourceStore.prototype.getSortDirectionsByPriority = function () {
        return reverse(
          map(this.orderPriority, order => {
            //TODO we should make all orders standardize to asc/desc
            if (this.orderDirections[order] === 1) {
              return 'asc';
            } else if (this.orderDirections[order] === -1) {
              return 'desc';
            } else {
              return this.orderDirections[order];
            }
          })
        );
      };

      LocalResourceStore.prototype.getSortIterateesByPriority = function () {
        return reverse(
          map(this.orderPriority, order => {
            return this.orderIteratees[order];
          })
        );
      };

      return LocalResourceStore;
    },
  ]);
