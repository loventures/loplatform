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
import React, { useMemo } from 'react';
import { SiWebpack } from 'react-icons/si';
import { useDispatch } from 'react-redux';
import { Input } from 'reactstrap';

import {
  beginProjectGraphEdit,
  editProjectGraphNodeData,
  useCurrentAssetName,
  useEditedAsset,
} from '../../graphEdit';
import { NewAsset } from '../../types/asset';
import NarrativePresence from '../NarrativeAsset/NarrativePresence';
import { PreviewMenu } from '../PreviewMenu';
import { NarrativeSettings } from '../story';
import { useFocusedRemoteEditor, useIsStoryEditMode } from '../storyHooks';

const DefaultsPage: React.FC = () => {
  const name = useCurrentAssetName();
  const course = useEditedAsset(name) as NewAsset<'course.1'>;

  return (
    <div className="defaults-editor">
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto' }}>
        <div className="button-spacer d-flex align-items-center justify-content-center actions-icon">
          <SiWebpack size="1.75rem" />
        </div>
        <div className="d-flex align-items-center justify-content-center minw-0 text-muted">
          Course Defaults
        </div>
        <NarrativePresence name="defaults">
          <PreviewMenu
            name={course.name}
            typeId={course.typeId}
            mode="apex"
            simple
          />
        </NarrativePresence>
      </div>
      <div className="asset-title d-flex flex-column mt-3">
        <h2>{course.data.title}</h2>
      </div>
      <div className="m-5">
        <DefaultJsEditor asset={course} />
        <DefaultCssEditor asset={course} />
      </div>
    </div>
  );
};

const DefaultJsEditor: NarrativeSettings<'course.1'> = ({ asset }) => {
  const dispatch = useDispatch();
  const editMode = useIsStoryEditMode();

  const origJs = asset.data.defaultJs;
  const defaultJs = useMemo(
    () => (editMode && (!origJs.length || !!origJs[origJs.length - 1]) ? [...origJs, ''] : origJs),
    [origJs, editMode]
  );

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'defaultJs'
  );

  const setJs = (index: number, js: string) => {
    dispatch(beginProjectGraphEdit('Edit course defaults', session));
    const newJs = [...origJs];
    newJs[index] = js.trim();
    dispatch(editProjectGraphNodeData(asset.name, { defaultJs: newJs.filter(s => !!s) }));
  };

  return (
    <>
      <h3 className="h5 mb-2 input-padding">Default JavaScripts for HTML Assets</h3>
      <div
        id="js-defaults"
        style={remoteEditor}
      >
        {defaultJs.map((js, index) => (
          <Input
            key={index}
            className={classNames('mb-2 mx-3 secret-input', remoteEditor && 'remote-edit')}
            value={js}
            onChange={e => setJs(index, e.target.value)}
            onFocus={onFocus}
            onBlur={onBlur}
            type="text"
            placeholder="https://..."
            disabled={!editMode}
            invalid={!!js && !js.match(/^https:\/\/.*\.js/)}
          />
        ))}
      </div>
    </>
  );
};

const DefaultCssEditor: NarrativeSettings<'course.1'> = ({ asset }) => {
  const dispatch = useDispatch();
  const editMode = useIsStoryEditMode();

  const origCss = asset.data.defaultCss;
  const defaultCss = useMemo(
    () =>
      editMode && (!origCss.length || !!origCss[origCss.length - 1]) ? [...origCss, ''] : origCss,
    [origCss, editMode]
  );
  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'defaultCss'
  );

  const setCss = (index: number, css: string) => {
    dispatch(beginProjectGraphEdit('Edit course defaults', session));
    const newCss = [...origCss];
    newCss[index] = css.trim();
    dispatch(editProjectGraphNodeData(asset.name, { defaultCss: newCss.filter(s => !!s) }));
  };

  return (
    <>
      <h3
        className="h5 mt-3 mb-2 input-padding"
        style={remoteEditor}
      >
        Default Stylesheets for HTML Assets
      </h3>
      <div id="css-defaults">
        {defaultCss.map((css, index) => (
          <Input
            key={index}
            className={classNames('mb-2 mx-3 secret-input', remoteEditor && 'remote-edit')}
            value={css}
            onChange={e => setCss(index, e.target.value)}
            onFocus={onFocus}
            onBlur={onBlur}
            type="text"
            placeholder="https://..."
            disabled={!editMode}
            invalid={!!css && !css.match(/^https:\/\/.*\.css/)}
          />
        ))}
      </div>
    </>
  );
};

export default DefaultsPage;
