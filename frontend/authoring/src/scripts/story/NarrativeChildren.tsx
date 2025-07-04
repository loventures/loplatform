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

import React, { useEffect, useMemo, useState } from 'react';
import VisibilitySensor from 'react-visibility-sensor';
import { Spinner } from 'reactstrap';

import { useAllEditedOutEdges, useEditedAssetTypeId } from '../graphEdit';
import { NodeName } from '../types/asset';
import { AddAsset } from './AddAsset';
import { NarrativeChild } from './NarrativeChild';
import { childEdgeGroup } from './story';
import { useNarrativeAssetState } from './storyHooks';

const ChunkSize = 10;

export const NarrativeChildren: React.FC<{
  name: NodeName;
  contextPath: string | undefined;
}> = ({ name, contextPath }) => {
  const typeId = useEditedAssetTypeId(name);
  const edgeGroup = childEdgeGroup(typeId);
  const allEdges = useAllEditedOutEdges(name);
  const fullList = useMemo(() => allEdges.filter(edge => edge.group === edgeGroup), [allEdges]);
  const subcontextPath = contextPath ? `${contextPath}.${name}` : name;
  const { renderAll } = useNarrativeAssetState(name);

  const [max, setMax] = useState(renderAll ? fullList.length : ChunkSize);
  const slimList = useMemo(() => fullList.slice(0, max), [fullList, max]);
  useEffect(() => {
    if (renderAll) setMax(fullList.length);
  }, [renderAll, fullList.length]);

  return (
    <div className="narrative-children">
      {!fullList.length && (
        <div className="d-flex">
          <div className="container flex-grow-1">
            <AddAsset
              parent={name}
              contextPath={subcontextPath}
              className="mb-3"
            />
          </div>
          <div className="flex-shrink-0 panel-sections feedback-width"></div>
        </div>
      )}
      {slimList.map(child => (
        <NarrativeChild
          key={child.name}
          parent={name}
          name={child.targetName}
          contextPath={subcontextPath}
        />
      ))}
      {slimList.length < fullList.length && (
        <VisibilitySensor
          onChange={visible => {
            if (visible) setMax(max => max + ChunkSize);
          }}
        >
          <div className="d-flex justify-content-center pt-4">
            <Spinner color="muted" />
          </div>
        </VisibilitySensor>
      )}
    </div>
  );
};
