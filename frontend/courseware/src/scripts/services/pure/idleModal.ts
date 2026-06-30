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

import { openErrorModal } from '../../modals/errorModal/errorModal.tsx';
import { openIdleModal } from '../../modals/idleModal/IdleModal.tsx';

/**
 * Pure-TS port of the AngularJS `IdleModal` factory: bridges the React `openIdleModal` / `openErrorModal`
 * modals (which already return native Promises) into the `.open(expiry)` / `.loggedOutError(cb)` contract
 * the keepalive `makeSessionManager` consumes. The only change from the Angular factory is `$q.reject()` →
 * `Promise.reject()`; the React modals are already Promise-based, so no other rewiring is needed. The thin
 * Angular `.factory('IdleModal', () => idleModal)` adapter in `SessionService.js` re-exports this singleton.
 */

const IdleModal: any = {
  modalIsOpen: false,
};

IdleModal.open = function (expiry: number): Promise<boolean> {
  if (IdleModal.modalIsOpen) {
    return Promise.reject();
  }

  IdleModal.modalIsOpen = true;

  // The React `openIdleModal` resolves true (stay) / false (log out) and rejects on backdrop/esc
  // dismiss — the same contract the old `$uibModal.open(...).result` had. (modalIsOpen is left set,
  // exactly as the original did — only loggedOutError resets it.)
  return openIdleModal(expiry);
};

IdleModal.loggedOutError = function (logoutAction: () => void) {
  // The React error modal: OK resolves (run the logout action), Cancel rejects (just dismiss).
  const modal = openErrorModal({
    title: 'LogoutErrorModal',
    message: 'LogoutErrorModalReason',
  });
  modal.then(
    function () {
      logoutAction();
      IdleModal.modalIsOpen = false;
    },
    function () {
      IdleModal.modalIsOpen = false;
    }
  );
  return modal;
};

export const idleModal = IdleModal;

export default idleModal;
