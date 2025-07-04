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
import PreviewFileRow from '../../components/fileViews/PreviewFileRow';
import LoLink from '../../components/links/LoLink';
import {
  AlertNotification,
  InstructorMessageSentNotification,
  useAlertViewedMutation,
} from '../../resources/AlertsResource';
import { getMessageAttachmentSignedUrl, getMessageAttachmentUrl } from '../../qna/qnaApi';
import { StudentQnaQuestionLink } from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import React from 'react';
import { IoCloseOutline } from 'react-icons/io5';
import { Button } from 'reactstrap';

interface InstructorMessageNudgeProps {
  alert: AlertNotification<InstructorMessageSentNotification>;
}

const InstructorMessageNudge: React.FC<InstructorMessageNudgeProps> = ({ alert }) => {
  const translate = useTranslation();

  const { id: alertId, notification } = alert;
  const viewedMutation = useAlertViewedMutation(alertId);

  return (
    <div className="flex-row flex-sm-row align-items-center align-items-sm-stretch card mb-4 nudge-card message-nudge">
      <button
        className="d-none d-sm-block p-2 close-notification"
        onClick={() => viewedMutation.mutate()}
        aria-label={translate('CLOSE_NUDGE')}
      >
        <IoCloseOutline size={24} />
      </button>

      <div className="py-2 px-3 w-100">
        <h4 className="my-2">
          {notification.subject ?? translate('QNA_INSTRUCTOR_MESSAGE_PLACEHOLDER_SUBJECT')}
        </h4>

        <div dangerouslySetInnerHTML={{ __html: notification.body }} />

        {notification.attachments.map(attachment => (
          <div className="my-2 me-3">
            <PreviewFileRow
              key={attachment.id}
              name={attachment.fileName}
              downloadUrl={getMessageAttachmentUrl(
                notification.questionId,
                notification.messageId,
                attachment.id,
                true
              )}
              viewUrl={getMessageAttachmentUrl(
                notification.questionId,
                notification.messageId,
                attachment.id
              )}
              getSignedUrl={getMessageAttachmentSignedUrl(
                notification.questionId,
                notification.messageId,
                attachment.id
              )}
            />
          </div>
        ))}

        <div className="d-flex justify-content-end align-items-center align-self-stretch align-self-sm-center gap-3 nudge-buttons">
          <LoLink
            className={classnames('btn btn-secondary my-2 me-0 me-sm-3')}
            style={{ textDecoration: 'none' }}
            to={StudentQnaQuestionLink.toLink(notification.questionId)}
            onClick={() => viewedMutation.mutate()}
          >
            {translate('QNA_INSTRUCTOR_MESSAGE_RESPOND')}
          </LoLink>
          <Button
            color="primary"
            className={classnames('my-2 me-0 me-sm-3')}
            onClick={() => viewedMutation.mutate()}
            style={{}}
          >
            {translate('QNA_INSTRUCTOR_MESSAGE_CLOSE')}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default InstructorMessageNudge;
