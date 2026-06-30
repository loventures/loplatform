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

import { openReactModal } from '../../directives/modalHost/reactModalHost';
import { ErrorModalBody, ErrorModalConfig } from './ErrorModalBody';
import React from 'react';

export type { ErrorModalConfig } from './ErrorModalBody';

/** Resolves with the selected action on OK; rejects on Cancel/dismiss. */
export function openErrorModal(config: ErrorModalConfig): Promise<string | undefined> {
  return openReactModal<string | undefined>(controls => (
    <ErrorModalBody
      {...controls}
      error={config}
    />
  ));
}
