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

import { NarrativeSettings, plural, sentence } from '../story';
import { useIsEditable } from '../storyHooks';
import { DurationEditor } from './components/DurationEditor';

export const CheckpointSettings: NarrativeSettings<'checkpoint.1'> = ({ asset }) => {
  const editMode = useIsEditable(asset.name, 'EditSettings');
  const duration = asset.data.duration;
  return editMode ? (
    <div className="mx-3 mb-3 text-center d-flex justify-content-center form-inline gap-2 parameter-center">
      <DurationEditor
        asset={asset}
        duration={duration}
      />
    </div>
  ) : (
    <div className="mx-3 mb-2 d-flex justify-content-center">
      <span className="input-padding text-muted text-center feedback-context">
        {sentence(!duration ? 'no duration set' : plural(duration, 'minute'))}
      </span>
    </div>
  );
};
