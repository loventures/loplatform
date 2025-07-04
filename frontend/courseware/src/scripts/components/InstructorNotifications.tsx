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

import { useInstructorNotificationsResource } from '../resources/InstructorNotificationsResource';
import { useCourseSelector } from '../loRedux';
import { map } from 'lodash';
import { useTranslation } from '../i18n/translationContext';
import { selectActualUser } from '../utilities/rootSelectors';
import React, { Suspense } from 'react';

type InstructorNotificationProps = { contentId: string };

const InstructorNotifications: React.FC<InstructorNotificationProps> = ({ contentId }) => {
  const translate = useTranslation();
  const actualUser = useCourseSelector(selectActualUser);
  const notifications = useInstructorNotificationsResource(actualUser, contentId);
  if (actualUser.isInstructor || actualUser.isPreviewing) {
    return null;
  }
  return notifications ? (
    <div>
      {map(notifications, notification => (
        <section
          className="alert alert-primary"
          key={notification.id}
        >
          <h1 className="h4 alert-heading">{translate('INSTRUCTOR_NOTIFICATION')}</h1>
          <div>{notification.message}</div>
        </section>
      ))}
    </div>
  ) : null;
};

export default (props: InstructorNotificationProps) => (
  <Suspense fallback={null}>
    <InstructorNotifications {...props} />
  </Suspense>
);
