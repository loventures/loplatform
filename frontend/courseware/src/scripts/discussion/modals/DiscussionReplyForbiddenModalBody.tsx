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
 * React port of discussionReplyForbiddenModal.html. Shown when a reply edit is
 * UNAUTHORIZED; the user chooses to keep working (resolve `true`) or discard
 * (resolve `false`). DOM mirrors the original: `.modal-header.modal-info` with
 * an `h3.modal-title`, a `.modal-body` paragraph, and a `.modal-footer` with the
 * keep button (`.lo-btn.lo-btn-primary`) first and discard (`.lo-btn.lo-btn-text-light`).
 */

export interface ForbiddenModalConfig {
  title: string;
  description: string;
}

const KEEP_KEY = 'DISCUSSION_POST_EDIT_FORBIDDEN_KEEP_WORK';
const DISCARD_KEY = 'DISCUSSION_POST_EDIT_FORBIDDEN_DISCARD_WORK';

export const DiscussionReplyForbiddenModalBody: React.FC<
  ModalControls<boolean> & { config: ForbiddenModalConfig }
> = ({ close, config }) => {
  const translate = useTranslation();
  return (
    <>
      <ModalHeader
        tag="h3"
        className="modal-info"
      >
        {translate(config.title)}
      </ModalHeader>
      <ModalBody>
        <p>{translate(config.description)}</p>
      </ModalBody>
      <ModalFooter>
        <button
          className="lo-btn lo-btn-primary"
          title={translate(KEEP_KEY)}
          onClick={() => close(true)}
        >
          {translate(KEEP_KEY)}
        </button>
        <button
          className="lo-btn lo-btn-text-light"
          title={translate(DISCARD_KEY)}
          onClick={() => close(false)}
        >
          {translate(DISCARD_KEY)}
        </button>
      </ModalFooter>
    </>
  );
};
