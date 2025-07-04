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
import { useCollapse } from 'react-collapsed';
import { Badge } from 'reactstrap';

import { useRestoredAsset } from '../graphEdit';
import { NodeName } from '../types/asset';
import { AddAsset } from './AddAsset';
import { NarrativeAsset } from './NarrativeAsset';
import { NarrativeChildren } from './NarrativeChildren';
import { useNarrativeAssetState } from './storyHooks';

export const NarrativeChild: React.FC<{
  parent: NodeName;
  name: NodeName;
  contextPath: string;
}> = ({ parent, name, contextPath }) => {
  const { expanded, deleted } = useNarrativeAssetState(name);
  const { getCollapseProps } = useCollapse({
    defaultExpanded: true,
    isExpanded: !deleted,
  });
  // TODO: verify that this is actually real
  const restored = useRestoredAsset(name); // restored from prior commit(!)

  return restored ? (
    <div className="container narrative-container">
      <div className="story-element">
        <div className="asset-title d-flex align-items-baseline mb-0">
          <h4>{restored.data.title}</h4>
          <Badge
            color="warning"
            className="ms-2 text-dark"
          >
            Restored
          </Badge>
        </div>
      </div>
      <AddAsset
        parent={parent}
        contextPath={contextPath}
        after={name}
        className="mb-3"
      />
    </div>
  ) : (
    <div {...getCollapseProps()}>
      <NarrativeAsset
        key={name}
        name={name}
        contextPath={contextPath}
        mode="inline"
        bottom={!expanded}
      />
      {expanded && (
        <NarrativeChildren
          name={name}
          contextPath={contextPath}
        />
      )}
      {expanded && (
        <NarrativeAsset
          key={`${name}-bottom`}
          name={name}
          contextPath={contextPath}
          mode="inline"
          top={false}
        />
      )}
    </div>
  );
};
