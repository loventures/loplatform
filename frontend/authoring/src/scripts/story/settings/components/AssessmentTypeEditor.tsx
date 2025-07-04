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
import { AssessmentType, NewAsset } from '../../../types/asset';
import { useFocusedRemoteEditor } from '../../storyHooks';

export const AssessmentTypeEditor: React.FC<{
  asset: NewAsset<
    'assessment.1' | 'assignment.1' | 'observationAssessment.1' | 'lti.1' | 'poolAssessment.1'
  >;
  assessmentType: AssessmentType;
}> = ({ asset, assessmentType }) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'assessmentType'
  );

  const editAssessmentType = useCallback(
    (option: 'formative' | 'summative') => {
      if (option === assessmentType) return;
      dispatch(beginProjectGraphEdit('Edit assessment mode', session));
      dispatch(editProjectGraphNodeData(asset.name, { assessmentType: option }));
    },
    [assessmentType, session]
  );

  return (
    <UncontrolledDropdown
      className={classNames('secret-input assessment-type-editor', remoteEditor && 'remote-edit')}
      onFocus={onFocus}
      onBlur={onBlur}
      style={remoteEditor}
    >
      <DropdownToggle
        color="transparent"
        caret
        className="secret-toggle"
      >
        {polyglot.t(`ASSESSMENT_TYPE_${assessmentType.toUpperCase()}`)}
      </DropdownToggle>
      <DropdownMenu>
        {(['formative', 'summative'] as const).map(option => (
          <DropdownItem
            key={option}
            onClick={() => editAssessmentType(option)}
          >
            {polyglot.t(`ASSESSMENT_TYPE_${option.toUpperCase()}`)}
          </DropdownItem>
        ))}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
