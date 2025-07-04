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

import React from 'react';

import { useEditedAsset, useGraphEditSelector } from '../../graphEdit';
import { useProjectGraph } from '../../structurePanel/projectGraphActions';
import { NodeName, TypeId } from '../../types/asset';
import { GenericEditor } from '../editors';
import { NarrativeMode } from '../story';
import { narrativeEditors } from './editors';

export const NarrativeEditorProxy: React.FC<{
  name: NodeName;
  typeId: TypeId;
  contextPath: string | undefined;
  mode: NarrativeMode;
  readOnly?: boolean;
}> = ({ name, typeId, contextPath, mode, readOnly }) => {
  const asset = useEditedAsset(name);
  const NarrativeEditor = narrativeEditors[typeId] ?? GenericEditor;
  const projectGraph = useProjectGraph();
  const generation = useGraphEditSelector(state => state.generation);

  return (
    <NarrativeEditor
      key={generation}
      asset={asset}
      contextPath={contextPath}
      mode={mode}
      projectGraph={projectGraph}
      readOnly={readOnly}
    />
  );
};
