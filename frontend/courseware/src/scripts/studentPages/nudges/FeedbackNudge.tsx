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
  FeedbackNotification,
  useAlertViewedMutation,
} from '../../resources/AlertsResource';
import { useContentResource } from '../../resources/ContentsResource';
import { ContentPlayerPageLink } from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import React from 'react';
import { TbScribble } from 'react-icons/tb';
import { CardHeader } from 'reactstrap';

interface FeedbackNudgeProps {
  alert: AlertNotification<FeedbackNotification>;
}

const FeedbackNudge: React.FC<FeedbackNudgeProps> = ({ alert }) => {
  const translate = useTranslation();
  const content = useContentResource(alert.notification.topic ?? '');
  const disabled = !content?.id;

  const viewedMutation = useAlertViewedMutation(alert.id);

  return (
    <div className="flex-col card mb-4 feedback-nudge nudge-card">
      <CardHeader className="text-white nudge-header">
        {translate('FEEDBACK_NUDGE_HEADER', { title: content?.name ?? '' })}
      </CardHeader>
      <div className="d-flex">
        <div className="d-flex flex-grow-1">
          <div className="p-2 mx-2 d-flex justify-content-center align-items-center flex-shrink-0">
            <TbScribble
              size={24}
              strokeWidth={1}
            />
          </div>
          <div className="p-2 d-flex align-items-center">
            <div className="clamp-feedback">{alert.notification.message}</div>
          </div>
        </div>
        <div className="p-2 d-flex align-items-center">
          <LoLink
            className={classnames('btn btn-primary my-2 mx-3', { disabled })}
            style={{ textDecoration: 'none' }}
            to={disabled ? {} : ContentPlayerPageLink.toLink({ content: content })}
            onClick={() => viewedMutation.mutate()}
            disabled={disabled}
          >
            {translate('FEEDBACK_NUDGE_BUTTON')}
          </LoLink>
        </div>
      </div>
    </div>
  );
};

export default FeedbackNudge;
