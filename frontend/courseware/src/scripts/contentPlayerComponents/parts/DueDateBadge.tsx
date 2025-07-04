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

import classnames from 'classnames';
import dayjs from 'dayjs';
import { TranslationContext } from '../../i18n/translationContext.tsx';
import { nearDueWarning, strictDueDate } from '../../utilities/preferences.ts';
import React, { useContext, useMemo } from 'react';

const DueDateBadge: React.FC<{
  date: string;
  completed?: boolean;
  exempt?: boolean;
}> = ({ date, completed = false, exempt = false }) => {
  const translate = useContext(TranslationContext);

  const isPastDueDate = useMemo(() => {
    return dayjs().isAfter(date);
  }, [date]);

  const isNearDueDate = useMemo(() => {
    const dateDiff = dayjs(date).diff(dayjs(), 'days');
    return !isPastDueDate && dateDiff <= nearDueWarning;
  }, [date, nearDueWarning]);

  const classes = useMemo(() => {
    return classnames('block-badge due-date-time', {
      'over-due': !exempt && !completed && isPastDueDate,
      'near-due': !exempt && !completed && isNearDueDate,
    });
  }, [completed, isPastDueDate]);

  const prefixTKey = useMemo(() => {
    if (exempt || completed) {
      return 'NORMAL_DUE';
    } else if (!completed && isPastDueDate) {
      if (strictDueDate) {
        return 'OVER_DUE_CANNOT_COMPLETE';
      } else {
        return 'OVER_DUE_CAN_COMPLETE';
      }
    } else if (isNearDueDate) {
      return 'NEAR_DUE';
    } else {
      return 'NORMAL_DUE';
    }
  }, [isPastDueDate, isNearDueDate, exempt, completed]);

  return (
    <span className={classes}>
      <span>{translate(prefixTKey)}:&nbsp;</span>
      <span>{translate('DUE_DATE_TIME', { time: date })}</span>
    </span>
  );
};

export default DueDateBadge;
