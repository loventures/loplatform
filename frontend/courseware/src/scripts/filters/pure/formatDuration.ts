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
import duration from 'dayjs/plugin/duration';

dayjs.extend(duration);

type Translate = (key: string) => string;
type Duration = ReturnType<typeof dayjs.duration>;

/**
 * Translate function used by the human-readable format. Defaults to identity so
 * the 'short' and regular formats — which never translate — work standalone.
 * The Angular adapter supplies `$translate.instant`.
 */
const identity: Translate = key => key;

function pluralize(translate: Translate, num: number, txt: string): string {
  if (num !== 1) {
    txt += 's';
  }
  txt = translate(txt) || txt;
  return '' + num + ' ' + txt + ' ';
}

function pad(num: number): string {
  return '0' + num;
}

function formatHuman(translate: Translate, input: Duration, rest: boolean): string {
  let out = '';
  if (input.days() > 0) {
    out += pluralize(translate, input.days(), 'Day');
  }
  if (rest || input.hours() > 0) {
    out += pluralize(translate, input.hours(), 'Hour');
  }
  if (rest || input.minutes() > 0) {
    out += pluralize(translate, input.minutes(), 'Minute');
  }
  if (rest || input.seconds() > 0) {
    out += pluralize(translate, input.seconds(), 'Second');
  }
  return out;
}

function formatHumanShort(translate: Translate, input: Duration, rest: boolean): string {
  let out = '';
  if (input.days() > 0) {
    out += pluralize(translate, input.days(), 'day');
  }
  if (rest || input.hours() > 0) {
    out += input.hours() + ' hr ';
  }
  if (rest || input.minutes() > 0) {
    out += input.minutes() + ' min ';
  }
  if (rest || input.seconds() > 0) {
    out += input.seconds() + ' sec ';
  }
  return out;
}

function formatRegular(input: Duration): string {
  let hours: number | string = Math.floor(input.asHours());
  let minutes: number | string = input.minutes();
  let seconds: number | string = input.seconds();

  if (hours > 99) {
    return '99:59:59';
  }
  if (hours < 10) {
    hours = pad(hours);
  }
  if (minutes < 10) {
    minutes = pad(minutes);
  }
  if (seconds < 10) {
    seconds = pad(seconds);
  }
  return hours + ':' + minutes + ':' + seconds;
}

/**
 * Format a dayjs duration as a string.
 *
 * Behaviour matches the original `formatDuration` Angular filter, with one bug
 * fix: a pre-built dayjs Duration is now used directly. dayjs.duration() does
 * NOT round-trip a Duration instance (it reads zero), so re-wrapping one — as
 * the original did — produced blank / "NaN:00:00" output. Callers that build a
 * Duration (e.g. formatContentDuration) now render correctly; number (ms),
 * string, and units-object inputs are unaffected.
 *
 * @param human 'short' => "1 hr 2 min 3 sec"; truthy => "1 Hour 2 Minutes …";
 *   falsy => "HH:MM:SS"
 * @param translate i18n function for the human format (identity by default)
 */
export const formatDuration = (
  // dayjs treats a bare number as milliseconds; strings/units-objects and
  // pre-built Durations are all accepted.
  input: number | string | object,
  human?: boolean | 'short' | 'human',
  translate: Translate = identity
): string => {
  const inputDayJs = dayjs.isDuration(input) ? input : dayjs.duration(input as never);
  let out = '';
  const rest = !human;
  if (inputDayJs) {
    if (human === 'short') {
      out = formatHumanShort(translate, inputDayJs, rest);
    } else if (human) {
      out = formatHuman(translate, inputDayJs, rest);
    } else {
      out = formatRegular(inputDayJs);
    }
  } else {
    if (!human) {
      out = '00:00:00';
    }
  }
  return out.trim();
};
