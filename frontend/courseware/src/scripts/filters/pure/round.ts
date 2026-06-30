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

/**
 * Round a value to a specific number of decimal places (default 2).
 *
 * Unlike the `number` filter this returns a number (not a string) and does not
 * render trailing zeroes. Negative `decimals` rounds the integer part.
 *
 * Behaviour is preserved verbatim from the original `round` Angular filter,
 * including the quirk that any falsy-but-nonzero value yields `null`.
 */
export const round = (value: number, decimals?: number): number | null => {
  if (value === 0) {
    return 0;
  }
  if (!value) {
    return null;
  }
  const d = decimals == null || isNaN(decimals) ? 2 : decimals;
  const p = Math.pow(10, d);
  return Math.round(value * p) / p;
};
