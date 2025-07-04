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

import { isEmpty, map, each, defaults, extend, isArray, isObject, values } from 'lodash';

import { DEFAULT_PAGE_SIZE } from '../components/PaginateWithMax.js';
import Reverse from '../utilities/Reverse.js';
import UrlQuery from '../utilities/UrlQuery.js';
import UrlBuilder from '../utilities/UrlBuilder.js';

export default angular.module('lo.srs.ResourceStore', [Reverse.name]).factory('ResourceStore', [
  '$q',
  'Request',
  'Settings',
  'errorMessageFilter',
  function ($q, Request, Settings, errorMessageFilter) {
    /**
     * @ngdoc type
     * @memberof lo.srs
     * @description
     *     A ResourceStore is a storage that connects to a resource API.
     *     The resource API is possibly paginated, filterable and sortable
     * @param {string} url
     *     The parameterized url to be used as the base url for this store.
     * @param {object} params
     *     A hash of fixed params that identifies a specific collection of items.
     * @param {string} idParam
     *     The name of the param in the parameterized url
     *     that identifies individual items in this collection
     * @returns {ResourceStore}
     *     ResourceStore object
     */
    var ResourceStore = function (url, params, idParam) {
      this.url = url;
      this.params = params;
      this.idParam = idParam;
      this.queryParams = {};

      this.filters = new UrlQuery({
        offset: 0,
        limit: DEFAULT_PAGE_SIZE,
      });

      this.pageSize = DEFAULT_PAGE_SIZE;

      this.data = [];
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Enables saving settings when user make a change
     *      Also loads settings if previously saved
     *      Currently saves to localStorage
     *      Currently only supports order
     * @param {object} key
     *     key for a particular usage of this store
     */
    ResourceStore.prototype.enableSaveSettings = function (key) {
      this.lsKey = key;

      const saved = Settings.getCourseLocalValue(this.lsKey);

      if (isEmpty(saved)) {
        return;
      }

      this.filters.setOrders(saved.orders);
    };

    ResourceStore.prototype.saveSettings = function () {
      if (!this.lsKey) {
        return;
      }

      const config = {};

      config.orders = map(this.filters.orderPriority, orderProp => {
        return {
          property: orderProp,
          order: this.filters.orderValues[orderProp],
        };
      });

      Settings.setCourseLocalValue(this.lsKey, config);
    };

    /**
     * @memberof ResourceStore
     * @description
     *     Does a load using the current filter options
     * @returns {Promise}
     *     Resolves the list of items loaded
     */
    ResourceStore.prototype.load = function () {
      this.loading = true;
      this.loadErrorMessage = null;

      return this.beforeLoad()
        .then(this.doLoad.bind(this), this.loadError.bind(this))
        .then(this.postLoad.bind(this), this.loadError.bind(this));
    };

    ResourceStore.prototype.beforeLoad = function () {
      return $q.when();
    };

    //the method to actually make the calls
    ResourceStore.prototype.doLoad = function () {
      var url = new UrlBuilder(this.url, this.params, this.filters);

      return Request.promiseRequest(url, 'get', this.queryParams);
    };

    ResourceStore.prototype.loadError = function (err) {
      this.loadErrorMessage = errorMessageFilter(err);
      this.loading = false;
      return $q.reject(err);
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Method that processes the data after a load.
     *      Adds all the items to the data store,
     *      and sets several item counts used in pagination and etc.
     * @returns {Promise}
     *      Resolves the list of items loaded
     */
    ResourceStore.prototype.postLoad = function (data) {
      this.loading = false;
      this.data.length = 0;
      each(
        data,
        function (d) {
          //this still retains the name that the constructor was created with
          this.data.push(this.deserialize(d));
        }.bind(this)
      );

      this.count = data.count; //actual number of items loaded in this call
      this.totalCount = data.totalCount; //total number of items for this resource, unfiltered
      this.filterCount = data.filterCount; //total number of items satisfying to current filter

      return this.data;
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Serialize an item that might be modified on the client
     *      into the form accepted by the server.
     * @param {object} item
     *     The item in the client
     * @returns {object}
     *     A json to be sent to server
     */
    ResourceStore.prototype.serialize = function (item) {
      return item;
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Deserialize data from the server into one usable by client
     *      If an old instance is passed in, combine client data
     *      from the old instance into the new data
     * @param {object} data
     *     The data from the sever
     * @param {object} oldData
     *     The old instance of the item
     * @returns {object}
     *     An object that can be used on the client
     */
    ResourceStore.prototype.deserialize = function (data, oldData) {
      return defaults({}, data, oldData);
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Helper method to augment the url of the collection
     *      with the id of a specific item
     * @param {number | string} id
     *      The data from the sever
     * @param {string} idParam
     *      The name of the param of the id.
     *      Defaults to a store wide name set when the store is created
     * @returns {object}
     *      The complete set of params that identifies an item in the collection.
     */
    ResourceStore.prototype.paramWithId = function (id, idParam) {
      var p = {};
      extend(p, this.params);
      p[idParam || this.idParam] = id;
      return p;
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Create an item on the server
     * @param {object} item
     *      The item to create
     * @returns {object}
     *      The created item, returned by the server and combined with the item on client.
     */
    ResourceStore.prototype.create = function (item) {
      var url = new UrlBuilder(this.url, this.params);
      return Request.promiseRequest(url, 'post', this.serialize(item)).then(
        function (newItem) {
          return this.deserialize(newItem, item);
        }.bind(this)
      );
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Create an item on the server
     * @param {number | string} id
     *      The id of the item we want to get
     * @param {string} idParam
     *      The name of the param of the id.
     *      Defaults to a store wide name set when the store is created
     * @returns {object}
     *      The item, returned by the server.
     */
    ResourceStore.prototype.get = function (id, idParam) {
      var url = new UrlBuilder(this.url, this.paramWithId(id, idParam));
      console.log('get 1', url.toString());
      return Request.promiseRequest(url, 'get').then(this.deserialize.bind(this));
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Updates the item
     * @param {number | string} id
     *      The id of the item we want to update
     * @param {string} idParam
     *      The name of the param of the id.
     *      Defaults to a store wide name set when the store is created
     * @returns {object}
     *      The item, returned by the server and combined with the item on client.
     */
    ResourceStore.prototype.update = function (item, idParam) {
      var url = new UrlBuilder(this.url, this.paramWithId(item.id, idParam));
      return Request.promiseRequest(url, 'put', this.serialize(item)).then(
        function (newItem) {
          return this.deserialize(newItem, item);
        }.bind(this)
      );
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Remove the item
     * @param {number | string} id
     *      The id of the item we want to remove
     * @param {string} idParam
     *      The name of the param of the id.
     *      Defaults to a store wide name set when the store is created
     * @returns {object}
     *      The status of the deletion.
     */
    ResourceStore.prototype.remove = function (item, idParam) {
      var url = new UrlBuilder(this.url, this.paramWithId(item.id, idParam));
      return Request.promiseRequest(url, 'delete');
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Set the number of items to load for each paginated load
     * @param {number} pageSize
     *      The size limit
     * @returns {object}
     *      The complete set of params that identifies an item in the collection.
     */
    ResourceStore.prototype.setPageSize = function (pageSize) {
      this.pageSize = pageSize;
      this.filters.setLimit(pageSize);
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Load items at the specified page
     * @param {number} page
     *      The page to load
     * @returns {promise}
     *      Resolves a load promise
     */
    ResourceStore.prototype.gotoPage = function (page) {
      this.filters.gotoPage(page - 1);
      return this.load().then(
        function (data) {
          this.currentPage = page;
          this.saveSettings();
          return data;
        }.bind(this)
      );
    };

    /**
     * @memberof ResourceStore
     * Sorts by a property name and direction
     * @param configs {Object} Array of config objects { property, order} to apply ordering to
     * @param startNew {boolean} true to clear other sort order before using this one
     * @param clearAfter {boolean} true to clear sort order before the next sort
     * @returns {promise|UrlQuery|*}
     */
    ResourceStore.prototype.sort = function (startNew, clearAfter, ...configs) {
      if (startNew || this.clearOnNextOrder) {
        this.filters.clearOrder();
        this.clearOnNextOrder = false;
      }

      this.filters.sort(configs);

      if (clearAfter) {
        this.clearOnNextOrder = true;
      }

      return this.gotoPage(1);
    };

    /**
     * @memberof ResourceStore
     * Reverses the ordering direction of the provided property, or all properties if none is specified
     * @param props {string|Array} property or array of properties to switch sorting direction on.
     * @returns {promise|UrlQuery|*}
     */
    ResourceStore.prototype.switchDirection = function (props, startNew, clearAfter) {
      this.filters.switchDirection(props, startNew, clearAfter);
      return this.gotoPage(1);
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Update the search filter based on the searchString
     *      By default it should clear the previous search for the props
     *      And do an exact match for one of the props
     * @param {string} prop
     *      The proeprty to sort by
     * @returns {promise}
     *      Resolves a load promise
     */
    ResourceStore.prototype.updateSearchFilters = function (searchString) {
      this.filters.setFilterOp('or');

      this.filters.setFilters(
        map(this._currentSearchProps, function (prop) {
          return [prop, 'contains', searchString];
        })
      );

      return this;
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Clear search filters from last search
     * @param {string} prop
     *      The proeprty to sort by
     * @returns {promise}
     *      Resolves a load promise
     */
    ResourceStore.prototype.clearFilters = function () {
      this.filters.removeFilters(
        map(this._currentSearchProps, function (prop) {
          return [prop]; //for consistent format
        })
      );
    };

    /**
     * @memberof ResourceStore
     * @description
     *      Set the filter to search for matches in properties,
     *      and reload to the first page.
     *      If searchString is blank, reset the search filter.
     * @param {string} searchString
     *      The string to search with
     * @param {string} props
     *      The proeprties to search from
     * @returns {promise}
     *      Resolves a load promise
     */
    ResourceStore.prototype.search = function (searchString, props, filterOp) {
      //backward compat
      if (typeof searchString !== 'string') {
        searchString = this.queryString;
      }
      this.queryString = searchString;

      this.clearFilters(searchString, props, filterOp);

      if (!searchString) {
        this.filters.setFilterOp(null);
        return this.gotoPage(1);
      }

      //also backward compat, should always be a hash
      if (isArray(props)) {
        this._currentSearchProps = props;
      } else if (props) {
        if (isObject(props)) {
          this._currentSearchProps = values(props);
        } else {
          this._currentSearchProps = [props];
        }
      }

      this.updateSearchFilters(searchString, props, filterOp);

      return this.gotoPage(1);
    };

    ResourceStore.prototype.addQueryParam = function (newParam) {
      if (this.url.indexOf('?') === -1) {
        this.url += '?' + newParam;
      } else {
        this.url += ',' + newParam;
      }
    };

    return ResourceStore;
  },
]);
