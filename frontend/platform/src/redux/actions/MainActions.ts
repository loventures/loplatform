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

import Polyglot from 'node-polyglot';

/*
 * action types
 */
export const SET_LO_PLATFORM = 'SET_LO_PLATFORM';
export const SET_PORTAL_ALERT_STATUS = 'SET_PORTAL_ALERT_STATUS';
export const SET_TRANSLATIONS = 'SET_TRANSLATIONS';

/*
 * action creators
 */
export function setLoPlatform(lo_platform: any) {
  return { type: SET_LO_PLATFORM, lo_platform };
}

export function setPortalAlertStatus(error: any, success: boolean, message: string) {
  return { type: SET_PORTAL_ALERT_STATUS, error, success, message };
}

export function setTranslations(translations: Polyglot) {
  return { type: SET_TRANSLATIONS, translations };
}
