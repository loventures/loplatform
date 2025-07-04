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
import { plural } from '../../story';
import { useFocusedRemoteEditor } from '../../storyHooks';

export const PointsEditor: React.FC<{
  asset: NewAsset<
    | 'assessment.1'
    | 'assignment.1'
    | 'diagnostic.1'
    | 'observationAssessment.1'
    | 'courseLink.1'
    | 'lti.1'
    | 'poolAssessment.1'
    | 'scorm.1'
  >;
  points: number;
  disabled?: boolean;
}> = ({ asset, points, disabled }) => {
  const dispatch = useDispatch();

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'pointsPossible'
  );

  const editPoints = useCallback(
    (pts: number) => {
      if (pts === points) return;
      dispatch(beginProjectGraphEdit('Edit points', session));
      dispatch(editProjectGraphNodeData(asset.name, { pointsPossible: pts }));
    },
    [points, session]
  );

  return (
    <div style={remoteEditor}>
      <NumericInput
        step={1}
        min={1}
        value={!points || isNaN(points) ? 0 : points}
        onChange={editPoints}
        format={n => plural(parseFloat(n), 'Point')}
        className={classNames(
          'form-control secret-input point-editor bigly',
          remoteEditor && 'remote-edit'
        )}
        disabled={disabled}
        onFocus={onFocus}
        onBlur={onBlur}
      />
    </div>
  );
};
