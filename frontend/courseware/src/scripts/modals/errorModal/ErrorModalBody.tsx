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
import React, { useState } from 'react';

/**
 * React port of modals/errorModal/errorModal.html (the `errorService` modal).
 * DOM preserved: `.error-modal` wrapper, `.modal-header.modal-error h3.modal-title`
 * with the warning icon, `.modal-body.csModal.modal-error` with the message and an
 * optional selectable `.handling-options` action list, and `.modal-footer.modal-error`
 * with an OK (`.btn.btn-primary`) and a Cancel (`.btn.btn-alert`, hidden when
 * `buttons.hideSecondaryButton`). OK resolves the selected action; Cancel rejects.
 */

export interface ErrorModalConfig {
  title: string;
  message: string;
  actions?: string[];
  buttons?: { hideSecondaryButton?: boolean };
}

export const ErrorModalBody: React.FC<
  ModalControls<string | undefined> & { error: ErrorModalConfig }
> = ({ close, dismiss, error }) => {
  const translate = useTranslation();
  const actions = error.actions ?? [];
  const [selected, setSelected] = useState<string | undefined>(actions[0]);
  return (
    <div className="error-modal">
      <div className="modal-header modal-error">
        <h3 className="modal-title">
          <span className="icon icon-warning" />
          {translate(error.title)}
        </h3>
      </div>
      <div className="modal-body csModal modal-error">
        <p>{translate(error.message)}</p>
        {actions.length > 0 && (
          <div className="handling-options">
            <ul>
              {actions.map(action => (
                <li
                  key={action}
                  className={selected === action ? 'selected-action' : undefined}
                  onClick={() => setSelected(action)}
                >
                  {action}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
      <div className="modal-footer modal-error">
        <button
          className="btn btn-primary"
          onClick={() => close(selected)}
        >
          {translate('OK')}
        </button>
        {!error.buttons?.hideSecondaryButton && (
          <button
            className="btn btn-alert"
            onClick={() => dismiss('cancel')}
          >
            {translate('Cancel')}
          </button>
        )}
      </div>
    </div>
  );
};
