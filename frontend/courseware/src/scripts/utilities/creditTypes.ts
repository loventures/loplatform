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

import { ExtractValue } from '../types/objects.ts';

export const CreditTypes = {
  NoCredit: 'NoCredit',
  Credit: 'Credit',
  ExtraCredit: 'ExtraCredit',
} as const;

export const NoCredit = CreditTypes.NoCredit;
export const Credit = CreditTypes.Credit;
export const ExtraCredit = CreditTypes.ExtraCredit;

export type CreditType = ExtractValue<typeof CreditTypes>;

export function isForCredit(c: CreditType): c is 'Credit' {
  return c === 'Credit';
}
