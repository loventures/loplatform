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

import Polyglot from 'node-polyglot';

export const formatSize = (s: number, T: Polyglot): string =>
  s < 1024
    ? T.t('format.size.bytes', { size: s })
    : s < 1024 * 1024
      ? T.t('format.size.KB', { size: Math.floor(s / 1024) })
      : s < 1024 * 1024 * 1024
        ? T.t('format.size.MB', { size: Math.floor(s / 1024 / 1024) })
        : T.t('format.size.GB', { size: Math.floor(s / 1024 / 1024 / 1024) });
