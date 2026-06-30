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
import type { ModalControls } from './reactModalHost';
import React from 'react';
import { ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

/**
 * The React port of the `confirmModal` Angular component (directives/confirmModal).
 * DOM mirrors the old `confirm.html`: a `.modal-header` title, a `.modal-body`
 * message, and a `.modal-footer` whose **first** button confirms (close) and
 * **last** button cancels (dismiss) — the order the Selenide `ConfirmModal` page
 * object relies on (`footer.$("button:first-of-type")` / `:last-of-type`).
 *
 * Kept free of any store/host import so it is unit-testable in isolation.
 */

export interface ConfirmModalOptions {
  /** translation key (or already-translated text) for the body message */
  message: string;
  /** translation key for the confirm button; defaults to CONFIRM_MODAL_CONFIRM */
  confirmButton?: string;
  /** translation key for the cancel button; defaults to CONFIRM_MODAL_CANCEL */
  cancelButton?: string;
  /** translation key for the header; defaults to CONFIRM_MODAL_HEADER */
  header?: string;
  /** extra bootstrap class for the confirm button (e.g. 'btn-success'); defaults to 'btn-primary' */
  confirmClass?: string;
  /** extra bootstrap class for the cancel button (e.g. 'btn-danger'); defaults to 'btn-secondary' */
  cancelClass?: string;
}

export const ConfirmModalBody: React.FC<ModalControls<void> & { resolve: ConfirmModalOptions }> = ({
  close,
  dismiss,
  resolve,
}) => {
  const translate = useTranslation();
  return (
    <>
      <ModalHeader tag="h1">{translate(resolve.header || 'CONFIRM_MODAL_HEADER')}</ModalHeader>
      <ModalBody>
        <p>{translate(resolve.message)}</p>
      </ModalBody>
      <ModalFooter>
        <button
          id="confirm-modal-confirm"
          data-testid="confirm-modal-confirm"
          className={`btn ${resolve.confirmClass || 'btn-primary'}`}
          onClick={() => close()}
        >
          {translate(resolve.confirmButton || 'CONFIRM_MODAL_CONFIRM')}
        </button>
        <button
          id="confirm-modal-cancel"
          data-testid="confirm-modal-cancel"
          className={`btn ${resolve.cancelClass || 'btn-secondary'}`}
          onClick={() => dismiss('cancel')}
        >
          {translate(resolve.cancelButton || 'CONFIRM_MODAL_CANCEL')}
        </button>
      </ModalFooter>
    </>
  );
};
