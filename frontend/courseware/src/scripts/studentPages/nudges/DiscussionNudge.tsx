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
  PostNotification,
  useAlertViewedMutation,
} from '../../resources/AlertsResource';
import { useContentResource } from '../../resources/ContentsResource';
import { ContentPlayerPageLink } from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import React from 'react';
import { IoChatbubbleOutline, IoCloseOutline } from 'react-icons/io5';

interface DiscussionNudgeProps {
  alert: AlertNotification<PostNotification>;
}

const DiscussionNudge: React.FC<DiscussionNudgeProps> = ({ alert }) => {
  const { count, id: alertId, notification } = alert;
  const { title, edgePath } = notification;
  const translate = useTranslation();

  const discussion = useContentResource(edgePath);
  const disabled = !discussion?.id;

  const viewedMutation = useAlertViewedMutation(alertId);

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
        <IoChatbubbleOutline
          size={20}
          className="d-none d-sm-inline mx-2 me-3 chat-icon flex-shrink-0"
        />
        <span>{translate('DISCUSSION_NUDGE_CONTENT', { count, title })}</span>
      </div>
      <div className="d-flex align-items-center py-2 px-3 align-self-stretch align-self-sm-center">
        <LoLink
          className={classnames('btn btn-primary my-2 me-0 me-sm-3 w-100', { disabled })}
          style={{ textDecoration: 'none' }}
          to={disabled ? {} : ContentPlayerPageLink.toLink({ content: discussion })}
          onClick={() => viewedMutation.mutate()}
          disabled={disabled}
        >
          {translate('DISCUSSION_NUDGE_BUTTON')}
        </LoLink>
      </div>
    </div>
  );
};

export default DiscussionNudge;
