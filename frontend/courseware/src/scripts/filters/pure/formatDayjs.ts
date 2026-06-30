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
import advanced from 'dayjs/plugin/advancedFormat';
import localized from 'dayjs/plugin/localizedFormat';
import timezonePlugin from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';

dayjs.extend(utc);
dayjs.extend(timezonePlugin);
dayjs.extend(localized);
dayjs.extend(advanced);

/** Named format presets accepted as the `format` argument. */
export const FormatDayjsSetFormats: Record<string, string> = {
  time: 'MMM DD h:mm A z',
  full: 'MMM DD, YYYY h:mm A z',
};

/**
 * Format a dayjs-compatible date as a string in the platform's current language.
 *
 * Behaviour is preserved verbatim from the original `formatDayjs` Angular
 * filter; the only intentional change is that a missing `window.lo_platform`
 * (e.g. under unit tests) falls back to 'en' instead of throwing.
 *
 * @param format   a {@link FormatDayjsSetFormats} key, a dayjs format string, or 'l' by default
 * @param timezone an IANA tz name, or 'guessTimezone' to use the browser's zone
 */
export const formatDayjs = (input: dayjs.ConfigType, format?: string, timezone?: string): string => {
  if (!input) {
    return '';
  }
  const d = dayjs(input);

  const lang =
    (typeof window !== 'undefined' && window.lo_platform && window.lo_platform.i18n.language) || 'en';
  const fmt = FormatDayjsSetFormats[format] || format || 'l';

  if (timezone) {
    const zone = timezone === 'guessTimezone' ? dayjs.tz.guess() : timezone;
    return d.locale(lang).tz(zone).format(fmt);
  }
  return d.locale(lang).format(fmt);
};
