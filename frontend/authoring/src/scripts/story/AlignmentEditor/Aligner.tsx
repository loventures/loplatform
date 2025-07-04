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
import { map } from 'lodash';
import React, { useCallback, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import Select from 'react-select';

import { useFlatCompetencies } from '../../competency/useFlatCompetencies';
import { useAllEditedOutEdges } from '../../graphEdit';
import { NewAsset, NodeName, TypeId } from '../../types/asset';
import { EdgeGroup } from '../../types/edge';
import { isQuestion, toMultiWordRegex } from '../questionUtil';
import { scrollBottomIntoView } from '../story';
import { useIsEditable, useRemoteEditor } from '../storyHooks';
import { setAlignedAction } from './actions';

type SelectOption = {
  value: string;
  label: string;
  className: string;
  disabled: boolean;
};

const getCompetencyOptions = (
  flatCompetencies: NewAsset<any>[],
  disabled: Set<NodeName>
): SelectOption[] =>
  flatCompetencies.map(c => ({
    value: c.name,
    label: c.data.title,
    className: c.typeId.replace(/\..*/, ''),
    disabled: disabled.has(c.name),
  }));

export const stockClassnames = {
  control: () => 'Select-control',
  valueContainer: () => 'Select-value-container',
  placeholder: () => 'Select-placeholder',
  multiValue: () => 'Select-value',
  multiValueLabel: () => 'Select-value-label',
  multiValueRemove: () => 'Select-value-icon',
  input: () => 'Select-input',
  menu: () => 'Select-menu-outer',
  indicatorsContainer: () => 'Select-indicators-container',
};

export const Aligner: React.FC<{
  name: NodeName;
  typeId: TypeId;
  group: EdgeGroup;
  multi: boolean;
}> = ({ name, typeId, group, multi }) => {
  const dispatch = useDispatch();
  const editMode = useIsEditable(name, 'AlignContent');
  const divRef = useRef<HTMLDivElement>();
  const [editing, setEditing] = useState(false);
  const trailing = isQuestion(typeId);
  const remoteEditor = useRemoteEditor(name, group, editing);

  const flatCompetencies = useFlatCompetencies();

  const alignmentEdges = useAllEditedOutEdges(name);

  const aligned = useMemo(() => {
    return new Set(
      alignmentEdges.filter(edge => edge.group === group).map(edge => edge.targetName)
    );
  }, [alignmentEdges]);

  const competencyOptions = useMemo<SelectOption[]>(
    () => getCompetencyOptions(flatCompetencies, aligned),
    [flatCompetencies, aligned]
  );

  const alignment = useMemo(
    () => competencyOptions.filter(o => aligned.has(o.value)),
    [competencyOptions, aligned]
  );

  const onSetAligned = useCallback(
    (names: NodeName[]) => {
      dispatch(setAlignedAction(name, group, names, aligned));
    },
    [name, group, aligned]
  );

  return editing && editMode ? (
    <div
      ref={divRef}
      onKeyDown={e => setEditing(e.key !== 'Escape')}
    >
      <Select
        className={classNames('select-aligning', { trailing })}
        classNames={stockClassnames}
        value={alignment}
        onChange={option => onSetAligned(map(option, 'value'))}
        isMulti
        placeholder={`Learning objectives${multi ? ` (${group})` : ''}`}
        autoFocus
        openMenuOnFocus={true}
        menuPosition="fixed"
        isClearable={false}
        tabSelectsValue={false}
        options={competencyOptions}
        filterOption={(option, filter) => toMultiWordRegex(filter).test(option.label)}
        onBlur={() => setEditing(false)}
        onMenuOpen={() =>
          setTimeout(
            () =>
              scrollBottomIntoView(
                divRef.current?.getElementsByClassName('Select-menu-outer')[0] as HTMLElement
              ),
            0
          )
        }
      />
    </div>
  ) : (editMode && competencyOptions.length) || alignment.length ? (
    <div
      key="currently-aligned"
      className={classNames(
        'alignment feedback-context',
        { trailing },
        editMode && 'edit-mode',
        remoteEditor && 'remote-edit'
      )}
      data-id="alignment"
      tabIndex={editMode ? 0 : undefined}
      onClick={() => setEditing(true)}
      onFocus={() => setEditing(true)}
      data-field={group}
      style={remoteEditor}
    >
      {!alignment.length && (
        <div className="text-muted">{`Learning objectives${multi ? ` (${group})` : ''}`}</div>
      )}
      {alignment.map(competency => (
        <div key={competency.value}>{competency.label}</div>
      ))}
    </div>
  ) : null;
};
