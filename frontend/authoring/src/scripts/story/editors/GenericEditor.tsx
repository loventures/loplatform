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

import { beginProjectGraphEdit, editProjectGraphNodeData } from '../../graphEdit';
import { usePolyglot } from '../../hooks';
import { HtmlPart, TypeId } from '../../types/asset';
import { PartEditor } from '../PartEditor';
import { NarrativeEditor, cap, storyTypeName } from '../story';
import { useIsEditable } from '../storyHooks';

export const GenericEditor: NarrativeEditor<any> = ({ asset, readOnly }) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();

  const editMode = useIsEditable(asset.name);

  const instructionsKey = instructionsField[asset.typeId];

  const typeName = cap(storyTypeName(polyglot, asset.typeId));
  const placeholder = polyglot.t('STORY_INSTRUCTIONS', { typeName });
  const uninstructable = !instructionsField[asset.typeId];

  const updateHtml = useCallback(
    (html: HtmlPart, session: string) => {
      dispatch(beginProjectGraphEdit('Edit instructions', session));
      dispatch(
        editProjectGraphNodeData(asset.name, {
          [instructionsKey]: {
            partType: 'block',
            parts: [html],
          },
        })
      );
    },
    [asset.name]
  );

  return uninstructable ? (
    <div className={classNames('uninstructable', 'text-muted', editMode && 'edit-mode')}>
      {polyglot.t(editMode ? 'STORY_UNEDITABLE' : 'STORY_UNVIEWABLE')}
    </div>
  ) : (
    <PartEditor
      id="instructions"
      asset={asset}
      part={asset.data[instructionsKey]}
      placeholder={placeholder}
      onChange={updateHtml}
      readOnly={readOnly}
    />
  );
};

const instructionsField: Partial<Record<TypeId, 'instructions' | 'prompt' | 'instructionsBlock'>> =
  {
    'assessment.1': 'instructions',
    'assignment.1': 'prompt',
    'checkpoint.1': 'instructions',
    'diagnostic.1': 'instructions',
    'observationAssessment.1': 'instructions',
    'lti.1': 'instructions',
    'poolAssessment.1': 'instructions',
    'resource.1': 'instructions',
    'discussion.1': 'instructionsBlock',
    'courseLink.1': 'instructions',
  };
