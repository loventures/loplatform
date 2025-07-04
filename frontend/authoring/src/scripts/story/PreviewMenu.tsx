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

import React, { useCallback } from 'react';
import { GiDinosaurBones } from 'react-icons/gi';
import { IoEyeOutline, IoGitCompareOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import edgeRules from '../editor/EdgeRuleConstants';
import { useIsAdded } from '../graphEdit';
import { useBranchId, useDcmSelector } from '../hooks';
import { exporteousTypes, ImportExportItems } from '../importer/EdgeEditorMenu';
import { useRouteParameter } from '../router/ReactRouterService';
import { NodeName, TypeId } from '../types/asset';
import { useContentAccess, useProjectAccess } from './hooks';
import { EyeOfHorus } from './icons/EyeOfHorus';
import {
  expandAllAction,
  expandParentAction,
  previewAction,
  revisionHistoryAction,
} from './PreviewMenu/actions';
import { editorUrl, NarrativeMode, trackNarrativeEventHandler } from './story';
import { useRevisionCommit } from './storyHooks';
import { Link } from 'react-router-dom';

const childBearing = (typeId: TypeId): boolean =>
  !!edgeRules[typeId]?.elements || !!edgeRules[typeId]?.questions;

export const PreviewMenu: React.FC<{
  name: NodeName;
  typeId: TypeId;
  contextPath?: string;
  mode: NarrativeMode;
  simple?: boolean;
}> = ({ name, typeId, contextPath, mode, simple }) => {
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const commit = useRevisionCommit();
  const diff = useRouteParameter('diff') === 'true';
  const userCanEdit = useDcmSelector(state => state.layout.userCanEdit);
  const contentAccess = useContentAccess(name);
  const projectAccess = useProjectAccess();

  const graphLink = editorUrl(
    'story',
    branchId,
    'graph',
    contextPath ? `${contextPath}.${name}` : name,
    { commit }
  );
  const showGraphLink = useDcmSelector(
    state => state.user.profile?.user_type === 'Overlord' || !state.layout.platform.isProduction
  );

  const isAdded = useIsAdded(name);

  const onPreview = useCallback(
    (event: React.MouseEvent) =>
      dispatch(
        previewAction(
          name,
          contextPath,
          event.currentTarget.getAttribute('data-preview-role') as any,
          event.metaKey || event.ctrlKey
        )
      ),
    [name, contextPath]
  );

  const onRevisionHistory = useCallback(
    () => dispatch(revisionHistoryAction(name, typeId, contextPath)),
    [name, typeId, contextPath]
  );

  const onExpandAll = useCallback(() => dispatch(expandAllAction(name)), [name]);

  const onExpandParent = useCallback(
    () => dispatch(expandParentAction(name, contextPath)),
    [name, contextPath]
  );

  const Icon = mode === 'inline' ? IoEyeOutline : EyeOfHorus;

  const showPageHistory = mode !== 'feedback' && userCanEdit && !simple;
  const exporteous = mode !== 'feedback' && exporteousTypes.has(typeId) && !simple;
  const bearable = mode !== 'feedback' && childBearing(typeId);
  const parentable = mode === 'apex';

  return mode !== 'revision' && !!commit && exporteous ? (
    <UncontrolledDropdown className="narrative-right-menu">
      <DropdownToggle
        color="primary"
        outline
        caret
        className="border-0 asset-preview unhover-muted hover-white"
      >
        <GiDinosaurBones size="2rem" />
      </DropdownToggle>
      <DropdownMenu end>
        <ImportExportItems
          name={name}
          typeId={typeId}
          export
        />
      </DropdownMenu>
    </UncontrolledDropdown>
  ) : mode === 'revision' || !!commit ? (
    <div className="button-spacer d-flex align-items-center justify-content-center actions-icon">
      {diff ? (
        <IoGitCompareOutline
          size="1.5rem"
          style={{ marginLeft: '.96em' }}
        />
      ) : (
        <GiDinosaurBones
          size="2rem"
          style={{ marginLeft: '.46em' }}
        />
      )}
    </div>
  ) : (
    <UncontrolledDropdown className="narrative-right-menu">
      <DropdownToggle
        color="primary"
        outline
        caret
        className="border-0 asset-preview unhover-muted hover-white"
      >
        <Icon size="1.5rem" />
      </DropdownToggle>
      <DropdownMenu end>
        <DropdownItem
          disabled={isAdded || !projectAccess.Preview}
          onClick={onPreview}
          data-preview-role="Learner"
        >
          Learner Preview
        </DropdownItem>
        <DropdownItem
          disabled={isAdded || !projectAccess.Preview}
          onClick={onPreview}
          data-preview-role="Instructor"
        >
          Instructor Preview
        </DropdownItem>
        <DropdownItem
          disabled={isAdded || !projectAccess.Preview}
          onClick={onPreview}
        >
          Author Preview
        </DropdownItem>
        {showPageHistory && (
          <>
            <DropdownItem divider />
            <DropdownItem
              onClick={onRevisionHistory}
              disabled={!contentAccess.PageHistory}
            >
              Page History
            </DropdownItem>
          </>
        )}
        {showGraphLink && (
          <DropdownItem
            tag={Link}
            to={graphLink}
            onClick={trackNarrativeEventHandler('Project Graph')}
          >
            Project Graph
          </DropdownItem>
        )}
        {(bearable || parentable) && <DropdownItem divider />}
        {parentable && (
          <DropdownItem
            onClick={onExpandParent}
            disabled={typeId === 'course.1'}
          >
            Expand Parent
          </DropdownItem>
        )}
        {bearable && (
          <DropdownItem
            onClick={onExpandAll}
            disabled={typeId === 'course.1'}
          >
            Expand Descendants
          </DropdownItem>
        )}
        {exporteous && (
          <>
            <DropdownItem divider />
            <ImportExportItems
              name={name}
              typeId={typeId}
              export
            />
          </>
        )}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
