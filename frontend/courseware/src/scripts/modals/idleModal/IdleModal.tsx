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

import * as React from 'react';
import { ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { type ModalControls, openReactModal } from '../../directives/modalHost/reactModalHost.tsx';
import { fromNow } from '../../filters/pure/fromNow.ts';
import { useTranslation } from '../../i18n/translationContext.tsx';

const CYCLE_MS = 1000;

/**
 * React port of `modals/idleModal/idleModal.html` + `IdleModalCtrl`: the "session about to expire" warning.
 * `expiry` is the remaining milliseconds; we count down by a second each tick (as the old `$timeout` loop
 * did) and flip to the "expired" state at zero. `close(true)` = stay logged in, `close(false)` = log out /
 * return to login — matching the booleans `SessionManager.test` reads off the old `$uibModal` `.result`.
 */
export const IdleModalBody: React.FC<ModalControls<boolean> & { expiry: number }> = ({
  close,
  expiry,
}) => {
  const translate = useTranslation();
  const [remaining, setRemaining] = React.useState(expiry);

  React.useEffect(() => {
    const id = setInterval(() => setRemaining(r => r - CYCLE_MS), CYCLE_MS);
    return () => clearInterval(id);
  }, []);

  const expiring = remaining > 0;
  return (
    <>
      <ModalHeader tag="h3">
        {translate(expiring ? 'Session About To Expire' : 'Session Expired')}
      </ModalHeader>
      <ModalBody>
        {expiring ? (
          <p>{translate('sessionExpiringMessage', { expiresIn: fromNow(remaining, true, true) })}</p>
        ) : (
          <p>{translate('sessionExpiredMessage')}</p>
        )}
      </ModalBody>
      <ModalFooter>
        {expiring ? (
          <>
            <button
              className="btn btn-danger"
              onClick={() => close(false)}
            >
              {translate('Logout')}
            </button>
            <button
              className="btn btn-success"
              onClick={() => close(true)}
            >
              {translate('Stay logged in')}
            </button>
          </>
        ) : (
          <button
            className="btn btn-warning"
            onClick={() => close(false)}
          >
            {translate('Return to login page')}
          </button>
        )}
      </ModalFooter>
    </>
  );
};

/**
 * Opens the idle warning. Resolves `true` (stay) / `false` (log out); rejects if dismissed by backdrop/esc
 * — matching the old uib `$uibModal.open(...).result` the session keepalive consumes.
 */
export const openIdleModal = (expiry: number): Promise<boolean> =>
  openReactModal<boolean>(controls => <IdleModalBody {...controls} expiry={expiry} />, {
    backdrop: true,
    keyboard: true,
  });
