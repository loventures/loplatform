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
import { useDispatch } from 'react-redux';
import { Modal, ModalBody, ModalFooter } from 'reactstrap';

import { trackSessionExtension } from '../analytics/AnalyticsEvents';
import { useModal, usePolyglot } from '../hooks';
import { fetchUserSession } from '../user/userActions';

const SessionTimeoutModal = () => {
  const { modalOpen, toggleModal } = useModal();
  const polyglot = usePolyglot();
  const dispatch = useDispatch();

  const continueSession = () => {
    toggleModal();
    dispatch(fetchUserSession());
    trackSessionExtension();
  };

  return (
    <Modal isOpen={modalOpen}>
      <ModalBody className="p-4">
        <div>{polyglot.t('SESSION_TIMEOUT_MESSAGE')}</div>
      </ModalBody>
      <ModalFooter className="border-0">
        <div className="d-flex justify-content-end">
          <button
            onClick={continueSession}
            className="btn btn-primary"
          >
            {polyglot.t('CONTINUE_SESSION')}
          </button>
        </div>
      </ModalFooter>
    </Modal>
  );
};

export default SessionTimeoutModal;
