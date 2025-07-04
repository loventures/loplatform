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
import relativeTime from 'dayjs/plugin/relativeTime';
import { padStart } from 'lodash';

dayjs.extend(relativeTime);

const FullFormat = new Intl.DateTimeFormat('en', {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
  hour12: true,
});

const MDFormat = new Intl.DateTimeFormat('en', {
  month: 'short',
  day: 'numeric',
});

const MDYHMFormat = new Intl.DateTimeFormat('en', {
  month: 'numeric',
  day: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
  hour12: true,
});

// Dec 13, 2023, 1:27 PM
export const formatFullDate = (date: string | number | undefined): string | undefined =>
  !date ? undefined : FullFormat.format(new Date(date));

// 2/8/2024, 10:16 AM
export const formatMDYHM = (date: string | number | undefined): string | undefined =>
  !date ? undefined : MDYHMFormat.format(new Date(date));

// May 12
export const formatMD = (date: string | number | undefined): string | undefined =>
  !date ? undefined : MDFormat.format(new Date(date));

export const fromNow = (input: string | number, withoutSuffix?: boolean) =>
  dayjs(input).fromNow(withoutSuffix);

const pad = (n: number) => padStart(n + '', 2, '0');

export const formatDuration = (duration: number): string => {
  const seconds = Math.floor(duration / 1000) % 60;
  const minutes = Math.floor(duration / 60000) % 60;
  const hours = Math.floor(duration / 3600000);
  return pad(hours) + ':' + pad(minutes) + ':' + pad(seconds);
};
