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

import { trackAuthoringEvent } from '../analytics';
import { confirmSaveProjectGraphEdits, suppressPromptForUnsavedGraphEdits } from '../graphEdit';
import { Thunk } from '../types/dcmState';

export const testSectionAction = (newTab: boolean): Thunk =>
  confirmSaveProjectGraphEdits((dispatch, getState) => {
    const state = getState();
    const project = state.layout.project;
    trackAuthoringEvent('Narrative Editor - Test Section');
    suppressPromptForUnsavedGraphEdits(true);
    const url = `/Administration/TestSections?project=${project.id}`;
    if (newTab) {
      window.open(url, '_blank');
    } else {
      document.location.href = url;
    }
  });
