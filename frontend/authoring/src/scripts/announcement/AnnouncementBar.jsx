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

import gretchen from '../grfetchen/';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Alert, Button } from 'reactstrap';

import { disableAnnouncement } from './AnnouncementActions';
import { IoAlertCircleOutline } from 'react-icons/io5';

const AnnouncementBar = ({ announcements, dispatch }) => {
  const isActive = ann => new Date(ann.endTime) > new Date();

  const onDisableAnnouncement = annId => {
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
        .sort((a, b) => new Date(b.startTime) - new Date(a.startTime))
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

AnnouncementBar.propTypes = {
  announcements: PropTypes.array,
  dispatch: PropTypes.func.isRequired,
};

const mapStateToProps = state => ({
  announcements: state.announcement.announcements,
});

export default connect(mapStateToProps)(AnnouncementBar);
