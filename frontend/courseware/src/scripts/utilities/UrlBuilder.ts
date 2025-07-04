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

import { createUrl } from '../bootstrap/loConfig.ts';
import { extend } from 'lodash';

import UrlQuery from './UrlQuery';

/**
 * Class for constructing a URL with SRS-friendly query & matrix parameters.
 *
 * @param {String} url     Base URL template.  Use same conventions as ui-router paths.
 *                         Example: `/api/v2/users/{userId}/enrollments?optionalParam
 *
 * @param {object} params  Hash of values for query parameters.  In the above example
 *                         this would be `{ userId: 1234 }`
 *
 * @param {object} cfg     Hash of filtering options, if you want to set them all when
 *                         the URL object is instantiated.
 *
 * @returns {UrlBuilder}   loURL object
 *
 * */
class UrlBuilder {
  origin: string;
  value: string;
  paramValues?: Record<string, any>;
  query: UrlQuery;

  constructor(
    url: string,
    params?: Record<string, any>,
    urlQuery?: UrlQuery | Record<string, any>
  ) {
    const protocol = window.location.protocol;
    // IE returns "" as the host for a relative URL
    const host = window.location.host;

    this.origin = protocol + '//' + host;
    this.value = url;

    // IE10 weirdness.
    if (this.value.charAt(0) !== '/') {
      this.value = '/' + this.value;
    }

    Object.defineProperty(this, 'length', {
      get: function () {
        return this.value.length;
      },
    });

    params && this.params(params);

    // Load in a default UrlQuery
    if (urlQuery instanceof UrlQuery) {
      this.query = urlQuery;
    } else if (urlQuery) {
      this.query = new UrlQuery(urlQuery);
    } else {
      this.query = new UrlQuery({});
    }
  }

  create(url: string | UrlBuilder) {
    /** TODO: remove. only used in AttachmentService */
    if (!(url instanceof UrlBuilder)) {
      url = new UrlBuilder(url);
    }
    return url;
  }

  toString(): string {
    const matrix = this.query.serializeMatrix();
    const baseUrls = this.baseUrl().split('?');
    const queryString = baseUrls[1] ? '?' + baseUrls[1] : '';
    return baseUrls[0] + matrix + queryString;
  }

  // unused?
  match(regex: RegExp) {
    return this.toString().match(regex);
  }

  baseUrl() {
    let val = this.value;
    if (this.paramValues) {
      val = createUrl(val, this.paramValues);
      if (val.charAt(val.length - 1) === '/') {
        val = val.slice(0, val.length - 1);
      }
    }
    return this.origin + val;
  }

  params(params: Record<string, any>) {
    if (!this.paramValues) {
      this.paramValues = {};
    }
    extend(this.paramValues, params);
    return this;
  }
}

export default UrlBuilder;
