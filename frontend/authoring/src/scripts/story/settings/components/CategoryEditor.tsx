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
import React, { useCallback, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import {
  addProjectGraphEdge,
  beginProjectGraphEdit,
  deleteProjectGraphEdge,
  useAllEditedOutEdges,
  useEditedAssetTitle,
} from '../../../graphEdit';
import { FakeEdge } from '../../../graphEdit/graphEditReducer';
import { useHomeNodeName, usePolyglot } from '../../../hooks';
import { NewAsset, NodeName } from '../../../types/asset';
import { Thunk } from '../../../types/dcmState';
import { NewEdge } from '../../../types/edge';
import AssetDropdownItem from '../../components/AssetDropdownItem';
import { useFocusedRemoteEditor } from '../../storyHooks';

const editCategoryAction =
  (
    name: NodeName,
    cat: NodeName | undefined,
    category: NewEdge | FakeEdge | undefined,
    session: string
  ): Thunk =>
  dispatch => {
    if (cat === category?.targetName) return;
    dispatch(beginProjectGraphEdit('Edit gradebook category', session));
    if (category) dispatch(deleteProjectGraphEdge(category as NewEdge));
    if (cat) {
      const newEdge: NewEdge = {
        name: crypto.randomUUID(),
        group: 'gradebookCategory',
        sourceName: name,
        targetName: cat,
        data: {},
        traverse: false,
        newPosition: 'end',
      };
      dispatch(addProjectGraphEdge(newEdge));
    }
  };

export const CategoryEditor: React.FC<{
  asset: NewAsset<
    | 'assessment.1'
    | 'assignment.1'
    | 'observationAssessment.1'
    | 'courseLink.1'
    | 'lti.1'
    | 'poolAssessment.1'
    | 'scorm.1'
  >;
  category?: NewEdge | FakeEdge;
  disabled?: boolean;
}> = ({ asset, category, disabled }) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const homeNode = useHomeNodeName();
  const allEdges = useAllEditedOutEdges(homeNode);
  const categoryEdges = useMemo(
    () => allEdges.filter(edge => edge.group === 'gradebookCategories'),
    [homeNode]
  );
  const categorized = categoryEdges.find(edge => edge.targetName === category?.targetName);
  const categoryTitle = useEditedAssetTitle(category?.targetName);

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'gradebookCategory'
  );

  const editCategory = useCallback(
    (cat: NodeName | undefined) => dispatch(editCategoryAction(asset.name, cat, category, session)),
    [asset.name, category, session]
  );

  return categoryEdges.length ? (
    <UncontrolledDropdown
      className={classNames('secret-input category-editor', remoteEditor && 'remote-edit')}
      onFocus={onFocus}
      onBlur={onBlur}
      style={remoteEditor}
    >
      <DropdownToggle
        color="transparent"
        caret
        className="secret-toggle"
        disabled={disabled}
      >
        {(categorized ? categoryTitle : undefined) ??
          polyglot.t('STORY_gradebookCategory_uncategorized')}
      </DropdownToggle>
      <DropdownMenu>
        <DropdownItem onClick={() => editCategory(undefined)}>
          {polyglot.t('STORY_gradebookCategory_uncategorized')}
        </DropdownItem>
        {categoryEdges.map(edge => (
          <AssetDropdownItem
            key={edge.name}
            onClick={() => editCategory(edge.targetName)}
            name={edge.targetName}
          />
        ))}
      </DropdownMenu>
    </UncontrolledDropdown>
  ) : null;
};
