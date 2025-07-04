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

export const GradableEditor: React.FC<{
  asset: NewAsset<'courseLink.1'>;
  gradable: boolean;
  forCredit: boolean;
}> = ({ asset, gradable, forCredit }) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(asset.name, 'gradable');

  const editForCredit = useCallback(
    (gr: boolean, fc: boolean) => {
      if (gr === gradable && fc === forCredit) return;
      dispatch(beginProjectGraphEdit('Edit gradable', session));
      dispatch(editProjectGraphNodeData(asset.name, { gradable: gr, isForCredit: fc }));
    },
    [gradable, session]
  );

  return (
    <UncontrolledDropdown
      className={classNames('secret-input gradable-editor', remoteEditor && 'remote-edit')}
      onFocus={onFocus}
      onBlur={onBlur}
      style={remoteEditor}
    >
      <DropdownToggle
        color="transparent"
        className="secret-toggle"
        caret
      >
        {polyglot.t(!gradable ? 'STORY_ungraded' : `STORY_forCredit_${forCredit}`)}
      </DropdownToggle>
      <DropdownMenu>
        <DropdownItem onClick={() => editForCredit(false, false)}>
          {polyglot.t(`STORY_ungraded`)}
        </DropdownItem>
        {([false, true] as const).map(credit => (
          <DropdownItem
            key={credit.toString()}
            onClick={() => editForCredit(true, credit)}
          >
            {polyglot.t(`STORY_forCredit_${credit}`)}
          </DropdownItem>
        ))}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
