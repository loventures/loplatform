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

import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';

import Course from '../../bootstrap/course.ts';
import { openReactModal } from '../../directives/modalHost/reactModalHost.tsx';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { presenceService } from '../../presence/presenceServiceImpl.ts';
import { sessionEvents } from '../../services/sessionEvents.ts';
import { appIsFramed } from '../../utilities/deviceType.js';
import { disconnectGate } from '../../utilities/pure/disconnect.ts';
import { DisconnectAlertModalBody } from './DisconnectAlertModalBody.tsx';

interface Messages {
  banner: string;
  modal: { title: string; message: string; dismiss: string };
}

const maintenanceMessages: Messages = {
  banner: 'MAINTENANCE_MODE_STARTED_MESSAGE',
  modal: {
    title: 'MAINTENANCE_MODE_MODAL_TITLE',
    message: 'MAINTENANCE_MODE_MODAL_MESSAGE',
    dismiss: 'MAINTENANCE_MODE_MODAL_DISMISS_BUTTON',
  },
};
const maintenanceWithEndMessages: Messages = {
  ...maintenanceMessages,
  modal: { ...maintenanceMessages.modal, message: 'MAINTENANCE_MODE_MODAL_MESSAGE_WITH_END' },
};
const loggedoutMessages: Messages = {
  banner: 'LOGGED_OUT_BANNER_MESSAGE',
  modal: {
    title: 'LOGGED_OUT_MODAL_TITLE',
    message: 'LOGGED_OUT_MODAL_MESSAGE',
    dismiss: 'LOGGED_OUT_MODAL_DISMISS_BUTTON',
  },
};
const loggedinMessages: Messages = {
  banner: 'LOGGED_IN_BANNER_MESSAGE',
  modal: {
    title: 'LOGGED_IN_MODAL_TITLE',
    message: 'LOGGED_IN_MODAL_MESSAGE',
    dismiss: 'LOGGED_IN_MODAL_DISMISS_BUTTON',
  },
};
const transferredOutMessages: Messages = {
  banner: 'TRANSFERRED_OUT_BANNER_MESSAGE',
  modal: {
    title: 'TRANSFERRED_OUT_MODAL_TITLE',
    message: 'TRANSFERRED_OUT_MODAL_MESSAGE',
    dismiss: 'TRANSFERRED_OUT_MODAL_DISMISS_BUTTON',
  },
};

/**
 * React port of the `maintenanceModeWarning` directive (B2). Subscribes to the
 * PresenceService System / LearnerTransfer channels; on a maintenance/logout/login/
 * transfer event it opens the (already-React) DisconnectAlertModalBody via the B0
 * modal host, then on dismiss disconnects and shows the danger banner. The jQuery
 * `trackBannerHeight` directive becomes a ResizeObserver that sizes the
 * `.content-top-margin` spacer. Previously angular2react; now native React (its only
 * renderer is ERPageHeaderContainer) — DOM preserved (`.system-banner`,
 * `.maintenance-mode-banner`, `.content-top-margin`).
 */
export const MaintenanceModeWarning: React.FC = () => {
  const translate = useTranslation();
  const [showBanner, setShowBanner] = useState(false);
  const [messages, setMessages] = useState<Messages | null>(null);
  const [marginHeight, setMarginHeight] = useState(0);
  const bannerRef = useRef<HTMLDivElement>(null);

  const openModal = (msgs: Messages, data?: any) => {
    setMessages(msgs);
    openReactModal(
      controls => (
        <DisconnectAlertModalBody
          messages={msgs}
          messageData={data}
          close={controls.close}
        />
      ),
      { backdrop: 'static', keyboard: false }
    ).then(() => {
      disconnectGate.disable();
      setShowBanner(true);
    });
  };

  useEffect(() => {
    const PresenceService = presenceService;

    const onSystemEvent = (ev: any) => {
      if (ev.type === 'Maintenance') {
        openModal(ev.end ? maintenanceWithEndMessages : maintenanceMessages, ev);
      } else if (ev.type === 'Logout') {
        sessionEvents.emit('logout', undefined);
        openModal(loggedoutMessages);
      } else if (ev.type === 'Login') {
        sessionEvents.emit('logout', undefined);
        openModal(loggedinMessages);
      }
    };
    const onLearnerTransferEvent = (ev: any) => {
      if (Course.id === ev.sourceCourseId) openModal(transferredOutMessages);
    };

    const deregisterSystem = PresenceService.on('System', onSystemEvent);
    const deregisterTransfer = PresenceService.on('LearnerTransferMessage', onLearnerTransferEvent);
    return () => {
      deregisterSystem?.();
      deregisterTransfer?.();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Size the content spacer to the banner height (the old trackBannerHeight directive).
  useLayoutEffect(() => {
    const banner = bannerRef.current;
    if (!showBanner || !banner) {
      setMarginHeight(0);
      return;
    }
    const update = () => setMarginHeight(appIsFramed ? 0 : banner.offsetHeight);
    update();
    const observer = new ResizeObserver(update);
    observer.observe(banner);
    window.addEventListener('resize', update);
    return () => {
      observer.disconnect();
      window.removeEventListener('resize', update);
    };
  }, [showBanner]);

  return (
    <>
      <div
        className="content-top-margin"
        style={{ height: marginHeight }}
      />
      {showBanner && messages && (
        <div className="system-banner">
          <div
            ref={bannerRef}
            className="alert-danger banner-body maintenance-mode-banner"
          >
            <span className="message">{translate(messages.banner)}</span>
          </div>
        </div>
      )}
    </>
  );
};

