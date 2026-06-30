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
import { ModalBody, ModalFooter } from 'reactstrap';

/**
 * React port of feedbackModal.html (the "ongoing media feedback" confirm). DOM
 * preserved: a `.modal-body` paragraph (translated with `{ type }` values) and a
 * `.modal-footer` with confirm (`.lo-btn`, resolves `true`) and cancel (`.lo-btn`,
 * rejects). Mirrors the original `confirm()` → close / `cancel()` → dismiss.
 */

export const FeedbackModalBody: React.FC<ModalControls<boolean> & { type: string }> = ({
  close,
  dismiss,
  type,
}) => {
  const translate = useTranslation();
  return (
    <>
      <ModalBody>
        <p>{translate('MEDIA_FEEDBACK_ONGOING_FEEDBACK', { type })}</p>
      </ModalBody>
      <ModalFooter>
        <button
          className="lo-btn"
          type="button"
          onClick={() => close(true)}
        >
          {translate('DIALOG_CONFIRM')}
        </button>
        <button
          className="lo-btn"
          type="button"
          onClick={() => dismiss('cancel')}
        >
          {translate('DIALOG_CANCEL')}
        </button>
      </ModalFooter>
    </>
  );
};
