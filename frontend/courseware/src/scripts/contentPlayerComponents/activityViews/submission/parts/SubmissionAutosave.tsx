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

import Autosaver from '../../../../directives/Autosaver';
import React from 'react';

const delayAfterSave = 3 * 60 * 1000;
const delayAfterUpdate = 30 * 1000;

const SubmissionAutosave: React.FC<{
  lastSaved: string | null;
  lastUpdated?: number;
  autosaveAction: () => void;
}> = ({ lastSaved, lastUpdated, autosaveAction }) => (
  <Autosaver
    delayAfterSave={delayAfterSave}
    delayAfterUpdate={delayAfterUpdate}
    lastUpdated={lastUpdated}
    lastSaved={lastSaved}
    save={autosaveAction}
  />
);

export default SubmissionAutosave;
