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

import { Announcement, getActiveAnnouncements, hideAnnouncement } from '../../api/announcementsApi';
import {
  PresenceEvent,
  isAnnouncementEndEvent,
  isAnnouncementEvent,
} from '../../events/eventsReducer';
import { map, reject } from 'lodash';
import { useTranslation } from '../../i18n/translationContext';
import React, { useCallback, useContext, useEffect, useState } from 'react';
import { IoAlertCircleOutline, IoClose } from 'react-icons/io5';
import { Alert } from 'reactstrap';
import { lojector } from '../../loject';

type AnnouncementsContext = {
  announcements: Announcement[];
  dismissAnnouncement: (id: number) => void;
};

export const AnnouncementsContext = React.createContext<AnnouncementsContext>({
  announcements: [],
  dismissAnnouncement: () => void 0,
});

export const AnnouncementsProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
  const [announcements, setAnnouncements] = useState<Announcement[]>([]);

  useEffect(() => {
    getActiveAnnouncements().then(announcements => setAnnouncements(announcements));

    const PresenceService = lojector.get<any>('PresenceService');
    return PresenceService.on('Control', (event: PresenceEvent) => {
      if (isAnnouncementEvent(event)) {
        const { type, ...newAnnouncement } = event;
        setAnnouncements(existingAnnouncements => [...existingAnnouncements, newAnnouncement]);
      }
      if (isAnnouncementEndEvent(event)) {
        setAnnouncements(existingAnnouncements => reject(existingAnnouncements, { id: event.id }));
      }
    });
  }, []);

  const dismissAnnouncement = useCallback(
    (id: number) => {
      hideAnnouncement(id).then(() => {
        setAnnouncements(reject(announcements, { id }));
      });
    },
    [announcements]
  );

  return (
    <AnnouncementsContext.Provider value={{ announcements, dismissAnnouncement }}>
      {children}
    </AnnouncementsContext.Provider>
  );
};

const Announcements: React.FC = () => {
  const { announcements, dismissAnnouncement } = useContext(AnnouncementsContext);
  const translate = useTranslation();
  return (
    <div id="announcements">
      {map(announcements, announcement => (
        <Alert
          fade={false}
          className="m-0 alert-dismissible"
          key={announcement.id}
          id={`announcements-${announcement.id}` as string}
          color={announcement.style}
        >
          <button
            className="btn-close"
            onClick={() => dismissAnnouncement(announcement.id)}
            title={translate('CLOSE')}
            aria-label={translate('CLOSE')}
          >
          </button>
          <IoAlertCircleOutline
            className="me-2"
            style={{ verticalAlign: '-2px' }}
          />
          <span
            className="alert-inner-html"
            dangerouslySetInnerHTML={{ __html: announcement.message }}
          />
        </Alert>
      ))}
    </div>
  );
};

export default Announcements;
