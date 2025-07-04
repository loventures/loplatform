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
 *  Turns decimals into percentages.
 *
 * @param {number|string} percentage Decimal number to be converted
 *    into a percentage (ie. 0.8 -> 80%)
 * @param {number} [precision] number of decimals to round to
 * @returns {string} Formatted string as a human-readable percentage.
 *
 */
export const percentFilter = (value, precision = 0) => {
  if (Number.isNaN(value) || value === undefined) {
    return '– –';
  }
  if (Number.isFinite(value)) {
    // automatically adds the % character.
    return Number(value).toLocaleString(undefined, {
      style: 'percent',
      minimumFractionDigits: precision,
    });
  }
  return value + '%';
};
