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

export const CreditEditor: React.FC<{
  asset: NewAsset<
    | 'assessment.1'
    | 'assignment.1'
    | 'observationAssessment.1'
    | 'courseLink.1'
    | 'lti.1'
    | 'poolAssessment.1'
    | 'scorm.1'
  >;
  forCredit: boolean;
  disabled?: boolean;
}> = ({ asset, forCredit, disabled }) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(asset.name, 'credit');

  const editForCredit = useCallback(
    (fc: boolean) => {
      if (fc === forCredit) return;
      dispatch(beginProjectGraphEdit('Edit for credit', session));
      dispatch(editProjectGraphNodeData(asset.name, { isForCredit: fc }));
    },
    [forCredit, session]
  );

  return (
    <UncontrolledDropdown
      className={classNames('secret-input credit-editor', remoteEditor && 'remote-edit')}
      onFocus={onFocus}
      onBlur={onBlur}
      style={remoteEditor}
    >
      <DropdownToggle
        color="transparent"
        className="secret-toggle"
        disabled={disabled}
        caret
      >
        {polyglot.t(`STORY_forCredit_${forCredit}`)}
      </DropdownToggle>
      <DropdownMenu>
        {([true, false] as const).map(credit => (
          <DropdownItem
            key={credit.toString()}
            onClick={() => editForCredit(credit)}
          >
            {polyglot.t(`STORY_forCredit_${credit}`)}
          </DropdownItem>
        ))}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
