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

import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import duration from 'dayjs/plugin/duration';

dayjs.extend(relativeTime);
dayjs.extend(duration);

/**
 * Human-readable amount of time between now and `input` (e.g. "a day ago"),
 * via dayjs' relativeTime plugin.
 *
 * Behaviour is preserved verbatim from the original `fromNow` Angular filter.
 *
 * @param suffix     suppress the "ago"/"in" suffix when true
 * @param asDuration treat `input` as a duration (humanize) rather than an absolute date
 */
export const fromNow = (input: dayjs.ConfigType, suffix?: boolean, asDuration?: boolean): string =>
  asDuration ? dayjs.duration(input as number).humanize(suffix) : dayjs(input).fromNow(suffix);
