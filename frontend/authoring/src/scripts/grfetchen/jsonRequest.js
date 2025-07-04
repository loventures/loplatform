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

'use strict';

import { BProgress } from '@bprogress/core';

var _createClass = (function () {
  function defineProperties(target, props) {
    for (var i = 0; i < props.length; i++) {
      var descriptor = props[i];
      descriptor.enumerable = descriptor.enumerable || false;
      descriptor.configurable = true;
      if ('value' in descriptor) descriptor.writable = true;
      Object.defineProperty(target, descriptor.key, descriptor);
    }
  }
  return function (Constructor, protoProps, staticProps) {
    if (protoProps) defineProperties(Constructor.prototype, protoProps);
    if (staticProps) defineProperties(Constructor, staticProps);
    return Constructor;
  };
})();

var _extends =
  Object.assign ||
  function (target) {
    for (var i = 1; i < arguments.length; i++) {
      var source = arguments[i];
      for (var key in source) {
        if (Object.prototype.hasOwnProperty.call(source, key)) {
          target[key] = source[key];
        }
      }
    }
    return target;
  };

import _queryString2 from 'query-string';

import _defaultHeaders2 from './defaultHeaders';

import _util from './util';

function _classCallCheck(instance, Constructor) {
  if (!(instance instanceof Constructor)) {
    throw new TypeError('Cannot call a class as a function');
  }
}

var createUrl = function createUrl(url, parameters) {
  var queryParams = {};
  var interpolatedUrl = Object.keys(parameters).reduce(function (currentUrl, paramKey) {
    var parameter = parameters[paramKey];
    if (currentUrl.includes(':' + paramKey)) {
      return currentUrl.replace(':' + paramKey, parameter);
    } else {
      queryParams[paramKey] = parameters[paramKey];
      return currentUrl;
    }
  }, url);

  var queryParamsStringified = _queryString2.stringify(queryParams);

  return queryParamsStringified.length > 0
    ? interpolatedUrl + '?' + queryParamsStringified
    : interpolatedUrl;
};

var constructCredentials = function constructCredentials(credentials) {
  if (credentials === false) {
    return {
      credentials: 'omit',
    };
  }

  if (!credentials) {
    return {
      credentials: 'same-origin',
    };
  }

  return {
    credentials: credentials,
  };
};

var createConfiguration = function createConfiguration(request) {
  // TODO should we set a cache option?
  if (request._body) {
    return _extends(
      {
        method: request._method,
      },
      constructCredentials(request._credentials),
      {
        headers: _extends({}, _defaultHeaders2, request._headers),
        body: request._body,
      },
      request._cancel
    );
  }

  return _extends(
    {
      method: request._method,
    },
    constructCredentials(request._credentials),
    {
      headers: _extends({}, _defaultHeaders2, request._headers),
    },
    request._cancel
  );
};

let count = 0;

var JsonRequest = (function () {
  /**
   * Constructor for a JsonRequest that creates a defaulted request
   * with teh specified method and url
   * @param {String} method which HTTP method to use
   * @param {String} url the resource to fetch
   */
  function JsonRequest(method, url) {
    _classCallCheck(this, JsonRequest);

    this._url = url;
    this._method = method;
    this._parameters = {};
    this._cancel = {};
    if (method === 'POST' || method === 'PUT') {
      this._headers = {
        'content-type': 'application/json',
        accept: 'application/json',
      };
    } else {
      this._headers = {
        accept: 'application/json',
      };
    }
  }

  /**
   * Sets the parameters for the request; this includes both parameters
   * that are in the path and should be interpolated as well as parameters
   * that should be added as a query string
   *
   * Parameters that are not used in the path will be automatically added
   * to the url as a query parameter
   *
   * @param {object} the parameters for the request
   * @return {object} the request
   */

  _createClass(JsonRequest, [
    {
      key: 'params',
      value: function params(parameters) {
        this._parameters = parameters;
        return this;
      },

      /**
       * Sets the data that should be added to the request
       *
       * @throws {Error} if the method is not PUT or POST
       * @param {object} the body for the request
       * @return {object} the request
       */
    },
    {
      key: 'data',
      value: function data(jsonData) {
        if (this._method == 'GET' || this._method == 'DELETE') {
          throw new Error('Data cannot be set for ' + this._method);
        }
        // TODO: investigate using a safe json stringify
        this._body = JSON.stringify(jsonData);
        return this;
      },

      /**
       * Adds additional headers to the default headers for the request. By default
       * both 'content-type' and 'accept' headers are set to 'application/json'
       *
       * @param {object} additionalHeaders
       * @return {object} the request
       */
    },
    {
      key: 'headers',
      value: function headers(additionalHeaders) {
        this._headers = _extends({}, this._headers, additionalHeaders);
        return this;
      },
    },
    {
      key: 'useCredentials',
      value: function useCredentials(credentials) {
        this._credentials = credentials;
        return this;
      },

      /**
       * Makes the request cancellable if an AbortController is passed in.
       *
       * @param {AbortController} abortController Instance of an AbortController
       * @return {object} the request
       * */
    },
    {
      key: 'makeCancellable',
      value: function makeCancellable(abortController) {
        if (abortController && abortController.signal) {
          this._cancel = {
            signal: abortController.signal,
          };
        }
        return this;
      },

      /**
       * Executes the build request and returns a Promise.  A resolved Promise
       * will return the JSON fetched. A rejected Promise will return the status
       * code for the response and the error JSON from the response
       *
       * Potential network timeout errors are caught first and then any reponses
       * with statues >300 are transferred into errors.
       *
       *
       *
       * @return {Promise} returns a promise of the executed request
       */
    },
    {
      key: 'silent',
      value: function silent() {
        this._silent = true;
        return this;
      },
    },
    {
      key: 'exec',
      value: function exec() {
        if (!this._silent && !count++) BProgress.start();
        return window
          .fetch(createUrl(this._url, this._parameters), createConfiguration(this))
          .catch(function (res) {
            return (0, _util.parseResponseBody)(res).then(function (data) {
              return Promise.reject({
                status: res.status,
                message: data,
              });
            });
          })
          .then(function (res) {
            // 204 No Content cannot be parsed into JSON
            if (res.status === 204) {
              return {};
            }

            if (res.status >= 200 && res.status < 300) {
              return (0, _util.parseResponseBody)(res);
            }

            return (0, _util.parseResponseBody)(res).then(function (message) {
              return Promise.reject({
                status: res.status,
                message: message,
              });
            });
          })
          .finally(() => {
            if (!this._silent && !--count) BProgress.done();
          });
      },
    },
  ]);

  return JsonRequest;
})();

export default JsonRequest;
