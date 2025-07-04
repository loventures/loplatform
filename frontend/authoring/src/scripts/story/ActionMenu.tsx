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
import React, { useCallback, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Dropdown, DropdownItem, DropdownMenu, DropdownToggle } from 'reactstrap';

import { trackAuthoringEvent } from '../analytics';
import edgeRuleConstants from '../editor/EdgeRuleConstants';
import { setAddFeedbackForAsset } from '../feedback/feedbackActions';
import { confirmSaveProjectGraphEdits, useIsConflicted } from '../graphEdit';
import { ImportExportItems } from '../importer/EdgeEditorMenu';
import { assetToExtensions } from '../importer/importConstants';
import { openModal } from '../modals/modalActions';
import { useRemoteAssetBranch } from '../structurePanel/projectGraphActions';
import { NodeName, TypeId } from '../types/asset';
import { AccessRightSubmenu } from './ActionMenu/AccessRightSubmenu';
import { ContentStatusSubmenu } from './ActionMenu/ContentStatusSubmenu';
import { CourseSubmenu } from './ActionMenu/CourseSubmenu';
import { MultiverseSubmenu } from './ActionMenu/MultiverseSubmenu';
import { ProjectStatusSubmenu } from './ActionMenu/ProjectStatusSubmenu';
import { getIcon } from './AddAsset';
import { addContentGateAction } from './GateEditor/actions';
import { useContentAccess } from './hooks/useContentAccess';
import { copyAssetAction, removeAssetAction } from './NarrativeIndex/actions';
import { NarrativeMode, primitiveTypes } from './story';
import { setNarrativeAssetState, setNarrativeState } from './storyActions';
import { useIsStoryEditMode, useRevisionCommit } from './storyHooks';

export const ActionMenu: React.FC<{
  name: NodeName;
  typeId: TypeId;
  contextPath: string;
  mode: NarrativeMode;
}> = ({ name, typeId, contextPath, mode }) => {
  const dispatch = useDispatch();
  const contextNames = contextPath?.split('.') ?? [];
  const parentName = contextNames[contextNames.length - 1];
  const Icon = getIcon(typeId);
  const isCourse = typeId === 'course.1';
  const editMode = useIsStoryEditMode();
  const commit = useRevisionCommit();
  const primitive = primitiveTypes.has(typeId);

  const conflict = useIsConflicted(name);
  const remote = useRemoteAssetBranch(name);
  const contentAccess = useContentAccess(name);

  const copyAsset = useCallback(() => dispatch(copyAssetAction(name)), [name]);

  const removeAsset = useCallback(
    () => dispatch(removeAssetAction(parentName, name, contextPath, false, mode === 'apex')),
    [parentName, name, contextPath, mode]
  );
  const cutAsset = useCallback(
    () => dispatch(removeAssetAction(parentName, name, contextPath, true, mode === 'apex')),
    [parentName, name, contextPath, mode]
  );

  const addFeedback = useCallback(() => {
    dispatch(
      confirmSaveProjectGraphEdits(() => {
        trackAuthoringEvent('Narrative Editor - Add Feedback');
        dispatch(setAddFeedbackForAsset([...contextNames, name]));
      })
    );
  }, [contextPath, name]);

  const editKeywords = useCallback(() => {
    trackAuthoringEvent('Narrative Editor - Edit Keywords');
    dispatch(setNarrativeState({ keyWords: true }));
    dispatch(setNarrativeAssetState(name, { keywording: true }));
  }, [name]);

  const openImportModal = useCallback(
    (modalId: string) => {
      dispatch(confirmSaveProjectGraphEdits(() => dispatch(openModal(modalId, { name }))));
    },
    [name]
  );

  const [open, setOpen] = useState(false);
  const gates = edgeRuleConstants[typeId]?.gates;
  const testsOut = remote ? undefined : edgeRuleConstants[typeId]?.testsOut;
  const group = testsOut ? 'testsOut' : 'gates';
  const onAddGate = useCallback(() => dispatch(addContentGateAction(name, group)), [name, group]);
  const toggleOpen = useCallback(() => setOpen(o => !o), []);

  return mode !== 'revision' && !commit && !primitive ? (
    <Dropdown
      isOpen={open}
      className="narrative-left-menu"
      toggle={toggleOpen}
    >
      <DropdownToggle
        color="primary"
        outline
        caret
        className={classNames(
          'border-0 asset-settings',
          conflict ? 'conflict' : 'unhover-muted hover-white'
        )}
        title={conflict ? "Your edits conflict with another author's edits." : undefined}
      >
        <Icon size="1.75rem" />
      </DropdownToggle>
      <DropdownMenu className="with-submenu">
        <DropdownItem
          disabled={!contentAccess.AddFeedback}
          onClick={addFeedback}
        >
          Add Feedback
        </DropdownItem>
        {isCourse && !editMode && (
          <CourseSubmenu
            name={name}
            setOpen={setOpen}
          />
        )}
        {remote && (mode === 'feedback' || !editMode) ? (
          <>
            <DropdownItem divider />
            {remote && (
              <MultiverseSubmenu
                name={name}
                mode={mode}
              />
            )}
          </>
        ) : editMode && mode !== 'feedback' ? (
          <>
            <DropdownItem divider />
            {isCourse && (
              <CourseSubmenu
                name={name}
                setOpen={setOpen}
              />
            )}
            {isCourse && <ProjectStatusSubmenu />}
            <ContentStatusSubmenu
              name={name}
              contextPath={contextPath}
              typeId={typeId}
            />
            <MultiverseSubmenu
              name={name}
              mode={mode}
            />
            {!isCourse && (
              <>
                <AccessRightSubmenu
                  name={name}
                  typeId={typeId}
                />
                <DropdownItem
                  onClick={onAddGate}
                  disabled={(!gates && !testsOut) || !contentAccess.EditSettings}
                >
                  {testsOut ? 'Add Test Out' : 'Add Content Gate'}
                </DropdownItem>
              </>
            )}
            <DropdownItem
              onClick={editKeywords}
              disabled={!contentAccess.EditSettings || !!remote}
            >
              Edit Keywords
            </DropdownItem>
            <DropdownItem divider />
            <DropdownItem
              onClick={copyAsset}
              disabled={isCourse || !contentAccess.AddRemoveContent}
            >
              Copy
            </DropdownItem>
            <DropdownItem
              onClick={cutAsset}
              disabled={isCourse || !contentAccess.AddRemoveContent}
            >
              Cut
            </DropdownItem>
            <DropdownItem
              className={isCourse || !contentAccess.AddRemoveContent ? undefined : 'text-danger'}
              onClick={removeAsset}
              disabled={isCourse || !contentAccess.AddRemoveContent}
            >
              Remove
            </DropdownItem>
            {assetToExtensions[typeId] && !remote && (
              <>
                <DropdownItem divider />
                <ImportExportItems
                  name={name}
                  typeId={typeId}
                  import
                  openImportModal={openImportModal}
                />
              </>
            )}
          </>
        ) : null}
      </DropdownMenu>
    </Dropdown>
  ) : (
    <div
      className={classNames(
        'button-spacer d-flex align-items-center justify-content-center actions-icon',
        conflict ? 'conflict' : undefined
      )}
    >
      <Icon
        size="1.75rem"
        style={{ marginRight: '.585em' }}
      />
    </div>
  );
};
