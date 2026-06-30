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

import axios from 'axios';
import classnames from 'classnames';
import React from 'react';
import { useDispatch } from 'react-redux';
import { Alert, Button } from 'reactstrap';

import { Announcement, disableAnnouncement } from '../redux/actions/AnnouncementActions';
import { useLoPlatform, useTypedSelector } from '../redux/state';
import { IoAlertCircleOutline } from 'react-icons/io5';

const AnnouncementBar: React.FC = () => {
  const dispatch = useDispatch();
  const announcements: Announcement[] = useTypedSelector(
    state => state.announcement?.announcements ?? []
  );
  const { isProdLike } = useLoPlatform();

  const dangerouslyCreateMarkup = (html: string) => ({ __html: html });

  const isActive = (ann: Announcement) => new Date(ann.endTime ?? 0) > new Date();

  const disable = (annId: number) => {
    dispatch(disableAnnouncement(annId));
    axios
      .post('/api/v2/announcements/hide', { announcementId: annId })
      .then(res => res)
      .catch(err => console.log(err));
  };

  const alertCls = (style?: string) =>
    classnames('announcement-alert-inner-html', {
      'html1-blink-slow': !isProdLike && style === 'warning',
      'html1-blink-fast': !isProdLike && style === 'danger',
    });

  const AlertContentComponent: React.FC<React.HTMLAttributes<HTMLElement>> = props =>
    isProdLike ? <span {...props} /> : <marquee {...props} />;

  return announcements.length ? (
    <div id="announcements">
      {announcements.map(ann => (
        <Alert
          key={ann.id}
          color={ann.style}
          isOpen={isActive(ann)}
          className="d-flex p-2 pe-2 align-items-start"
        >
          <div className={`flex-grow-1 ${isProdLike ? '' : 'd-flex align-items-center'}`}>
            <IoAlertCircleOutline
              className="me-2"
              style={{ verticalAlign: '-2px' }}
            />
            <AlertContentComponent
              className={alertCls(ann.style)}
              dangerouslySetInnerHTML={dangerouslyCreateMarkup(ann.message)}
            />
          </div>
          <Button
            color="close"
            className="ms-2"
            aria-label="Close"
            onClick={() => disable(ann.id)}
          />
        </Alert>
      ))}
    </div>
  ) : null;
};

export default AnnouncementBar;
