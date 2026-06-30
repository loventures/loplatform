/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { DEFAULT_PAGE_SIZE } from '../../components/PaginateWithMax.js';
import UrlQuery from '../../utilities/UrlQuery.js';
import UrlBuilder from '../../utilities/UrlBuilder.ts';
import { request } from '../../utilities/request.ts';
import settings from '../../utilities/settingsService.ts';
import { errorMessage } from '../../filters/pure/errorMessage.ts';
import { instant } from '../../i18n/pure/i18n.ts';

/**
 * A ResourceStore is a storage that connects to a resource API.
 * The resource API is possibly paginated, filterable and sortable.
 *
 * Ported verbatim from the AngularJS `lo.srs.ResourceStore` factory. Kept as a
 * **constructor function** (not an ES6 `class`) so the legacy subclass
 * `UserListStore` can still invoke `ResourceStore.call(this, …)` /
 * `ResourceStore.prototype.load.apply(this)` — an ES6 class throws when called
 * without `new`.
 *
 * $q → native Promise: consumers re-render via the React `useSrsStore` hook, not
 * an Angular digest, so no digest is required.
 *
 * @param url     The parameterized url to be used as the base url for this store.
 * @param params  A hash of fixed params that identifies a specific collection of items.
 * @param idParam The name of the param in the parameterized url that identifies
 *                individual items in this collection.
 */
export function ResourceStore(this: any, url?: any, params?: any, idParam?: any) {
  this.url = url;
  this.params = params;
  this.idParam = idParam;
  this.queryParams = {};

  this.filters = new (UrlQuery as any)({
    offset: 0,
    limit: DEFAULT_PAGE_SIZE,
  });

  this.pageSize = DEFAULT_PAGE_SIZE;

  this.data = [];
}

/**
 * Enables saving settings when user makes a change. Also loads settings if
 * previously saved. Currently saves to localStorage and only supports order.
 * @param key key for a particular usage of this store
 */
ResourceStore.prototype.enableSaveSettings = function (key: any) {
  this.lsKey = key;

  const saved = settings.getCourseLocalValue(this.lsKey);

  if (isEmpty(saved)) {
    return;
  }

  this.filters.setOrders(saved.orders);
};

ResourceStore.prototype.saveSettings = function () {
  if (!this.lsKey) {
    return;
  }

  const config: any = {};

  config.orders = map(this.filters.orderPriority, (orderProp: any) => {
    return {
      property: orderProp,
      order: this.filters.orderValues[orderProp],
    };
  });

  settings.setCourseLocalValue(this.lsKey, config);
};

/**
 * Does a load using the current filter options.
 * @returns Resolves the list of items loaded.
 */
ResourceStore.prototype.load = function () {
  this.loading = true;
  this.loadErrorMessage = null;

  return this.beforeLoad()
    .then(this.doLoad.bind(this), this.loadError.bind(this))
    .then(this.postLoad.bind(this), this.loadError.bind(this));
};

ResourceStore.prototype.beforeLoad = function () {
  return Promise.resolve();
};

//the method to actually make the calls
ResourceStore.prototype.doLoad = function () {
  var url = new (UrlBuilder as any)(this.url, this.params, this.filters);

  return request.promiseRequest(url, 'get', this.queryParams);
};

ResourceStore.prototype.loadError = function (err: any) {
  this.loadErrorMessage = errorMessage(err, (key: string) => instant(key));
  this.loading = false;
  return Promise.reject(err);
};

/**
 * Processes the data after a load. Adds all the items to the data store, and
 * sets several item counts used in pagination and etc.
 * @returns Resolves the list of items loaded.
 */
