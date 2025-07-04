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

import { beginProjectGraphEdit, editProjectGraphNodeData } from '../../graphEdit';
import { usePolyglot } from '../../hooks';
import { NarrativeSettings, sentence } from '../story';
import { useFocusedRemoteEditor, useIsEditable } from '../storyHooks';

// Structural also offers:
// . Keywords
export const SurveySettings: NarrativeSettings<'survey.1'> = ({ asset }) => {
  const polyglot = usePolyglot();
  const editMode = useIsEditable(asset.name, 'EditSettings');

  const disabled = asset.data.disabled;
  const inline = asset.data.inline;
  const programmatic = asset.data.programmatic;

  const surveyKind = programmatic ? 'programmatic' : inline ? 'inline' : 'manual';

  return editMode ? (
    <div className="mx-3 mb-3 text-center d-flex justify-content-center form-inline gap-2 parameter-center">
      <SurveyKindEditor asset={asset} />
      <SurveyActiveEditor asset={asset} />
    </div>
  ) : (
    <div className="mx-3 mb-2 d-flex justify-content-center">
      <span className="input-padding text-muted text-center feedback-context">
        {sentence(
          polyglot.t(`STORY_survey_${surveyKind}`),
          polyglot.t(`STORY_disabled_${disabled}`)
        )}
      </span>
    </div>
  );
};

const SurveyKindEditor: NarrativeSettings<'survey.1'> = ({ asset }) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();

  const inline = asset.data.inline;
  const programmatic = asset.data.programmatic;

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'surveyKind'
  );

  const editKind = useCallback(
    (inl: boolean, pro: boolean) => {
      if (inl === inline && pro === programmatic) return;
      dispatch(beginProjectGraphEdit('Edit survey type', session));
      dispatch(editProjectGraphNodeData(asset.name, { inline: inl, programmatic: pro }));
    },
    [inline, programmatic, session]
  );

  const surveyKind = programmatic ? 'programmatic' : inline ? 'inline' : 'manual';

  return (
    <UncontrolledDropdown
      className={classNames('secret-input survey-kind-editor', remoteEditor && 'remote-edit')}
      onFocus={onFocus}
      onBlur={onBlur}
      style={remoteEditor}
    >
      <DropdownToggle
        color="transparent"
        caret
        className="secret-toggle"
      >
        {polyglot.t(`STORY_survey_${surveyKind}`)}
      </DropdownToggle>
      <DropdownMenu>
        <DropdownItem onClick={() => editKind(true, false)}>
          {polyglot.t(`STORY_survey_inline`)}
        </DropdownItem>
        <DropdownItem onClick={() => editKind(false, false)}>
          {polyglot.t(`STORY_survey_manual`)}
        </DropdownItem>
        <DropdownItem onClick={() => editKind(true, true)}>
          {polyglot.t(`STORY_survey_programmatic`)}
        </DropdownItem>
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};

export const SurveyActiveEditor: NarrativeSettings<'survey.1'> = ({ asset }) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();

  const disabled = asset.data.disabled;

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(asset.name, 'disabled');

  const editDisabled = useCallback(
    (dis: boolean) => {
      if (dis === disabled) return;
      dispatch(beginProjectGraphEdit('Edit active', session));
      dispatch(editProjectGraphNodeData(asset.name, { disabled: dis }));
    },
    [disabled, session]
  );

  return (
    <UncontrolledDropdown
      className={classNames('secret-input survey-active-editor', remoteEditor && 'remote-edit')}
      onFocus={onFocus}
      onBlur={onBlur}
      style={remoteEditor}
    >
      <DropdownToggle
        color="transparent"
        caret
        className="secret-toggle"
      >
        {polyglot.t(`STORY_disabled_${disabled}`)}
      </DropdownToggle>
      <DropdownMenu>
        {([false, true] as const).map(disabled => (
          <DropdownItem
            key={disabled.toString()}
            onClick={() => editDisabled(disabled)}
          >
            {polyglot.t(`STORY_disabled_${disabled}`)}
          </DropdownItem>
        ))}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
