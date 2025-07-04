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
import Dropzone from 'react-dropzone';
import { BsArrowsExpand } from 'react-icons/bs';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

import { trackAuthoringEvent } from '../analytics';
import { trackNarrativeExpand } from '../analytics/AnalyticsEvents';
import { confirmSaveProjectGraphEdits, useAllEditedOutEdges } from '../graphEdit';
import { openImportModal } from '../importer/importActions';
import { assetToExtensions } from '../importer/importConstants';
import { openToast } from '../toast/actions';
import { NodeName, TypeId } from '../types/asset';
import { AddAsset } from './AddAsset';
import { copyAssetAction, removeAssetAction } from './NarrativeIndex/actions';
import { IndexRow } from './NarrativeIndex/IndexRow';
import { NarrativeMode, childEdgeGroup } from './story';
import { setNarrativeAssetState, setNarrativeInlineViewAction } from './storyActions';
import { useIsStoryEditMode } from './storyHooks';

export const NarrativeIndex: React.FC<{
  name: NodeName;
  typeId: TypeId;
  contextPath: string;
  mode: NarrativeMode;
}> = ({ name, typeId, contextPath, mode }) => {
  const dispatch = useDispatch();
  const editMode = useIsStoryEditMode();
  const group = childEdgeGroup(typeId);
  const questions = group === 'questions';
  const children = useAllEditedOutEdges(name).filter(edge => edge.group === group);
  const acceptable = assetToExtensions[typeId] ?? {};
  const subcontextPath = contextPath ? `${contextPath}.${name}` : name;

  const copyAsset = useCallback(
    (event: React.MouseEvent) =>
      dispatch(copyAssetAction(event.currentTarget.getAttribute('data-node-name'))),
    []
  );
  const removeAsset = useCallback(
    (event: React.MouseEvent) =>
      dispatch(
        removeAssetAction(
          name,
          event.currentTarget.getAttribute('data-node-name'),
          contextPath,
          false
        )
      ),
    [name, contextPath]
  );
  const cutAsset = useCallback(
    (event: React.MouseEvent) =>
      dispatch(
        removeAssetAction(
          name,
          event.currentTarget.getAttribute('data-node-name'),
          contextPath,
          true
        )
      ),
    [name, contextPath]
  );
  // The shitty version of react-dropzone that we're on doesn't actually handle drag-accept or reject
  // properly. It can only do drag-active. But then we are given no file if bad.
  const onDrop = useCallback(
    files => {
      const file = files[0];
      if (file) {
        dispatch(
          confirmSaveProjectGraphEdits(() => {
            dispatch(
              openImportModal(typeId, {
                file,
                name,
              })
            );
          })
        );
      } else {
        dispatch(openToast('Unsupported import file.', 'danger'));
      }
    },
    [typeId, name]
  );

  const onExpand = useCallback(
    (e: React.MouseEvent) => {
      if (mode === 'apex') {
        trackAuthoringEvent('Narrative Editor - Inline', `${typeId} - true`);
        dispatch(setNarrativeInlineViewAction(true));
        dispatch(setNarrativeAssetState(name, { renderAll: e.ctrlKey || e.metaKey }));
      } else {
        dispatch(
          setNarrativeAssetState(name, { expanded: true, renderAll: e.ctrlKey || e.metaKey })
        );
        trackNarrativeExpand();
      }
    },
    [name, typeId]
  );

  return (
    <Dropzone
      accept={acceptable}
      onDrop={onDrop}
      disabled={!editMode}
    >
      {({ getRootProps, getInputProps, isDragActive }) => (
        <div
          {...getRootProps()}
          onClick={undefined /* undo dropzone */}
          tabIndex={undefined /* undo dropzone */}
          className={classNames('mx-5', 'my-5', 'drop-able', isDragActive && 'drop-active')}
        >
          {!children.length && !editMode ? (
            <div className="text-muted mt-5 depth-1 text-center">
              {questions ? 'No questions' : 'No contents'}
            </div>
          ) : null}
          <input
            {...getInputProps()}
            multiple={false}
            name="index-dropzone"
          />
          <div className="content-list">
            {mode !== 'feedback' && (
              <div
                className="d-flex align-items-start position-sticky"
                style={{
                  top: '3rem',
                  height: '5.5rem',
                  width: 0,
                  float: 'left',
                }}
              >
                <Button
                  color="primary"
                  outline
                  className="border-0 p-2 mb-2 d-flex align-items-center expand-content-btn"
                  title="Expand Content (⌃|⌘ = Immediate)"
                  onClick={onExpand}
                  style={{
                    marginLeft: '-3rem',
                    marginTop: children.length ? '1rem' : 0,
                  }}
                >
                  <BsArrowsExpand style={{ strokeWidth: '.25px' }} />
                </Button>
              </div>
            )}
            {children.map((edge, index) => (
              <IndexRow
                key={edge.name}
                index={index}
                name={edge.targetName}
                parent={name}
                edgeData={edge.data}
                contextPath={subcontextPath}
                questions={questions}
                copyAsset={copyAsset}
                removeAsset={removeAsset}
                cutAsset={cutAsset}
                unlinked={mode === 'revision'}
              />
            ))}
            <AddAsset
              className={children.length || !editMode ? 'mini-add' : 'mt-5'}
              parent={name}
              contextPath={subcontextPath}
              tooltip={!children.length ? 'parent' : undefined}
              redirect
            />
          </div>
        </div>
      )}
    </Dropzone>
  );
};
