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

import edgeRuleConstants from '../editor/EdgeRuleConstants';
import { NodeName, TypeId } from '../types/asset';
import { Aligner } from './AlignmentEditor/Aligner';

const competencyGroups = ['teaches', 'assesses'] as const;

export const AlignmentEditor: React.FC<{ name: NodeName; typeId: TypeId }> = ({ name, typeId }) => {
  const groups = competencyGroups.filter(group => !!edgeRuleConstants[typeId]?.[group]);
  return (
    <>
      {groups.map(group => (
        <Aligner
          key={group}
          name={name}
          typeId={typeId}
          group={group}
          multi={groups.length > 1}
        />
      ))}
    </>
  );
};
