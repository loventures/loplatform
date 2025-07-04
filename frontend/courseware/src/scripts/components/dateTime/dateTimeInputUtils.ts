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

const DATE_INPUT_FORMAT = 'YYYY-MM-DD';
const TIME_INPUT_FORMAT = 'HH:mm';

export const fromInputToISO = (date: any, time: any) => {
  return dayjs(`${date} ${time}`).toISOString();
};

export const fromInputToDate = (date: any, time: any) => {
  return dayjs(`${date} ${time}`).toDate();
};

export const isValidDateFromInput = (date: any) => {
  return date && dayjs(date, DATE_INPUT_FORMAT).isValid();
};

export const isValidTimeFromInput = (time: any) => {
  return time && String(time).match(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/);
};

export const fromISOToInput = (iso: string) => {
  const dateTime = dayjs(iso);
  return {
    date: dateTime.format(DATE_INPUT_FORMAT),
    time: dateTime.format(TIME_INPUT_FORMAT),
  };
};
