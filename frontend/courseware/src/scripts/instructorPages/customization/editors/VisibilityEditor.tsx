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

import { CustomisableContent } from '../../../api/customizationApi';
import { CourseState } from '../../../loRedux';
import { LoCheckbox } from '../../../directives/LoCheckbox';
import { AssetTypeId } from '../../../utilities/assetTypes';
import React from 'react';
import { connect } from 'react-redux';
import { withHandlers } from 'recompose';
import { Dispatch } from 'redux';

import { hideChild } from '../contentEdits';
import { addEdit, openHideConfirmModal } from '../courseCustomizerReducer';

type VisibilityEditorProps = VisibilityEditorOuterProps & {
  toggleHidden: () => void;
};

const AssetTypesThatContainOrTeachContent: AssetTypeId[] = [
  'html.1',
  'lesson.1',
  'lti.1',
  'module.1',
  'resource.1',
];

function contentShouldPromptModal(c: CustomisableContent): boolean {
  return AssetTypesThatContainOrTeachContent.includes(c.typeId);
}

type VisibilityEditorOuterProps = {
  skipHideConfirmation: boolean;
  dispatch: Dispatch;
  content: CustomisableContent;
  parentId: string;
  hidden: boolean;
  showEditors: boolean;
  nextVisibleNodeId?: string;
};

const VisibilityEditorInner: React.FC<VisibilityEditorProps> = ({
  hidden,
  toggleHidden,
  showEditors,
}) => (
  <>
    {(showEditors || hidden) && (
      <span className="hidden-editor">
        <LoCheckbox
          state={hidden}
          checkboxLabel="HIDDEN"
          onToggle={toggleHidden}
        />
      </span>
    )}
  </>
);

export const VisibilityEditor = connect((state: CourseState) => {
  return {
    skipHideConfirmation: state.courseCustomizations.customizerState.skipHiddenConfirmation,
  };
})(
  withHandlers({
    toggleHidden:
      ({
        dispatch,
        content,
        parentId,
        hidden,
        skipHideConfirmation,
        nextVisibleNodeId,
      }: VisibilityEditorOuterProps) =>
      () => {
        if (hidden) {
          dispatch(addEdit(hideChild({ id: parentId, childId: content.id, hidden: false })));
        } else if (contentShouldPromptModal(content) && !skipHideConfirmation) {
          dispatch(
            openHideConfirmModal({
              id: parentId,
              childId: content.id,
              nextVisibleNodeId: nextVisibleNodeId,
            })
          );
        } else {
          dispatch(addEdit(hideChild({ id: parentId, childId: content.id, hidden: true })));
        }
      },
  })(VisibilityEditorInner)
);
