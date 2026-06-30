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

import { rstr2b64, rstr_sha1, str2rstr_utf8 } from '../utilities/sha1.js';

/**
 * Pure-TS port of the `ChallengeResponseCreator` factory: sets the `X-Challenge-Response` header to the
 * base64 SHA1 of the server's challenge (the de.js/sha1.js dance), for the 202-retry in SessionService's
 * `dehttp`. Side-effects `config.headers` (as the original did) and returns it.
 */
export const challengeResponse = (config: any, o: any): any => {
  try {
    config.headers['X-Challenge-Response'] = rstr2b64(rstr_sha1(str2rstr_utf8(o.data.challenge)));
  } catch (e) {
    console.error('Cannot create challenge response');
  }
  return config;
};
