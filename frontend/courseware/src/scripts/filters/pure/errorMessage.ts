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

import { isEmpty, map, isArray, each, extend } from 'lodash';

type Translate = (key: string, params?: object) => string;

const isString = (x: unknown): x is string => typeof x === 'string';

/**
 * Extract and translate a human-readable message out of an error — a raw
 * string, an `{ message }`/`{ data: { messages } }` shape, or an HTTP error
 * response — joining multiple messages with a space.
 *
 * Behaviour is preserved verbatim from the original `errorMessage` Angular
 * filter.
 *
 * @param translate i18n function (identity by default); the Angular adapter
 *   supplies `$translate.instant`
 */
export const errorMessage = (errorObj: any, translate: Translate = key => key): string => {
  const error = errorObj && errorObj.data ? errorObj.data : errorObj;
  let messages: string[] = [];
  const interpolateParams: Record<string, unknown> = {};

  if (error && !isEmpty(error.messages)) {
    if (isArray(error.messages)) {
      each(error.messages, function (m: any) {
        if (m) {
          if (m.i18nableMessage) {
            extend(interpolateParams, m.data);
            messages.push(m.i18nableMessage);
          } else if (m.message) {
            messages.push(m.message);
          } else if (isString(m)) {
            messages.push(m);
          }
        }
      });
    } else if (isString(error.messages.message)) {
      messages = [error.messages.message];
    }
  } else if (error && error.message) {
    messages = [error.message];
  } else if (isString(error)) {
    messages = [error];
  }

  if (!messages.length) {
    messages = ['Error, Unknown issue.'];
  }

  return map(messages, msg => translate(msg, interpolateParams)).join(' ');
};
