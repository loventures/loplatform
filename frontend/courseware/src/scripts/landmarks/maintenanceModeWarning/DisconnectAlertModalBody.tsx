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

import { useTranslation } from '../../i18n/translationContext';
import type { ModalControls } from '../../directives/modalHost/reactModalHost';
import React from 'react';
import { ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

/**
 * React port of disconnectAlertModal.html (maintenance / logout / transfer
 * disconnect alert). DOM preserved for the Selenide `SiteMaintenanceModal` page
 * object: reactstrap supplies `.modal-content`; the header is `.modal-header h2`,
 * the body `.modal-body p`, and the dismiss button is the sole `.modal-footer`
 * button (Selenide selects `.modal-footer button`). Clicking it resolves the
 * modal (the old `$close()`), which the caller follows with disconnect + banner.
 */

export interface DisconnectMessages {
  modal: { title: string; message: string; dismiss: string };
}

export const DisconnectAlertModalBody: React.FC<
  Pick<ModalControls, 'close'> & { messages: DisconnectMessages; messageData?: any }
> = ({ close, messages, messageData }) => {
  const translate = useTranslation();
  return (
    <>
      <ModalHeader tag="h2">{translate(messages.modal.title)}</ModalHeader>
      <ModalBody>
        <p>{translate(messages.modal.message, messageData)}</p>
      </ModalBody>
      <ModalFooter>
        <button
          className="lo-btn lo-btn-warning"
          type="button"
          onClick={() => close()}
        >
          {translate(messages.modal.dismiss)}
        </button>
      </ModalFooter>
    </>
  );
};
