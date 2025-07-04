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

import React, { useMemo } from 'react';

import { useAllEditedInEdges, useGraphEditSelector } from '../graphEdit';
import { NodeName } from '../types/asset';
import { GateEl } from './GateInfo/GateEl';
import { GateIcon } from './icons/GateIcon';

// A remote asset may have inapplicable gates so I have to filter for content in the tree.
export const GateInfo: React.FC<{ name: NodeName }> = ({ name }) => {
  const allEdges = useAllEditedInEdges(name);
  const gates = useMemo(() => allEdges.filter(edge => edge.group === 'gates'), [allEdges]);
  const hasValidGates = useGraphEditSelector(state =>
    gates.some(gate => state.contentTree.contextPaths[gate.sourceName])
  );
  return hasValidGates ? (
    <div className="mb-4 gating d-flex align-items-center justify-content-center flex-wrap">
      <GateIcon size="1.4rem" />
      {gates.map((gate, idx) => (
        <GateEl
          key={gate.sourceName}
          gate={gate}
          last={idx === gates.length - 1}
        />
      ))}
    </div>
  ) : null;
};
