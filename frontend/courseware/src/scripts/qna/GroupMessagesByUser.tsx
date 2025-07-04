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
import { QnaMessageDto, QnaQuestionDto } from '../qna/qnaApi';
import QnaMessage from '../qna/QnaMessage';
import React from 'react';

import { selectCurrentUser } from '../utilities/rootSelectors';
import { useCourseSelector } from '../loRedux';

type GroupMessageByUserProps = {
  question: QnaQuestionDto;
  messages: QnaMessageDto[];
  date: string;
  initial: boolean;
};

const GroupMessagesByUser: React.FC<GroupMessageByUserProps> = ({
  question,
  messages,
  date,
  initial,
}) => {
  const user = useCourseSelector(selectCurrentUser);
  const groupedMessages = messages.reverse().reduce<QnaMessageDto[][]>((acc, m) => {
    if (acc.length) {
      if (m.creator.id === acc[0][0].creator.id) {
        acc[0].unshift(m);
        return acc;
      } else {
        acc.unshift([m]);
        return acc;
      }
    } else {
      return [[m]];
    }
  }, []);

  return (
    <>
      {groupedMessages.map((ms, idx) => {
        const creator = ms[0].creator;
        return (
          <div
            key={idx}
            className="message-group d-flex flex-column mb-2"
          >
            <div
              className={classNames('text-muted d-flex small mw-100', {
                'text-right align-self-end': creator.id === user.id,
              })}
            >
              <span className={classNames('text-truncate ms-2 ps-1', { 'me-2 pe-1': idx })}>
                {creator.fullName}
              </span>
              {!idx ? (
                <>
                  ,&nbsp;
                  <span className="me-2 pe-1 text-nowrap">{date}</span>
                </>
              ) : null}
            </div>
            {ms.map((message, jdx) => (
              <QnaMessage
                key={message.id}
                question={question}
                message={message}
                first={!jdx}
                last={jdx === ms.length - 1}
                initial={initial && !idx && !jdx}
              />
            ))}
          </div>
        );
      })}
    </>
  );
};

export default GroupMessagesByUser;
