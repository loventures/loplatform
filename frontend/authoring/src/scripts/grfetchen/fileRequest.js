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

import _defaultHeaders2 from './defaultHeaders';

import _util from './util';

function _classCallCheck(instance, Constructor) {
  if (!(instance instanceof Constructor)) {
    throw new TypeError('Cannot call a class as a function');
  }
}

var createUrl = function createUrl(url, parameters) {
  return Object.keys(parameters).reduce(function (currentUrl, paramKey) {
    var parameter = parameters[paramKey];
    return currentUrl.replace(':' + paramKey, parameter);
  }, url);
};

var FileRequest = (function () {
  /**
   * Constructor for a FileRequest that creates a defaulted request
   * with the specified method and url
   * @param {String} method which HTTP method to use
   * @param {String} url the resource to fetch
   */
  function FileRequest(method, url) {
    _classCallCheck(this, FileRequest);

    this._method = method;
    this._url = url;
  }

  /**
   * Add file to the request
   *
   * @param {File} file local file
   * @return {object} the file request object
   */

  _createClass(FileRequest, [
    {
      key: 'file',
      value: function file(_file) {
        if (!(_file instanceof window.File)) {
          throw new Error('Not a valid file for upload');
        }
        this._file = _file;
        return this;
      },

      /**
       * Adds additional headers to the default headers for the request. By default
       * the 'X-filename' header is added to the request but this can be used to change
       * 'content-type' or other headers as needed.
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

      /**
       * Sets the parameters for the request; this includes both parameters
       * that are in the path and should be interpolated as well as parameters
       * that should be added as a query string
       *
       * Parameters that are not used in the path will be automatically added
       * to the url as a query parameter
       *
       * @param {object} parameters for the request
       * @return {object} the request
       */
    },
    {
      key: 'params',
      value: function params(parameters) {
        this._parameters = parameters;
        return this;
      },

      /**
       * Executes the build request and returns a Promise.
       *
       * @return {Promise} returns a promise of the executed request
       */
    },
    {
      key: 'exec',
      value: function exec() {
        if (!this._file) {
          throw new Error('No file was specified for upload');
        }

        return window
          .fetch(createUrl(this._url, this._parameters), {
            credentials: 'include',
            method: this._method,
            headers: _extends(
              {},
              _defaultHeaders2,
              {
                'X-Filename': this._file.name,
              },
              this._headers
            ),
            body: this._file,
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
          .catch(function (res) {
            return (0, _util.parseResponseBody)(res).then(function (data) {
              return Promise.reject({
                status: res.status,
                message: data,
              });
            });
          });
      },
    },
  ]);

  return FileRequest;
})();

export default FileRequest;
