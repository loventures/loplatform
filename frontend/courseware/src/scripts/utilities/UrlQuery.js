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
  each,
  eachRight,
  every,
  isBoolean,
  isEmpty,
  isNumber,
  isObject,
  isUndefined,
  keys,
  map,
  uniq,
} from 'lodash';

/**
 * @ngdoc type
 * @memberof lo.services.UrlQuery
 * @description
 *     Class for constructing matrix and pagination parameters.  Use with {@link UrlBuilder}.
 * @param {object} cfg  Hash of filtering options to pre-populate into query object
 Delegates to
 *                      {@link lo.services.UrlQuery#methods_setCfg setCfg()}
 *
 * @returns {UrlQuery}   UrlQuery object
 */
function UrlQuery(cfg) {
  this.loadQuery(cfg);
}

/**
 * @description
 *     Gets the current page number.
 * @returns {UrlQuery} params object
 */
UrlQuery.prototype.page = function () {
  return parseInt(this.offset / this.limit, 10);
};

/**
 * @description
 *     Update pagination parameters to go to next page.
 *     Does not guarantee that the page will exist.
 * @param {number} page Page number.
 * @returns {UrlQuery} params object
 */
UrlQuery.prototype.gotoPage = function (page) {
  return this.setOffset(this.limit * page);
};

/**
 * @description
 *     Update pagination parameters to go to next page.
 *     Does not guarantee that the page will exist.
 * @returns {UrlQuery} params object
 */
UrlQuery.prototype.nextPage = function () {
  return this.gotoPage(this.page() + 1);
};

/**
 * @description
 *     Update pagination parameters to go to next page.
 *     Does not guarantee that the page will exist.
 * @returns {UrlQuery} params object
 */
UrlQuery.prototype.prevPage = function () {
  return this.gotoPage(this.page() - 1);
};

/* Helper method for turning 'asc' into 'desc' */
var opposite = function (dir) {
  if (dir == 'asc') {
    return 'desc';
  }
  if (dir == 'desc') {
    return 'asc';
  }
  return 'asc'; // Why not.
};

/*
    Helper method for translating
    -1 to descending,
    1 to ascending.
    god help you for 0
    and if not a number return whatever passed in
*/
var getDirection = function (order) {
  if (isNaN(order)) {
    return order;
  } else {
    return parseInt(order, 10) >= 0 ? 'asc' : 'desc';
  }
};

/**
 * @description Clear order params
 */
UrlQuery.prototype.clearOrder = function () {
  this.orderPriority = [];
  this.orderValues = {};
  this.setMatrix('order', null);
};

/**
 * @description
 *      Changes the sorting direction on a given property.
 *      If no property is specified, flips sorting direction
 *      on all properties.
 *      If more than one properties are given, the sorting priority will be in that order
 *      If switchDirection is called multiple times, later property has higher prioirity
 *
 *      query.switchDirection('age');
 *      query.switchDirection(['firstName', 'lastName']);
 *      //result: firstName:asc;lastName:asc;age:asc
 *
 *      query.switchDirection('lastName')
 *      //result: lastName:desc;firstName:asc;age:asc
 *
 *      query.switchDirection('firstName', true)
 *      //result: firstName:desc
 *
 * @param {string|Array} props Property or array of properties to switch sorting direction on.
 * @param {boolean} startNew Clear order of any previously set sorts before switching direction.
 * @returns {UrlQuery} params object
 */
UrlQuery.prototype.switchDirection = function (props, startNew) {
  this.orderPriority = this.orderPriority || [];
  this.orderValues = this.orderValues || {};

  if (!props) {
    props = angular.copy(this.orderPriority);
  }

  if (!Array.isArray(props)) {
    props = [props];
  }

  var isMostRecentOrder = every(
    props,
    function (prop, index) {
      return this.orderPriority[index] === prop;
    }.bind(this)
  );

  for (var i = props.length - 1; i >= 0; i--) {
    var prop = props[i];
    var order = isMostRecentOrder
      ? opposite(this.orderValues[prop])
      : this.orderValues[prop] || opposite();
    if (startNew && !isMostRecentOrder) {
      this.clearOrder();
    }
    this.setOrder(prop, order);
  }

  return this;
};

/**
 * Sorts based on config
 * Order is REVERSED to maintain priority
 * @param configs {object} Array of property configs { property, order}
 */
UrlQuery.prototype.sort = function (configs) {
  for (var i = configs.length - 1; i >= 0; i--) {
    var config = configs[i];
    var prop = config.property;
    var orderStr = getDirection(config.order);
    this.setOrder(prop, orderStr);
  }

  return this;
};

UrlQuery.prototype.setOrders = function (orders) {
  eachRight(orders, config => this.setOrder(config.property, config.order));

  return this;
};

/**
 * @description
 *     Sets a sort order for the query response.
 *     Will bump the the priority of the last sorting prop to front
 * @param   {string} prop  the sorting property to be used
 * @param   {string} order  asc or desc, the order to sort by
 * @returns {UrlQuery}     params object
 */
