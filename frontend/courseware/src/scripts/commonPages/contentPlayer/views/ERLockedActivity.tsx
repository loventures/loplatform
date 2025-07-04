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

import ERContentTitle from '../../../commonPages/contentPlayer/ERContentTitle';
import { ERActivityProps } from '../../../commonPages/contentPlayer/views/ERActivity';
import ContentAvailabilityMessage from '../../../contentPlayerComponents/parts/ContentAvailabilityMessage';
import { useTranslation } from '../../../i18n/translationContext';
import React, { useEffect } from 'react';
import { IoLockClosedOutline } from 'react-icons/io5';

const ERLockedActivity: React.FC<ERActivityProps> = ({
  content,
  viewingAs,
  onLoaded,
  printView,
}) => {
  const translate = useTranslation();
  useEffect(() => onLoaded?.(), [onLoaded]);

  return (
    <div className="card er-content-wrapper">
      <div className="card-body">
        <ERContentTitle
          content={content}
          printView={printView}
        />
        <div className="er-expandable-activity">
          <div className="d-flex flex-column-center">
            <IoLockClosedOutline
              size="10rem"
              aria-hidden
              className="d-print-none"
            />

            <h2 className="locked-page-title h4 mt-4">{translate('CONTENT_LOCKED')}</h2>
          </div>

          <div className="mt-4 locked card">
            <div className="card-body">
              <ContentAvailabilityMessage
                content={content}
                viewingAs={viewingAs}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ERLockedActivity;
