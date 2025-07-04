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

export const CHROME = 'chrome';
export const FF = 'firefox';
export const SAFARI = 'safari';
export const IE = 'ie';
export const EDGE = 'edge';

const BrowserType = function () {
  //keep them in this order
  //the earlier ones includes the later ones
  if (navigator.appVersion.match(/Edge\//)) {
    return EDGE;
  } else if (navigator.appVersion.match(/like Gecko$/)) {
    return IE;
  } else if (navigator.appVersion.match(/Chrome\//)) {
    return CHROME;
  } else if (navigator.appVersion.match(/Safari\//)) {
    return SAFARI;
  } else {
    return FF;
  }
};

const browserType: string = BrowserType();

export default browserType;
