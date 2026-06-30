/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import gretchen from '../grfetchen/';
import React from 'react';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';
import { Alert, Button } from 'reactstrap';

import { Announcement, disableAnnouncement } from './AnnouncementActions';
import { IoAlertCircleOutline } from 'react-icons/io5';

interface AnnouncementBarProps {
  announcements: Announcement[];
  dispatch: Dispatch;
}

const AnnouncementBar = ({ announcements, dispatch }: AnnouncementBarProps) => {
  const isActive = (ann: Announcement) => new Date(ann.endTime) > new Date();

  const onDisableAnnouncement = (annId: number) => {
    dispatch(disableAnnouncement(annId));
    gretchen
      .post('/api/v2/announcements/hide')
      .data({ announcementId: annId })
      .exec()
      .then(res => res)
      .catch(err => console.log(err));
  };

  return (
    <div
      id="announcements"
      className="my-0"
    >
      {announcements
        .sort((a, b) => +new Date(b.startTime) - +new Date(a.startTime))
        .map(ann => (
          <Alert
            key={ann.id}
            color={ann.style}
            isOpen={isActive(ann)}
          >
            <div className="flex-grow-1">
              <IoAlertCircleOutline
                className="me-2"
                style={{ verticalAlign: '-2px' }}
              />
              <span dangerouslySetInnerHTML={{ __html: ann.message }} />
            </div>
            <Button
              color="close"
              aria-label="Close"
              onClick={() => onDisableAnnouncement(ann.id)}
            />
          </Alert>
        ))}
    </div>
  );
};



const mapStateToProps = (state: any) => ({
  announcements: state.announcement.announcements,
});

export default connect(mapStateToProps)(AnnouncementBar);
