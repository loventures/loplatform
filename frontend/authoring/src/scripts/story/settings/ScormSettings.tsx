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
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import {
  beginProjectGraphEdit,
  editProjectGraphNodeData,
  useAllEditedOutEdges,
  useEditedAssetTitle,
} from '../../graphEdit';
import { usePolyglot } from '../../hooks';
import { NewAsset, ScormData } from '../../types/asset';
import { NarrativeSettings, plural, sentence } from '../story';
import { useFocusedRemoteEditor, useIsEditable } from '../storyHooks';
import { CategoryEditor } from './components/CategoryEditor';
import { CreditEditor } from './components/CreditEditor';
import { DurationEditor } from './components/DurationEditor';
import { PointsEditor } from './components/PointsEditor';

// Structural also offers:
// . Subtitle
// . Keywords
// . Icon
// . Soft Limit <- deliberately deprecating
export const ScormSettings: NarrativeSettings<'scorm.1'> = ({ asset }) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const editMode = useIsEditable(asset.name, 'EditSettings');

  const forCredit = asset.data.isForCredit;
  const allEdges = useAllEditedOutEdges(asset.name);
  const categoryEdge = allEdges.find(edge => edge.group === 'gradebookCategory');
  const categoryTitle = useEditedAssetTitle(categoryEdge?.targetName, 'Uncategorized');
  const points = asset.data.pointsPossible;
  const duration = asset.data.duration;
  const width = asset.data.contentWidth;
  const height = asset.data.contentHeight;
  const newWindow = !!asset.data.launchNewWindow;

  const editAttribute = useCallback(
    (data: Partial<ScormData>, attribute: string, session: string) => {
      dispatch(beginProjectGraphEdit(`Edit ${attribute}`, session));
      dispatch(editProjectGraphNodeData(asset.name, data));
    },
    [asset.name, dispatch]
  );

  return editMode ? (
    <div className="mx-3 mb-3 text-center d-flex justify-content-center form-inline gap-2 parameter-center">
      <CreditEditor
        asset={asset}
        forCredit={forCredit}
      />
      <CategoryEditor
        asset={asset}
        category={categoryEdge}
      />
      <PointsEditor
        asset={asset}
        points={points}
      />
      <ScormWidthEditor
        asset={asset}
        editAttribute={editAttribute}
      />
      &times;
      <ScormHeightEditor
        asset={asset}
        editAttribute={editAttribute}
      />
      <ScormLaunchStyleEditor
        asset={asset}
        editAttribute={editAttribute}
      />
      <DurationEditor
        asset={asset}
        duration={duration}
      />
    </div>
  ) : (
    <div className="mx-3 mb-2 d-flex justify-content-center">
      <span className="input-padding text-muted text-center feedback-context">
        {sentence(
          polyglot.t(`STORY_forCredit_${forCredit}`),
          categoryTitle,
          plural(points, 'point'),
          `${width || 'unset'} × ${height || 'unset'} px`,
          polyglot.t(`STORY_newWindow_${newWindow}`),
          !duration ? 'no duration set' : plural(duration, 'minute')
        )}
      </span>
    </div>
  );
};

type ScormAttributeEditor = {
  asset: NewAsset<'scorm.1'>;
  editAttribute: (data: Partial<ScormData>, attribute: string, session: string) => void;
};

const ScormWidthEditor: React.FC<ScormAttributeEditor> = ({ asset, editAttribute }) => {
  const width = asset.data.contentWidth;

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'contentWidth'
  );

  return (
    <div style={remoteEditor}>
      <NumericInput
        step={1}
        min={0}
        value={!width || isNaN(width) ? 0 : width}
        onChange={w => editAttribute({ contentWidth: w || null }, 'width', session)}
        format={n => (n ? `${n} px` : 'unset')}
        className={classNames(
          'form-control secret-input px-editor x-dimension',
          remoteEditor && 'remote-edit'
        )}
        placeholder="Unset"
        onFocus={onFocus}
        onBlur={onBlur}
      />
    </div>
  );
};

const ScormHeightEditor: React.FC<ScormAttributeEditor> = ({ asset, editAttribute }) => {
  const height = asset.data.contentHeight;

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'contentHeight'
  );

  return (
    <div style={remoteEditor}>
      <NumericInput
        step={1}
        min={0}
        value={!height || isNaN(height) ? 0 : height}
        onChange={h => editAttribute({ contentHeight: h || null }, 'height', session)}
        format={n => (n ? `${n} px` : 'unset')}
        className={classNames(
          'form-control secret-input px-editor y-dimension',
          remoteEditor && 'remote-edit'
        )}
        placeholder="Unset"
        onFocus={onFocus}
        onBlur={onBlur}
      />
    </div>
  );
};

const ScormLaunchStyleEditor: React.FC<ScormAttributeEditor> = ({ asset, editAttribute }) => {
  const polyglot = usePolyglot();

  const newWindow = !!asset.data.launchNewWindow;

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'launchNewWindow'
  );

  return (
    <UncontrolledDropdown
      className={classNames('secret-input display-dropdown', remoteEditor && 'remote-edit')}
      onFocus={onFocus}
      onBlur={onBlur}
      style={remoteEditor}
    >
      <DropdownToggle
        color="transparent"
        caret
        className="secret-toggle"
      >
        {polyglot.t(`STORY_newWindow_${newWindow}`)}
      </DropdownToggle>
      <DropdownMenu>
        {([false, true] as const).map(launchNewWindow => (
          <DropdownItem
            key={launchNewWindow.toString()}
            onClick={() => editAttribute({ launchNewWindow }, 'launch style', session)}
          >
            {polyglot.t(`STORY_newWindow_${launchNewWindow}`)}
          </DropdownItem>
        ))}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