UrlQuery.prototype.setOrder = function (prop, order) {
  if (!order && Array.isArray(prop)) {
    eachRight(
      prop,
      function (config) {
        this.setOrder(config.property, config.order);
      }.bind(this)
    );

    return this;
  } else if (!order && isObject(prop)) {
    this.setOrder(prop.property, prop.order);
    return this;
  }

  this.orderPriority = this.orderPriority || [];
  this.orderValues = this.orderValues || {};

  var existingOrderIndex = this.orderPriority.indexOf(prop);
  if (existingOrderIndex !== -1) {
    this.orderPriority.splice(existingOrderIndex, 1);
  }
  this.orderPriority.unshift(prop);
  this.orderValues[prop] = order;

  var m = map(
    this.orderPriority,
    function (prop) {
      return prop + ':' + this.orderValues[prop];
    }.bind(this)
  );
  return this.setMatrix('order', m.join(','));
};

/**
 * @description
 *     Sets the "start from" point for a paginated query
 * @param   {number}     num   Skip this many results.
 * @returns {UrlQuery} params object
 */
UrlQuery.prototype.setOffset = function (num) {
  this.offset = num;
  return this.setMatrix('offset', num);
};

/**
 * @description
 *     Sets the number of desired results per page.  May be overruled
 *     by the server.
 * @param   {number}     num   This many per page
 * @returns {UrlQuery} params object
 */
UrlQuery.prototype.setLimit = function (num) {
  this.limit = num;
  return this.setMatrix('limit', num);
};

UrlQuery.prototype.setEmbed = function (embed) {
  return this.setEmbeds(embed);
};

/**
 * @description
 *     Child components to embed with this response.
 * @param   {array}       num   This many per page
 * @returns {UrlQuery}  params object
 */
UrlQuery.prototype.setEmbeds = function (embeds) {
  if (!angular.isArray(embeds)) {
    embeds = [embeds];
  }
  this.embeds = embeds;
  return this.setMatrix('embed', embeds.join(','));
};

/**
 * @description
 *      Add embeds to existing list of embeds
 *      Should check for uniqueness
 * @param   {array}       num   This many per page
 * @returns {UrlQuery}  params object
 */
UrlQuery.prototype.addEmbeds = function (embeds) {
  if (!angular.isArray(embeds)) {
    embeds = [embeds];
  }
  this.embeds = uniq((this.embeds || []).concat(embeds));
  return this.setMatrix('embed', this.embeds.join(','));
};

var removeFilter = function (query, filter, prop, op, val) {
  if (!query[filter]) {
    return;
  } else if (!prop || query[filter][prop]) {
    query[filter] = null;
  } else if (!op || !query[filter][prop][op]) {
    query[filter][prop] = null;
  } else if (!val) {
    query[filter][prop][op] = null;
  } else {
    query[filter][prop][op][val] = null;
  }
};

// There has never, in the history of humanity, been worse code
var addFilter = function (query, filter, prop, op, val) {
  query[filter] = query[filter] || {};
  query[filter][prop] = query[filter][prop] || {};
  query[filter][prop][op] = query[filter][prop][op] || {};
  query[filter][prop][op][val] = true;
};

/*
    Underlying functionality shared between
    proto.setFilter and proto.setPrefilter
 */
var setFilter = function (pre, unset) {
  var filter = pre ? 'prefilter' : 'filter';

  return function (prop, op, val) {
    if (unset) {
      removeFilter(this, filter, prop, op, val);
    } else {
      if (isUndefined(prop) || isUndefined(op) || isUndefined(val)) {
        console.error('Incomplete filter config:', prop, op, val);
      }
      addFilter(this, filter, prop, op, val);
    }

    var filterString = [];
    each(this[filter], function (propFilters, prop) {
      each(propFilters, function (opFilters, op) {
        each(opFilters, function (isActive, val) {
          if (isActive) {
            var prefix = prop === '' ? prop : prop + ':';
            val = window.encodeURIComponent(val);
            filterString.push(prefix + op + '(' + val + ')');
          }
        });
      });
    });

    return this.setMatrix(filter, filterString.join(','));
  };
};

/*
    Underlying functionality shared between
    proto.setFilters and proto.setPrefilters
 */
var setFilters = function (pre, unset) {
  var fn = setFilter(pre, unset);

  return function (filters) {
    each(
      filters,
      function (filter) {
        fn.apply(this, filter);
      }.bind(this)
    );
    return this;
  };
};

/**
 * @description
 *     Add a single filter expression to the query.  Note that
 *     this operation is additive.
 * @param   {string}        prop  property to filter by
 * @param   {string}        op    operation - function name or comparison type.
 *                                For function names, be sure to pass in empty strings for prop and value.
 *                                Ex: ['','isAssignmentType',''] => filter=isAssignmentType()
 *                                Valid comparison types are: `eq`, `contains`, and `neq`
 * @param   {string}        value  value to filter on
 * @returns {UrlQuery}  params object
 */
