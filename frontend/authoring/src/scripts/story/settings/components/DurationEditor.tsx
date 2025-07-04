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

import classNames from 'classnames';
import React, { useCallback } from 'react';
import NumericInput from 'react-numeric-input2';
import { useDispatch } from 'react-redux';

import { beginProjectGraphEdit, editProjectGraphNodeData } from '../../../graphEdit';
import { NewAsset } from '../../../types/asset';
import { useFocusedRemoteEditor } from '../../storyHooks';

export const DurationEditor: React.FC<{
  asset: NewAsset<any>;
  duration: number;
}> = ({ asset, duration }) => {
  const dispatch = useDispatch();

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(asset.name, 'duration');

  const editDuration = useCallback(
    (dur: number) => {
      if (!dur ? !duration : dur === duration) return;
      dispatch(beginProjectGraphEdit('Edit duration', session));
      dispatch(editProjectGraphNodeData(asset.name, { duration: dur || null }));
    },
    [duration, session]
  );

  return (
    <div style={remoteEditor}>
      <NumericInput
        step={1}
        min={0}
        value={!duration || isNaN(duration) ? 0 : duration}
        onChange={editDuration}
        format={n => (!parseInt(n) ? 'No Duration Set' : `${n} Minute Duration`)}
        className={classNames(
          'form-control secret-input duration-editor bigly',
          remoteEditor && 'remote-edit'
        )}
        placeholder="No Duration Set"
        onFocus={onFocus}
        onBlur={onBlur}
      />
    </div>
  );
};
