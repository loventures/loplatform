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

'use strict';

import _jsonRequest2 from './jsonRequest';

import _fileRequest2 from './fileRequest';

var createJsonRequest = function createJsonRequest(method: string, url: string) {
  return new (_jsonRequest2 as any)(method, url);
};

var createFileRequest = function createFileRequest(method: string, url: string) {
  return new (_fileRequest2 as any)(method, url);
};

/**
 * A GET request
 * @param {String} url
 * @return {JsonRequest} a request builder
 */
var get = function get(url: string) {
  return createJsonRequest('GET', url);
};

/**
 * A PUT request
 * @param {String} url
 * @return {JsonRequest} a request builder
 */
var put = function put(url: string) {
  return createJsonRequest('PUT', url);
};

/**
 * A DELETE request
 * @param {String} url
 * @return {JsonRequest} a request builder
 */
var _delete = function _delete(url: string) {
  return createJsonRequest('DELETE', url);
};

/**
 * A POST request
 * @param {String} url
 * @return {JsonRequest} a request builder
 */
var post = function post(url: string) {
  return createJsonRequest('POST', url);
};

/**
 * This is the default export for the package and exposes GET/POST/PUT/DELETE http request methods
 */
export const jsonApi = {
  get: get,
  put: put,
  post: post,
  delete: _delete,
};

/**
 * A PUT request
 * @param {String} url
 * @return {FileRequest} a request builder
 */
var putFile = function putFile(url: string) {
  return createFileRequest('PUT', url);
};

/**
 * A POST request
 * @param {String} url
 * @return {FileRequest} a request builder
 */
var postFile = function postFile(url: string) {
  return createFileRequest('POST', url);
};

/**
 *  This is the fileApi export that exposes PUT/POST http request methods for file uploads.
 */
export const fileApi = {
  put: putFile,
  post: postFile,
};

export default jsonApi;