ResourceStore.prototype.postLoad = function (data: any) {
  this.loading = false;
  this.data.length = 0;
  each(
    data,
    function (this: any, d: any) {
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
 * Serialize an item that might be modified on the client into the form
 * accepted by the server.
 */
ResourceStore.prototype.serialize = function (item: any) {
  return item;
};

/**
 * Deserialize data from the server into one usable by client. If an old
 * instance is passed in, combine client data from the old instance into the
 * new data.
 */
ResourceStore.prototype.deserialize = function (data: any, oldData?: any) {
  return defaults({}, data, oldData);
};

/**
 * Helper method to augment the url of the collection with the id of a specific
 * item.
 */
ResourceStore.prototype.paramWithId = function (id: any, idParam?: any) {
  var p: any = {};
  extend(p, this.params);
  p[idParam || this.idParam] = id;
  return p;
};

/**
 * Create an item on the server.
 */
ResourceStore.prototype.create = function (item: any) {
  var url = new (UrlBuilder as any)(this.url, this.params);
  return request.promiseRequest(url, 'post', this.serialize(item)).then(
    function (this: any, newItem: any) {
      return this.deserialize(newItem, item);
    }.bind(this)
  );
};

/**
 * Get an item from the server by id.
 */
ResourceStore.prototype.get = function (id: any, idParam?: any) {
  var url = new (UrlBuilder as any)(this.url, this.paramWithId(id, idParam));
  console.log('get 1', url.toString());
  return request.promiseRequest(url, 'get').then(this.deserialize.bind(this));
};

/**
 * Updates the item.
 */
ResourceStore.prototype.update = function (item: any, idParam?: any) {
  var url = new (UrlBuilder as any)(this.url, this.paramWithId(item.id, idParam));
  return request.promiseRequest(url, 'put', this.serialize(item)).then(
    function (this: any, newItem: any) {
      return this.deserialize(newItem, item);
    }.bind(this)
  );
};

/**
 * Remove the item.
 */
ResourceStore.prototype.remove = function (item: any, idParam?: any) {
  var url = new (UrlBuilder as any)(this.url, this.paramWithId(item.id, idParam));
  return request.promiseRequest(url, 'delete');
};

/**
 * Set the number of items to load for each paginated load.
 */
ResourceStore.prototype.setPageSize = function (pageSize: any) {
  this.pageSize = pageSize;
  this.filters.setLimit(pageSize);
};

/**
 * Load items at the specified page.
 */
ResourceStore.prototype.gotoPage = function (page: any) {
  this.filters.gotoPage(page - 1);
  return this.load().then(
    function (this: any, data: any) {
      this.currentPage = page;
      this.saveSettings();
      return data;
    }.bind(this)
  );
};

/**
 * Sorts by a property name and direction.
 * @param startNew true to clear other sort order before using this one
 * @param clearAfter true to clear sort order before the next sort
 * @param configs Array of config objects { property, order } to apply ordering to
 */
ResourceStore.prototype.sort = function (startNew: any, clearAfter: any, ...configs: any[]) {
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
 * Reverses the ordering direction of the provided property, or all properties
 * if none is specified.
 */
ResourceStore.prototype.switchDirection = function (props: any, startNew?: any, clearAfter?: any) {
  this.filters.switchDirection(props, startNew, clearAfter);
  return this.gotoPage(1);
};

/**
 * Update the search filter based on the searchString. By default it should
 * clear the previous search for the props and do an exact match for one of the
 * props.
 */
ResourceStore.prototype.updateSearchFilters = function (searchString: any) {
  this.filters.setFilterOp('or');

  this.filters.setFilters(
    map(this._currentSearchProps, function (prop: any) {
      return [prop, 'contains', searchString];
    })
  );

  return this;
};

/**
 * Clear search filters from last search.
 */
ResourceStore.prototype.clearFilters = function () {
  this.filters.removeFilters(
    map(this._currentSearchProps, function (prop: any) {
      return [prop]; //for consistent format
    })
  );
};

/**
 * Set the filter to search for matches in properties, and reload to the first
 * page. If searchString is blank, reset the search filter.
 */
ResourceStore.prototype.search = function (searchString: any, props?: any, filterOp?: any) {
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

ResourceStore.prototype.addQueryParam = function (newParam: any) {
  if (this.url.indexOf('?') === -1) {
    this.url += '?' + newParam;
  } else {
    this.url += ',' + newParam;
  }
};
