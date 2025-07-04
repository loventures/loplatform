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
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

import { trackAuthoringEvent } from '../../analytics';
import { trackNarrativeCollapse } from '../../analytics/AnalyticsEvents';
import { NodeName, TypeId } from '../../types/asset';
import { NarrativeMode } from '../story';
import { setNarrativeAssetState, setNarrativeInlineViewAction } from '../storyActions';

export const Jaggies: React.FC<{
  name: NodeName;
  typeId: TypeId;
  mode: NarrativeMode;
}> = ({ name, typeId, mode }) => {
  const dispatch = useDispatch();
  const toggleExpand = () => {
    if (mode === 'apex') {
      trackAuthoringEvent('Narrative Editor - Inline', `${typeId} - false`);
      dispatch(setNarrativeInlineViewAction(false));
    } else {
      dispatch(setNarrativeAssetState(name, { expanded: undefined, collapsed: true }));
      trackNarrativeCollapse();
    }
  };

  return (
    <Button
      color="transparent"
      block
      className="jag-btn p-0 m-0 border-0 d-flex "
      onClick={() => toggleExpand()}
      title="Collapse Content"
    >
      <svg
        viewBox="0 0 320 6"
        version="1.1"
        xmlns="http://www.w3.org/2000/svg"
      >
        <g>
          <path
            style={{
              fill: '#ffffff',
              strokeLinecap: 'butt',
              strokeLinejoin: 'miter',
            }}
            d="m 0,0 4.9999997,4.9999997 5,-4.9999997 4.9999993,4.9999997 5,-4.9999997 5,4.9999997 L 29.999998,0 l 5,4.9999997 L 39.999999,0 l 5,4.9999997 L 49.999997,0 54.999998,4.9999997 59.999998,0 64.999999,4.9999997 69.999997,0 l 5,4.9999997 L 79.999998,0 l 5,4.9999997 L 89.999996,0 94.999997,4.9999997 99.999997,0 105,4.9999997 110,0 115,4.9999997 120,0 125,4.9999997 130,0 135,4.9999997 140,0 145,4.9999997 149.99999,0 l 5,4.9999997 L 160,0 l 4.99999,4.9999997 5,-4.9999997 5,4.9999997 5,-4.9999997 5,4.9999997 5,-4.9999997 5,4.9999997 5,-4.9999997 5,4.9999997 5,-4.9999997 5,4.9999997 5,-4.9999997 5,4.9999997 5,-4.9999997 5,4.9999997 5,-4.9999997 5,4.9999997 5,-4.9999997 5,4.9999997 L 259.99999,0 265,4.9999997 269.99999,0 275,4.9999997 279.99999,0 284.99998,4.9999997 290,0 294.99999,4.9999997 299.99998,0 304.99999,4.9999997 309.99998,0 315,4.9999997 319.99999,0"
            id="path9531"
          />
        </g>
      </svg>
    </Button>
  );
};