UrlQuery.prototype.setFilter = setFilter();

/**
 * @description
 *     Convenience method to add an array of filter expressions.
 * @param   {array}  prop  Array of filter expressions:
 *                         `[[prop, op, val]]`
 * @returns {UrlQuery}   params object
 */
UrlQuery.prototype.setFilters = setFilters();

/**
 * @description
 *     Add a single pre-filter expression to the query.  Note that
 *     this operation is additive.
 * @param   {string}        prop  property to filter by
 * @param   {string}        op    comparison operator.  Valid values are:
 *                                `eq`, `contains`, and `neq`
 * @param   {string}        value  value to filter on
 * @returns {UrlQuery}  params object
 */
UrlQuery.prototype.setPrefilter = setFilter(true);

/**
 * @description
 *     Convenience method to add an array of pre-filter expressions.
 * @param   {array}  prop  Array of filter expressions:
 *                         `[[prop, op, val]]`
 * @returns {UrlQuery}   params object
 */
UrlQuery.prototype.setPrefilters = setFilters(true);

/**
 * @description
 *     Sets the operation we use to join multiple filter expressions
 *     together.  Want a complicated query?  Tough.
 * @param   {string}  op  `and` or `or
 * @returns {urlQuery}   params object
 */
UrlQuery.prototype.setFilterOp = function (op) {
  this.filterOp = op;
  return this.setMatrix('filterOp', op);
};

/**
 * @description
 *     Removes a filter.
 * @param   {string}  prop property to remove from filter expression
 * @returns {urlQuery}   params object
 */
UrlQuery.prototype.removeFilter = setFilter(false, true);

/**
 * @description
 *     Removes several filters.
 * @param   {array}  prop  Array of filter expressions:
 *                         `[[prop, op, val]]`
 * @returns {urlQuery}   params object
 */
UrlQuery.prototype.removeFilters = setFilters(false, true);

/**
 * @description
 *     Removes a prefilter.
 * @param   {string}  prop property to remove from prefilter expression
 * @returns {urlQuery}   params object
 */
UrlQuery.prototype.removePrefilter = setFilter(true, true);

/**
 * @description
 *     Removes several prefilter.
 * @param   {array}  prop  Array of prefilter expressions:
 *                         `[[prop, op, val]]`
 * @returns {urlQuery}   params object
 */
UrlQuery.prototype.removePrefilters = setFilters(true, true);

/**
 * @description
 *     Bulk loads all known pagination/filter/param options from a
 *     single hash.
 * @param   {object}  cfg   Configuration object, the following properties:
 *                              'filters','filter','filterOp','embeds',
 *                              'offset','limit','order','params'
 *                          will be set using predefined setter methods.
 *                          Other properties will be set directly onto the matrix params.
 * @returns {urlQuery}   loURL object
 */
UrlQuery.prototype.loadQuery = function (cfg) {
  if (!cfg) {
    return this;
  }

  var props = [
    'filters',
    'filter',
    'filterOp',
    'prefilter',
    'prefilters',
    'embed',
    'embeds',
    'offset',
    'limit',
    'order',
    'orders',
    'params',
  ];
  each(
    props,
    function (prop) {
      if (!isUndefined(cfg[prop])) {
        this.setProp(prop, cfg[prop]);
      }
    }.bind(this)
  );

  each(
    cfg,
    function (value, prop) {
      if (props.indexOf(prop) === -1) {
        console.info('Adding custom matrix prop', prop);
        this.setMatrix(prop, value);
      }
    }.bind(this)
  );

  return this;
};

UrlQuery.prototype.setProp = function (prop, val) {
  var setter = 'set' + prop.charAt(0).toUpperCase() + prop.slice(1);
  if (
    Array.isArray(val) &&
    prop !== 'embeds' &&
    prop !== 'orders' &&
    prop !== 'filters' &&
    prop !== 'prefilters'
  ) {
    this[setter].apply(this, val);
  } else {
    this[setter](val);
  }
};

/*
   Used internally to create a hash of matrix parameters that
   we can serialize.
*/
UrlQuery.prototype.setMatrix = function (param, value) {
  if (!this.matrix) {
    this.matrix = {};
  }
  if (isEmpty(value) && !isNumber(value) && !isBoolean(value)) {
    // console.log("dmatrix", param, value);
    delete this.matrix[param];
  } else {
    // console.log("amatrix", param, value);
    this.matrix[param] = value;
  }
  return this;
};

/*
    Used by UrlBuilder to stringify matrix parameters
*/
UrlQuery.prototype.serializeMatrix = function () {
  var matrix = '';
  if (this.matrix && keys(this.matrix)) {
    matrix =
      ';' +
      map(this.matrix, function (val, key) {
        return key + '=' + val;
      }).join(';');
  }
  return matrix;
};

export default UrlQuery;
