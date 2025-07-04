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

export const AttemptsEditor: React.FC<{
  asset: NewAsset<'assessment.1' | 'assignment.1' | 'observationAssessment.1' | 'poolAssessment.1'>;
  attempts: number;
}> = ({ asset, attempts }) => {
  const dispatch = useDispatch();

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'maxAttempts'
  );

  const editAttempts = useCallback(
    (att: number) => {
      if (att === attempts) return;
      dispatch(beginProjectGraphEdit('Edit attempts', session));
      dispatch(
        editProjectGraphNodeData(asset.name, {
          maxAttempts: att ? att : null,
          unlimitedAttempts: !att,
        })
      );
    },
    [attempts, session]
  );

  return (
    <div style={remoteEditor}>
      <NumericInput
        step={1}
        min={0}
        value={!attempts || isNaN(attempts) ? 0 : attempts}
        onChange={editAttempts}
        format={n => (!parseInt(n) ? 'Unlimited Attempts' : plural(parseInt(n), 'Attempt'))}
        parse={s => ('Unlimited Attempts'.startsWith(s) ? 0 : parseInt(s))}
        className={classNames(
          'form-control secret-input attempts-editor',
          remoteEditor && 'remote-edit'
        )}
        onFocus={onFocus}
        onBlur={onBlur}
      />
    </div>
  );
};
