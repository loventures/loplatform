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

import { Content } from '../../api/contentsApi';
import { percentFilter } from '../../filters/percent';
import { useTranslation } from '../../i18n/translationContext';
import React from 'react';
import { FiBookmark, FiLock } from 'react-icons/fi';

type ERNavItemIconProps = {
  content: Content;
  locked?: boolean;
  enter?: () => void;
  leave?: () => void;
  bookmarked?: boolean;
};

const ERNavItemIcon: React.FC<ERNavItemIconProps> = ({
  content,
  locked,
  bookmarked,
  enter,
  leave,
}) => {
  const translate = useTranslation();

  const progress = content.progress;
  const normalizedProgress = (progress?.weightedPercentage ?? 0) / 100;
  const complete = progress && progress.total > 0 && progress?.total === progress?.completions;
  const circumference = 2 * Math.PI * 5;
  const strokeDash = Math.round(normalizedProgress * circumference);
  const strokeDashArray = `${strokeDash} ${circumference}`;

  return (
    <div
      onMouseEnter={enter}
      onMouseLeave={leave}
      onFocus={enter}
      onBlur={leave}
      style={{ marginLeft: '.25rem', marginRight: '-.75rem', display: 'flex' }}
      role="none"
      className={
        locked
          ? 'activity-locked'
          : bookmarked
            ? 'activity-bookmarked'
            : complete
              ? 'activity-complete'
              : normalizedProgress > 0
                ? 'activity-progress'
                : undefined
      }
    >
      {locked ? (
        <FiLock
          size="1rem"
          strokeWidth={2}
          title={translate('ER_ACTIVITY_LOCKED')}
          aria-hidden={true}
        />
      ) : complete && !bookmarked ? (
        <svg
          stroke="currentColor"
          fill="currentColor"
          strokeWidth={0}
          viewBox="0 0 48 48"
          aria-hidden={true}
          height="1rem"
          width="1rem"
          xmlns="http://www.w3.org/2000/svg"
          style={{ minWidth: '1rem' }}
        >
          <title>{translate('ER_ACTIVITY_COMPLETED')}</title>
          <polygon points="40.6,12.1 17,35.7 7.4,26.1 4.6,29 17,41.3 43.4,14.9"></polygon>
        </svg>
      ) : normalizedProgress > 0 && !complete ? (
        <span
          className="d-flex"
          style={{ height: '1rem', width: '1rem', marginRight: bookmarked ? '2px' : 0 }}
          title={translate('ER_ACTIVITY_COMPLETION', {
            progress: percentFilter(normalizedProgress),
          })}
        >
          <svg
            height="1rem"
            width="1rem"
            viewBox="0 0 20 20"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden={true}
          >
            <circle
              r="10"
              cx="10"
              cy="10"
              fill="#E1E2E2"
              stroke="initial"
            ></circle>
            <circle
              r="5"
              cx="10"
              cy="10"
              fill="#E1E2E2"
              stroke="#509DD0"
              strokeWidth="10"
              strokeDasharray={strokeDashArray}
              transform="rotate(-90) translate(-20)"
            ></circle>
          </svg>
        </span>
      ) : !bookmarked ? (
        <span
          className="d-inline-block"
          style={{ height: '1rem', width: '1rem' }}
        />
      ) : null}
      {bookmarked && (
        <FiBookmark
          size="1rem"
          strokeWidth={2}
          fill="#0c6e9e80"
          title={translate('ER_ACTIVITY_BOOKMARKED')}
          aria-hidden={true}
        />
      )}
    </div>
  );
};

export default ERNavItemIcon;
