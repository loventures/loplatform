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
import LoLink from '../../components/links/LoLink';
import {
  AlertNotification,
  QnaNotification,
  useAlertViewedMutation,
} from '../../resources/AlertsResource';
import { useContentResource } from '../../resources/ContentsResource';
import { COURSE_ROOT } from '../../utilities/courseRootType';
import { ContentPlayerPageLink, StudentQnaQuestionLink } from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import React from 'react';
import { IoCloseOutline } from 'react-icons/io5';
import { PiSealQuestionLight } from 'react-icons/pi';

interface QnaNudgeProps {
  alert: AlertNotification<QnaNotification>;
}

const QnaNudge: React.FC<QnaNudgeProps> = ({ alert }) => {
  const translate = useTranslation();

  const { count, id: alertId, notification } = alert;
  const { edgePath } = notification;

  const viewedMutation = useAlertViewedMutation(alertId);
  const content = useContentResource(edgePath);
  const disabled = !content?.id;

  const toLink =
    content.id === COURSE_ROOT
      ? StudentQnaQuestionLink.toLink(notification.questionId)
      : ContentPlayerPageLink.toLink({ content, qna: true });

  return (
    <div className="flex-column flex-sm-row align-items-center align-items-sm-stretch card mb-4 nudge-card">
      <button
        className="d-none d-sm-block p-2 close-notification"
        onClick={() => viewedMutation.mutate()}
        aria-label={translate('CLOSE_NUDGE')}
      >
        <IoCloseOutline size={24} />
      </button>

      <div className="d-flex flex-grow-1 align-items-center pt-3 pt-sm-0 px-4 px-sm-2">
        <PiSealQuestionLight
          size={20}
          className="d-none d-sm-inline mx-2 me-3 chat-icon flex-shrink-0"
        />
        <span>{translate('QNA_NUDGE_CONTENT', { count, title: content?.name ?? '' })}</span>
      </div>
      <div className="d-flex align-items-center py-2 px-3 align-self-stretch align-self-sm-center">
        <LoLink
          className={classnames('btn btn-primary my-2 me-0 me-sm-3 w-100', { disabled })}
          style={{ textDecoration: 'none' }}
          to={disabled ? {} : toLink}
          onClick={() => viewedMutation.mutate()}
          disabled={disabled}
        >
          {translate('QNA_NUDGE_BUTTON')}
        </LoLink>
      </div>
    </div>
  );
};

export default QnaNudge;
