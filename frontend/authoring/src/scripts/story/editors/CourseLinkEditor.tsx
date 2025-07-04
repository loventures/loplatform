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
import { MdOutlineOpenInNew } from 'react-icons/md';
import { useDispatch } from 'react-redux';
import Select from 'react-select';
import { Button, Col, FormGroup, Input, Label, Row } from 'reactstrap';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
} from '../../graphEdit';
import { usePolyglot } from '../../hooks';
import { HtmlPart, SectionPolicy } from '../../types/asset';
import { useProjects } from '../dataActions';
import { PartEditor } from '../PartEditor';
import { NarrativeEditor, cap, storyTypeName } from '../story';
import { useFocusedRemoteEditor, useIsEditable } from '../storyHooks';
import { previewCourseContent } from '../PreviewMenu/actions.ts';

const policyOptions = [
  {
    value: 'MostRecent',
    label: 'Most recently created section',
  },
  {
    value: 'LinkedSection',
    label: 'Create a new linked section',
  },
];

export const CourseLinkEditor: NarrativeEditor<'courseLink.1'> = ({ asset, readOnly }) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();

  const editMode = useIsEditable(asset.name) && !readOnly;

  const typeName = cap(storyTypeName(polyglot, asset.typeId));
  const placeholder = polyglot.t('STORY_INSTRUCTIONS', { typeName });

  const branchList = useProjects();
  const branchOptions = useMemo(
    () => branchList.map(o => ({ value: o.branchId, label: o.branchName })),
    [branchList]
  );
  const branch = useMemo(
    () => branchList.find(o => o.branchId === asset.data.branch),
    [branchList, asset]
  );
  const branchOption = useMemo(
    () =>
      branchOptions.find(o => o.value === branch?.branchId) ??
      (asset.data.branch
        ? {
            value: asset.data.branch,
            label: 'Inaccessible project',
          }
        : null),
    [branchOptions, branch, asset]
  );

  const updateHtml = useCallback(
    (html: HtmlPart, session: string) => {
      dispatch(beginProjectGraphEdit('Edit instructions', session));
      dispatch(
        editProjectGraphNodeData(asset.name, {
          instructions: {
            partType: 'block',
            parts: [html],
          },
        })
      );
    },
    [asset.name]
  );

  const updateBranch = useCallback(
    (option: { value: number }) => {
      dispatch(beginProjectGraphEdit('Edit linked course'));
      dispatch(
        editProjectGraphNodeData(asset.name, {
          branch: option.value,
        })
      );
      dispatch(autoSaveProjectGraphEdits());
    },
    [asset.name]
  );

  const updateNewWindow = useCallback(
    (newWindow: boolean) => {
      dispatch(beginProjectGraphEdit('Edit launch style'));
      dispatch(
        editProjectGraphNodeData(asset.name, {
          newWindow,
        })
      );
      dispatch(autoSaveProjectGraphEdits());
    },
    [asset.name]
  );

  const updatePolicy = useCallback(
    (option: { value: string }) => {
      dispatch(beginProjectGraphEdit('Edit launch style'));
      dispatch(
        editProjectGraphNodeData(asset.name, {
          sectionPolicy: option.value as SectionPolicy,
        })
      );
      dispatch(autoSaveProjectGraphEdits());
    },
    [asset.name]
  );

  const noLaunch = !branch;

  const { onFocus, onBlur, remoteEditor } = useFocusedRemoteEditor(asset.name, 'courseLink');
  const remoteEdit = remoteEditor && 'remote-edit';

  return (
    <>
      <PartEditor
        id="instructions"
        asset={asset}
        part={asset.data.instructions}
        placeholder={placeholder}
        onChange={updateHtml}
      />

      <div
        className="mt-4 mb-5 lti-margin"
        style={remoteEditor}
      >
        <Row>
          <Label md={2}>Linked Course</Label>
          <Col
            md={10}
            className="d-flex align-items-center"
          >
            <Select
              className={classNames('narrative-select flex-grow-1 Select', remoteEdit)}
              value={branchOption}
              options={branchOptions}
              isDisabled={!editMode}
              isClearable={false}
              onChange={updateBranch}
              classNames={{
                control: a => (a.isFocused ? 'react-select-focused hover-select' : 'hover-select'),
              }}
              onFocus={onFocus}
              onBlur={onBlur}
            />
            <FormGroup check>
              <Label
                check
                className="me-0 ms-3 mb-0"
              >
                <Input
                  type="checkbox"
                  className={remoteEdit}
                  checked={asset.data.newWindow}
                  onChange={e => updateNewWindow(e.target.checked)}
                  disabled={!editMode}
                  onFocus={onFocus}
                  onBlur={onBlur}
                />
                Launch in New Tab
              </Label>
            </FormGroup>
          </Col>
        </Row>
        <Row className="mt-2">
          <Label md={2}>Section Policy</Label>
          <Col
            md={10}
            className="d-flex align-items-center"
          >
            <Select
              className={classNames('narrative-select flex-grow-1 Select', remoteEdit)}
              value={policyOptions.find(p => p.value === asset.data.sectionPolicy)}
              options={policyOptions}
              isDisabled={!editMode}
              isClearable={false}
              onChange={updatePolicy}
              classNames={{
                control: a => (a.isFocused ? 'react-select-focused hover-select' : 'hover-select'),
              }}
              onFocus={onFocus}
              onBlur={onBlur}
            />
          </Col>
        </Row>
        <Row className="mt-4">
          <Col md={{ offset: 2, size: 10 }}>
            <div style={noLaunch ? { cursor: 'not-allowed' } : undefined}>
              <Button
                color="primary"
                block
                className={classNames(
                  'd-flex align-items-center justify-content-center',
                  noLaunch && 'disabled'
                )}
                style={noLaunch ? { pointerEvents: 'none' } : undefined}
                disabled={noLaunch}
                onClick={() => {
                  dispatch(
                    previewCourseContent(
                      true,
                      branch.branchId,
                      branch.project.homeNodeName,
                      '',
                      null
                    )
                  );
                }}
              >
                {`Launch ${branch?.branchName ?? 'Course Link'}`}
                <MdOutlineOpenInNew className="ms-2" />
              </Button>
            </div>
          </Col>
        </Row>
      </div>
    </>
  );
};
