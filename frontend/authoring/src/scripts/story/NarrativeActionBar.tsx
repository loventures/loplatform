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

import React from 'react';

import { useDcmSelector } from '../hooks';
import Presence from '../presence/Presence';
import { ActionButtons } from './NarrativeActionBar/ActionButtons';
import { NarrativeBreadCrumbs } from './NarrativeActionBar/NarrativeBreadCrumbs';
import { QuillMenu } from './NarrativeActionBar/QuillMenu';

export const NarrativeActionBar: React.FC<{ stuck: boolean }> = ({ stuck }) => {
  const presenceEnabled = useDcmSelector(s => s.configuration.presenceEnabled);
  return (
    <div className="narrative-action-bar d-flex align-items-stretch justify-content-between h-100 px-3">
      <h6 className="m-0 text-nowrap flex-shrink-1 me-3 d-flex align-items-center justify-content-end minw-0">
        <QuillMenu />
        <NarrativeBreadCrumbs />
      </h6>
      <div className="d-flex align-items-stretch">
        {presenceEnabled && <Presence compact />}
        <ActionButtons stuck={stuck} />
      </div>
    </div>
  );
};
