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

import classNames from 'classnames';
import GroupMessagesByUser from '../qna/GroupMessagesByUser';
import { QnaQuestionDto } from '../qna/qnaApi';
import dayjs from 'dayjs';
import { groupBy } from 'lodash';
import React from 'react';

const GroupMessagesByDate: React.FC<{
  question: QnaQuestionDto;
}> = ({ question }) => {
  const groupedMessages = groupBy(question.messages, m =>
    dayjs(m.created).format('MMM D, YYYY h:mm A')
  );
  const dates = Object.keys(groupedMessages).sort((a, b) => {
    const up = dayjs(a, 'MMM D, YYYY h:mm A');
    const down = dayjs(b, 'MMM D, YYYY h:mm A');
    return up.isBefore(down) ? -1 : 1;
  });

  return (
    <div
      className={classNames('d-flex flex-column px-2', {
        'closed-question': question.closed,
        'open-question': question.open,
      })}
    >
      <div className="d-flex justify-content-start flex-column border-dark group-by-date">
        {dates.map((date, idx) => (
          <GroupMessagesByUser
            key={date}
            question={question}
            messages={groupedMessages[date]}
            date={date}
            initial={!idx}
          />
        ))}
      </div>
    </div>
  );
};

export default GroupMessagesByDate;
