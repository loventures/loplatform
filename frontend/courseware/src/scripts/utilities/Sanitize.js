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

import dayjs from 'dayjs';
import { each, isArray, isObject, isString, union } from 'lodash';

/**
 * @ngdoc object
 * @name lo.utilities.included:Sanitize
 * @description For doing common post process operations on the crazy server
 * results that we do all over the app.  This is primarily to handle date format
 * issues and is run automagically on most of our results from the services.
 *
 * Eventually this will also handle localization of the date formats and be aware
 * of angular-translate settings.
 */

const Sanity = {};

Sanity.DateOnly = 'YYYY-MM-DD';
Sanity.ISO8601 = 'YYYY-MM-DDTHH:mm:ssZ';
Sanity.DEFAULTFORMAT = Sanity.ISO8601;
Sanity.dateKeys = [
  'createDate',
  'editDate',
  'submitDate',
  'dueDate',
  'endDate',
  'startDate',
  'mostRecentSubmissionDate',
];
Sanity.addDateKeys = function (keys) {
  Sanity.dateKeys = union(Sanity.dateKeys, keys);
};

/**
 * @description
 * Take an object reference and convert all our common date params into dayjs
 * objects.  By default it will recurse down one level if that object is an array, for
 * example: {count: 1, objects: [{startDate: str}]} would convert startDate
 *
 * @param {Object} ref object containing the dates you might want to sanitize
 * @param {String} [format] the dayjs format string to use.
 * @param {Object} [lookup] the list of valid keys to dayjsize
 * @returns {Object} Modifies ref in place and returns it for chaining purposes
 */
Sanity.dates = function (ref, format, lookup) {
  if (!ref || !(isArray(ref) || isObject(ref))) {
    return;
  }

  if (!lookup) {
    //Create a default date lookup if none was provided
    lookup = {};
    each(Sanity.dateKeys, function (key) {
      lookup[key] = true;
    });
  }

  var m = null;
  var form = null;
  each(ref, function (val, key) {
    if (lookup[key] && val && isString(val)) {
      if (format) {
        //Hack to support our APIs inconsistent date formats...
        form = format;
      } else {
        form = val.length == Sanity.DateOnly.length ? Sanity.DateOnly : Sanity.ISO8601;
      }
      m = dayjs(val, form);
      if (m && m.isValid && m.isValid()) {
        ref[key] = m.toISOString();
      }
    } else if (isObject(val) || isArray(val)) {
      Sanity.dates(val, format, lookup, false);
    }
  });
  return ref;
};

/**
 * @description
 *
 * Opposite of `dates`; takes an object reference and converts dayjs objects
 * into strings.
 * @param {Object} src object containing the dates you might want to stringify
 * @param {String|Optional} format the dayjs format string to use.
 * @returns {Object} a copy of the src object with dates as strings now
 */
Sanity.stringifyDates = function (src, format) {
  if (!src) {
    return;
  }
  var dest = angular.copy(src);

  format = isString(format) ? format : Sanity.DEFAULTFORMAT;

  each(src, function (val, key) {
    if (val && dayjs.isDayjs(val)) {
      dest[key] = val.format(format);
    }
  });
  return dest;
};

export default Sanity;
