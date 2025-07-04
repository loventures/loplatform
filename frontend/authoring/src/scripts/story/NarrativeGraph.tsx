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

import { sortBy } from 'lodash';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import Tree from 'react-d3-tree';
import { GiEvilTree } from 'react-icons/gi';

import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { NodeName } from '../types/asset';
import { SlimEdge } from '../types/edge';
import NarrativePresence from './NarrativeAsset/NarrativePresence';
import { useCurrentContextPath } from '../graphEdit';

const NarrativeGraph: React.FC = () => {
  const projectGraph = useProjectGraph();
  const contextPath = useCurrentContextPath();
  const structure = useMemo(() => {
    const loop = (name: NodeName, edge?: SlimEdge) => {
      const asset = projectGraph.nodes[name];
      const edges = projectGraph.outEdgesByNode2[name];
      const node = {
        name: asset.data.title,
        attributes: {
          typeId: asset.typeId,
          name: asset.name,
          ...(edge
            ? {
                group: edge.group,
                traverse: edge.traverse,
              }
            : {}),
        },
        children:
          (edge && !edge.traverse) || !edges?.length
            ? undefined
            : sortBy(edges, ['group', 'position']).map(e => loop(e.targetName, e)),
      };
      return node;
    };
    const name = contextPath.replace(/.*\./, '');
    return loop(name === projectGraph.homeNodeName ? projectGraph.rootNodeName : name);
  }, [projectGraph, contextPath]);
  const ref = useRef<HTMLDivElement>();
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  useEffect(() => {
    const x = ref.current?.getBoundingClientRect();
    if (x) setDimensions({ width: x.width, height: x.height });
  }, [ref.current]);

  return (
    <>
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto' }}>
        <div className="button-spacer d-flex align-items-center justify-content-center actions-icon">
          <GiEvilTree size="1.75rem" />
        </div>
        <h2 className="my-4 text-center">Project Graph</h2>
        <NarrativePresence name="graph">
          <div className="button-spacer d-flex align-items-center justify-content-center actions-icon"></div>
        </NarrativePresence>
      </div>

      <div
        id="treeWrapper"
        className="mt-5"
        style={{ height: 'calc(100vh - 10rem)' }}
        ref={ref}
      >
        <Tree
          data={structure}
          initialDepth={1}
          depthFactor={256}
          translate={{ x: 50, y: 200 }}
          pathClassFunc={datum => {
            const remote = datum.target.data.attributes.remote ? 'remote-edge' : '';
            const traverse = datum.target.data.attributes.traverse ? 'traverse-edge' : '';
            const group = `edge-${datum.target.data.attributes.group}`;
            return `${remote} ${traverse} ${group}`;
          }}
          dimensions={dimensions}
          shouldCollapseNeighborNodes
        />
      </div>
    </>
  );
};

export default NarrativeGraph;
