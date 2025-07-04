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
import { useDispatch } from 'react-redux';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { beginProjectGraphEdit, editProjectGraphNodeData } from '../../../graphEdit';
import { usePolyglot } from '../../../hooks';
import { NewAsset } from '../../../types/asset';
import { useFocusedRemoteEditor } from '../../storyHooks';

export const ShowAnswersEditor: React.FC<{
  asset: NewAsset<'assessment.1' | 'diagnostic.1' | 'poolAssessment.1'>;
  hideAnswerIfIncorrect: boolean;
}> = ({ asset, hideAnswerIfIncorrect }) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'hideAnswerIfIncorrect'
  );

  const editHideAnswerIfIncorrect = useCallback(
    (option: boolean) => {
      if (option === hideAnswerIfIncorrect) return;
      dispatch(beginProjectGraphEdit('Edit show answers if correct', session));
      dispatch(editProjectGraphNodeData(asset.name, { hideAnswerIfIncorrect: option }));
    },
    [hideAnswerIfIncorrect, session]
  );

  return (
    <UncontrolledDropdown
      className={classNames('secret-input show-answers-editor', remoteEditor && 'remote-edit')}
      onFocus={onFocus}
      onBlur={onBlur}
      style={remoteEditor}
    >
      <DropdownToggle
        color="transparent"
        caret
        className="secret-toggle"
      >
        {polyglot.t(`STORY_hideAnswerIfIncorrect_${!!hideAnswerIfIncorrect}`)}
      </DropdownToggle>
      <DropdownMenu>
        {([true, false] as const).map(option => (
          <DropdownItem
            key={`${option}`}
            onClick={() => editHideAnswerIfIncorrect(option)}
          >
            {polyglot.t(`STORY_hideAnswerIfIncorrect_${option}`)}
          </DropdownItem>
        ))}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
