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
import React from 'react';

import PreviewFileRow from '../components/fileViews/PreviewFileRow';
import { selectCurrentUser } from '../utilities/rootSelectors';
import { useCourseSelector } from '../loRedux';
import {
  getMessageAttachmentSignedUrl,
  getMessageAttachmentUrl,
  QnaMessageDto,
  QnaQuestionDto,
} from './qnaApi';

const QnaMessage: React.FC<{
  question: QnaQuestionDto;
  message: QnaMessageDto;
  first: boolean;
  last: boolean;
  initial: boolean;
}> = ({ question, message, first, last, initial }) => {
  const user = useCourseSelector(selectCurrentUser);

  return (
    <>
      <div
        key={message.id}
        className={classNames('chat-body-box', {
          'local-user': message.creator.id === user.id,
          first,
          last: last && message.attachments.length === 0,
        })}
      >
        {initial && question.subject && (
          <div className="font-weight-bold mb-1">{question.subject}</div>
        )}
        <div dangerouslySetInnerHTML={{ __html: message.html }} />
      </div>
      {message.attachments.map((attachment, i) => (
        <div
          key={attachment.id}
          className={classNames('chat-body-box attachment', {
            'local-user': message.creator.id === user.id,
            last: last && i === message.attachments.length - 1,
          })}
        >
          <PreviewFileRow
            name={attachment.fileName}
            downloadUrl={getMessageAttachmentUrl(question.id, message.id, attachment.id, true)}
            viewUrl={getMessageAttachmentUrl(question.id, message.id, attachment.id)}
            getSignedUrl={getMessageAttachmentSignedUrl(question.id, message.id, attachment.id)}
          />
        </div>
      ))}
    </>
  );
};

export default QnaMessage;
