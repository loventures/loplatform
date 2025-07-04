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
import React, { useCallback, useMemo, useRef, useState } from 'react';
import { GiTeacher } from 'react-icons/gi';
import { IoCheckmarkOutline } from 'react-icons/io5';
import { MdOutlineOpenInNew } from 'react-icons/md';
import { PiStudent } from 'react-icons/pi';
import { useDispatch } from 'react-redux';
import Select from 'react-select';
import {
  Button,
  Col,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  FormGroup,
  Input,
  Label,
  Row,
  UncontrolledDropdown,
  UncontrolledTooltip,
} from 'reactstrap';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
  useIsEdited,
} from '../../graphEdit';
import { useBranchId, usePolyglot } from '../../hooks';
import { HtmlPart } from '../../types/asset';
import { LtiToolConfiguration } from '../../types/lti';
import { useLtiTools } from '../dataActions';
import { PartEditor } from '../PartEditor';
import { NarrativeEditor, cap, storyTypeName } from '../story';
import { useFocusedRemoteEditor, useIsEditable } from '../storyHooks';

export const LtiEditor: NarrativeEditor<'lti.1'> = ({ asset, readOnly }) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const branchId = useBranchId();

  const editMode = useIsEditable(asset.name) && !readOnly;
  const edited = useIsEdited(asset.name);
  const [learner, setLearner] = useState<boolean>();
  const isLearner = learner ?? !editMode;

  const typeName = cap(storyTypeName(polyglot, asset.typeId));
  const placeholder = polyglot.t('STORY_INSTRUCTIONS', { typeName });
  const launchRef = useRef<HTMLDivElement>();

  const toolList = useLtiTools();
  const toolOptions = useMemo(
    () => toolList.map(o => ({ value: o.toolId, label: o.name })),
    [toolList]
  );
  const tool = useMemo(
    () => toolList.find(o => o.toolId === asset.data.lti.toolId),
    [toolList, asset]
  );
  const toolOption = useMemo(
    () => toolOptions.find(o => o.value === tool?.toolId) ?? null,
    [toolOptions, tool]
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

  const lti = asset.data.lti;
  const editable = tool?.ltiConfiguration.instructorEditable;
  const defaultConfig = tool?.ltiConfiguration.defaultConfiguration;

  const updateTool = useCallback(
    (option: { value: string, label: string }) => {
      dispatch(beginProjectGraphEdit('Edit LTI tool'));
      // we need to copy down isGraded because that defines the gradebook and needs to be
      // stable even if changed in admin.
      const newTool = toolList.find(tool => tool.toolId === option.value);
      dispatch(
        editProjectGraphNodeData(asset.name, {
          lti: {
            ...lti,
            toolId: option.value,
            name: option.label,
            toolConfiguration: {
              customParameters: {},
              isGraded: newTool?.ltiConfiguration.defaultConfiguration.isGraded,
            },
          },
        })
      );
      dispatch(autoSaveProjectGraphEdits());
    },
    [asset.name, lti, toolList]
  );

  const updateToolConfig = useCallback(
    <K extends keyof LtiToolConfiguration>(
      comment: string,
      key: K,
      value: LtiToolConfiguration[K]
    ) => {
      dispatch(beginProjectGraphEdit(comment, comment));
      dispatch(
        editProjectGraphNodeData(asset.name, {
          lti: {
            ...lti,
            toolConfiguration: {
              ...lti.toolConfiguration,
              [key]: value === defaultConfig?.[key] && key !== 'isGraded' ? undefined : value,
            },
          },
        })
      );
      // TODO: the input boxes don't support expected enter/escape behaviour, instead every keypress is saved...
      dispatch(autoSaveProjectGraphEdits());
    },
    [asset.name, lti]
  );

  const updateUrl = useCallback(
    (url: string | undefined) => updateToolConfig('Edit LTI tool URL', 'url', url),
    [updateToolConfig]
  );
  const updateKey = useCallback(
    (key: string | undefined) => updateToolConfig('Edit LTI tool key', 'key', key),
    [updateToolConfig]
  );
  const updateSecret = useCallback(
    (secret: string | undefined) => updateToolConfig('Edit LTI tool secret', 'secret', secret),
    [updateToolConfig]
  );

  const config = useMemo(
    () =>
      defaultConfig
        ? {
            url: lti.toolConfiguration.url ?? defaultConfig.url,
            key: lti.toolConfiguration.key ?? defaultConfig.key,
            secret: lti.toolConfiguration.secret ?? defaultConfig.secret,
            launchStyle: lti.toolConfiguration.launchStyle ?? defaultConfig.launchStyle,
            includeUsername: lti.toolConfiguration.includeUsername ?? defaultConfig.includeUsername,
            includeRoles: lti.toolConfiguration.includeRoles ?? defaultConfig.includeRoles,
            includeEmailAddress:
              lti.toolConfiguration.includeEmailAddress ?? defaultConfig.includeEmailAddress,
            includeContextTitle:
              lti.toolConfiguration.includeContextTitle ?? defaultConfig.includeContextTitle,
            useExternalId: lti.toolConfiguration.useExternalId ?? defaultConfig.useExternalId,
            isGraded: lti.toolConfiguration.isGraded ?? defaultConfig.isGraded,
            customParameters: {
              ...defaultConfig.customParameters,
              ...lti.toolConfiguration.customParameters,
            },
          }
        : lti.toolConfiguration,
    [lti.toolConfiguration, defaultConfig]
  );

  // isGraded is invalid if the default tool is graded and either it's instructor editable
  // and no value exists in the LTI tool, or it's not instructor editable and not graded in the LTI tool
  const invalidGraded =
    defaultConfig?.isGraded &&
    (editable.isGraded ? lti.toolConfiguration.isGraded == null : !lti.toolConfiguration.isGraded);

  const { onFocus, onBlur, remoteEditor } = useFocusedRemoteEditor(asset.name, 'lti');
  const remoteEdit = remoteEditor && 'remote-edit';

  // The weird onBlur stuff below is because clearing a field should reset it to the default, but
  // if I want to change a field I need to be able to delete everything in it and then type in a
  // new value. So I only reset to default on blur

  return (
    <>
      <PartEditor
        id="instructions"
        asset={asset}
        part={asset.data.instructions}
        placeholder={placeholder}
        onChange={updateHtml}
        readOnly={readOnly}
      />

      <div
        className="my-4 lti-margin"
        style={remoteEditor}
      >
        <Row>
          <Label md={2}>LTI Tool</Label>
          <Col
            md={10}
            className="d-flex align-items-center"
          >
            <Select
              className={classNames(
                'narrative-select flex-grow-1 Select',
                invalidGraded && 'is-invalid',
                remoteEdit
              )}
              value={toolOption}
              options={toolOptions}
              isDisabled={!editMode}
              isClearable={false}
              onChange={updateTool}
              classNames={{
                control: a => (a.isFocused ? 'react-select-focused hover-select' : 'hover-select'),
              }}
              onFocus={onFocus}
              onBlur={onBlur}
            />
            {editable?.launchStyle && (
              <FormGroup check>
                <Label
                  check
                  className="me-0 ms-3 mb-0"
                >
                  <Input
                    type="checkbox"
                    className={remoteEdit}
                    checked={config.launchStyle === 'NEW_WINDOW'}
                    onChange={e =>
                      updateToolConfig(
                        'Edit launch style',
                        'launchStyle',
                        e.target.checked ? 'NEW_WINDOW' : 'FRAMED'
                      )
                    }
                    disabled={!editMode}
                    onFocus={onFocus}
                    onBlur={onBlur}
                  />
                  Launch in New Tab
                </Label>
              </FormGroup>
            )}
          </Col>
        </Row>
        {invalidGraded && (
          <Row>
            <Col md={{ size: 10, offset: 2 }}>
              <Button
                size="sm"
                color="link"
                className="btn-link-danger"
                onClick={() => updateToolConfig('Add LTI tool to gradebook', 'isGraded', true)}
              >
                + Add this tool to the gradebook
              </Button>
            </Col>
          </Row>
        )}
        {editable?.url && (
          <Row className="mt-2">
            <Label md={2}>Tool URL</Label>
            <Col md={10}>
              <Input
                type="text"
                value={config.url ?? ''}
                onChange={e => updateUrl(e.target.value)}
                onFocus={onFocus}
                onBlur={e => {
                  if (!config.url) updateUrl(undefined);
                  onBlur(e);
                }}
                disabled={!editMode}
                className={remoteEdit}
              />
            </Col>
          </Row>
        )}
        {(editable?.key || editable?.secret) && (
          <Row className="mt-2">
            <Label md={2}>{`Tool ${[
              ...(editable?.key ? ['Key'] : []),
              ...(editable?.secret ? ['Secret'] : []),
            ].join(' / ')}`}</Label>
            <Col
              md={10}
              className="d-flex gap-2"
            >
              <Input
                type="text"
                value={config.key ?? ''}
                onChange={e => updateKey(e.target.value)}
                onFocus={onFocus}
                onBlur={e => {
                  if (!config.key) updateKey(undefined);
                  onBlur(e);
                }}
                disabled={!editMode}
                className={remoteEdit}
              />
              <Input
                type="password"
                value={config.secret ?? ''}
                onChange={e => updateSecret(e.target.value)}
                onFocus={onFocus}
                onBlur={e => {
                  if (!config.secret) updateSecret(undefined);
                  onBlur(e);
                }}
                disabled={!editMode}
                className={remoteEdit}
              />
            </Col>
          </Row>
        )}
        <Row className="mt-2">
          <Col md={{ size: 10, offset: 2 }}>
            {editable?.includeUsername && (
              <FormGroup
                check
                inline
              >
                <Label className="mb-0 me-3">
                  <Input
                    type="checkbox"
                    checked={!!config.includeUsername}
                    onChange={e =>
                      updateToolConfig('Edit send username', 'includeUsername', e.target.checked)
                    }
                    disabled={!editMode}
                    onFocus={onFocus}
                    onBlur={onBlur}
                    className={remoteEdit}
                  />
                  Send Username
                </Label>
              </FormGroup>
            )}
            {editable?.includeEmailAddress && (
              <FormGroup
                check
                inline
              >
                <Label className="mb-0 me-3">
                  <Input
                    type="checkbox"
                    checked={!!config.includeEmailAddress}
                    onChange={e =>
                      updateToolConfig(
                        'Edit send email address',
                        'includeEmailAddress',
                        e.target.checked
                      )
                    }
                    disabled={!editMode}
                    onFocus={onFocus}
                    onBlur={onBlur}
                    className={remoteEdit}
                  />
                  Send Email Address
                </Label>
              </FormGroup>
            )}
            {editable?.includeRoles && (
              <FormGroup
                check
                inline
              >
                <Label className="mb-0 me-3">
                  <Input
                    type="checkbox"
                    checked={!!config.includeRoles}
                    onChange={e =>
                      updateToolConfig('Edit send roles', 'includeRoles', e.target.checked)
                    }
                    disabled={!editMode}
                    onFocus={onFocus}
                    onBlur={onBlur}
                    className={remoteEdit}
                  />
                  Send Roles
                </Label>
              </FormGroup>
            )}
            {editable?.includeContextTitle && (
              <FormGroup
                check
                inline
              >
                <Label className="mb-0 me-3">
                  <Input
                    type="checkbox"
                    checked={!!config.includeContextTitle}
                    onChange={e =>
                      updateToolConfig(
                        'Edit send context title',
                        'includeContextTitle',
                        e.target.checked
                      )
                    }
                    disabled={!editMode}
                    onFocus={onFocus}
                    onBlur={onBlur}
                    className={remoteEdit}
                  />
                  Send Context Title
                </Label>
              </FormGroup>
            )}
            {editable?.isGraded && (
              <FormGroup
                check
                inline
              >
                <Label className="mb-0 me-3">
                  <Input
                    type="checkbox"
                    checked={!!config.isGraded}
                    onChange={e =>
                      updateToolConfig('Edit graded activity', 'isGraded', e.target.checked)
                    }
                    disabled={!editMode}
                    onFocus={onFocus}
                    onBlur={onBlur}
                    className={remoteEdit}
                  />
                  Graded Activity
                </Label>
              </FormGroup>
            )}
          </Col>
        </Row>
        {editable?.editableCustomParameters.map(param => (
          <Row
            className="mt-2"
            key={param}
          >
            <Label md={2}>{param}</Label>
            <Col md={10}>
              <Input
                type="text"
                value={config.customParameters[param] ?? ''}
                onChange={e =>
                  updateToolConfig(`Edit ${param}`, 'customParameters', {
                    ...config.customParameters,
                    [param]: e.target.value,
                  })
                }
                onFocus={onFocus}
                onBlur={e => {
                  if (!config.customParameters[param]) {
                    const customParameters = { ...config.customParameters };
                    delete customParameters[param];
                    updateToolConfig(`Edit ${param}`, 'customParameters', customParameters);
                  }
                  onBlur(e);
                }}
                className={remoteEdit}
                disabled={!editMode}
              />
            </Col>
          </Row>
        ))}
        {/* TODO: support new custom parameters */}
        <Row className="mt-2">
          <Col md={{ offset: 2, size: 10 }}>
            <div
              ref={launchRef}
              className="btn-group w-100"
            >
              <a
                target="_blank"
                rel="noreferrer nofollow"
                className={classNames(
                  'btn btn-primary d-flex align-items-center justify-content-center',
                  (edited || !tool) && 'disabled'
                )}
                onClick={e => {
                  if (edited || !tool) e.preventDefault();
                }}
                href={`/api/v2/authoring/${branchId}/asset/${asset.name}/lti/launch?role=${
                  isLearner ? 'Learner' : 'Instructor'
                }`}
              >
                {`Launch ${tool?.name ?? 'LTI Tool'}`}
                <MdOutlineOpenInNew className="ms-2" />
              </a>
              {editMode && (
                <UncontrolledDropdown
                  group
                  style={{ borderLeft: '1px solid #fff3' }}
                >
                  <DropdownToggle
                    color="primary"
                    className="dropdown-toggle d-flex align-items-center"
                    title={isLearner ? 'Launch as Learner' : 'Launch as Instructor'}
                  >
                    {isLearner ? <PiStudent aria-hidden /> : <GiTeacher aria-hidden />}
                  </DropdownToggle>
                  <DropdownMenu
                    id="launch-type-menu"
                    right
                  >
                    <DropdownItem
                      id="text-type-button"
                      onClick={() => setLearner(false)}
                    >
                      <div className="check-spacer">{!isLearner && <IoCheckmarkOutline />}</div>
                      Instructor Launch
                    </DropdownItem>
                    <DropdownItem
                      id="text-type-button"
                      onClick={() => setLearner(true)}
                    >
                      <div className="check-spacer">{isLearner && <IoCheckmarkOutline />}</div>
                      Learner Launch
                    </DropdownItem>
                  </DropdownMenu>
                </UncontrolledDropdown>
              )}
            </div>
            {edited && (
              <UncontrolledTooltip
                delay={0}
                placement="bottom"
                target={launchRef}
              >
                Save your changes in order to launch this tool.
              </UncontrolledTooltip>
            )}
          </Col>
        </Row>
      </div>
    </>
  );
};
