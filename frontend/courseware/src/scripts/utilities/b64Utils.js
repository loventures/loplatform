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

/**
 * Converts a POJO into a Base64 string that can safely be put into urlParams
 */
export const b64Encode = obj => {
  const jsonData = window.JSON.stringify(obj);
  const b64Data = window.btoa(jsonData);
  return b64Data.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
};

/**
 * Converts a url safe Base64 string into a POJO.  Returns nothing if it fails.
 */
export const b64Decode = urlSafeB64Data => {
  try {
    let b64Data = urlSafeB64Data;
    if (b64Data.length % 4) b64Data += Array(5 - (b64Data.length % 4)).join('=');
    b64Data = b64Data.replace(/-/g, '+').replace(/_/g, '/');
    const jsonData = window.atob(b64Data);
    return window.JSON.parse(jsonData);
  } catch (e) {
    //ignore garbage data
    return;
  }
};
