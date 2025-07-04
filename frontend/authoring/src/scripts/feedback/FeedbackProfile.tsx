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

import * as React from 'react';
import { useState } from 'react';

import { FeedbackProfileDto, profileImage } from './FeedbackApi';

export const profileColor = (profile: { id: number }) =>
  'hsl(' + (((profile.id || 0) * 47) % 360) + ', 50%, 40%)';

const FeedbackProfile: React.FC<{ profile: FeedbackProfileDto; className?: string }> = ({
  profile,
  className,
}) => {
  const [brokenImage, setBrokenImage] = useState(false);
  const letter = (profile.givenName || profile.fullName || '?').charAt(0);
  const color = profileColor(profile);
  const imageUrl = profileImage(profile);

  return (
    <div className={`present-user position-relative flex-grow-0 ${className}`}>
      <div
        className="present-user-circle"
        aria-label={profile.fullName}
      >
        <div
          className="present-user-photo"
          style={{ backgroundColor: color }}
          role="presentation"
        >
          {imageUrl && !brokenImage ? (
            <img
              className="user-photo"
              alt=""
              src={imageUrl}
              onError={() => setBrokenImage(true)}
            />
          ) : (
            <div className="user-letter">{letter}</div>
          )}
          <div className="user-mask"></div>
        </div>
      </div>
    </div>
  );
};

export default FeedbackProfile;
