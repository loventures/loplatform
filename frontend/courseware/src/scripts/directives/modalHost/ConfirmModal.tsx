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

import { ConfirmModalBody, ConfirmModalOptions } from './ConfirmModalBody';
import { openReactModal } from './reactModalHost';
import React from 'react';

export type { ConfirmModalOptions } from './ConfirmModalBody';

/**
 * Open a confirmation modal. Resolves when the user confirms, rejects (with
 * `'cancel'`) when they cancel or dismiss — the same promise contract the old
 * `$uibModal.open({ component: 'confirmModal' }).result` had.
 */
export function openConfirmModal(resolve: ConfirmModalOptions): Promise<void> {
  return openReactModal<void>(controls => (
    <ConfirmModalBody
      {...controls}
      resolve={resolve}
    />
  ));
}
