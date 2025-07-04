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
import Course from '../bootstrap/course';
import { useCourseSelector } from '../loRedux';
import dayjs from 'dayjs';
import { useTranslation } from '../i18n/translationContext';
import { selectActualUser } from '../utilities/rootSelectors';
import React, { useState } from 'react';

const ReviewPeriodBanner: React.FC = () => {
  const translate = useTranslation();
  const [status, setStatus] = useState({ truncate: true });
  const actualUser = useCourseSelector(selectActualUser);

  const showBanner = Course.endDate && actualUser.isStudent && dayjs().isAfter(Course.endDate);
  const shutdownDate = Course.shutdownDate;

  // TODO: fix the a11y errors from making a div clickable.
  return showBanner ? (
    <div className="review-period-banner">
      {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events,jsx-a11y/no-static-element-interactions */}
      <div
        className={classnames({ 'truncate-content': status.truncate })}
        onClick={() => setStatus({ truncate: !status.truncate })}
      >
        <div
          className="alert alert-warning mb-0"
          style={{ borderRadius: 0, paddingLeft: '4rem' }}
        >
          <span className="icon icon-warning"></span>
          <span>{translate('REVIEW_PERIOD_WARNING', { shutdownDate: shutdownDate })}</span>
        </div>
      </div>
    </div>
  ) : null;
};

export default ReviewPeriodBanner;
