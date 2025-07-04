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
import * as React from 'react';
import { IoEyeOutline } from 'react-icons/io5';
import { Button } from 'reactstrap';

import { usePolyglot } from '../../hooks';
import { getIcon } from '../../story/AddAsset';
import { Highlights } from '../../story/NarrativeSearch/Highlights';
import { storyTypeName } from '../../story/story';
import { SearchWebHit } from './types';

const HitRow: React.FC<{
  hit: SearchWebHit;
  isSelected: boolean;
  onClick: () => void;
  onPreview: () => void;
}> = ({ hit, isSelected, onClick, onPreview }) => {
  const asset = hit.path[0];
  const Icon = getIcon(asset.typeId as any);
  const polyglot = usePolyglot();
  return (
    <div
      className={classNames(
        'story-index-item',
        'd-flex',
        'align-items-center',
        'mx-3',
        `depth-1`,
        'gap-1',
        'search-hit-row',
        isSelected && 'selected'
      )}
      onClick={onClick}
    >
      <div className="d-flex flex-column gap-2 flex-grow-1 minw-0">
        <div className="d-flex align-items-center gap-2">
          <Icon
            className={classNames('flex-shrink-0 ', !isSelected && 'text-muted')}
            title={storyTypeName(polyglot, hit.path[0].typeId)}
          />
          <div className="a text-truncate flex-shrink-1 flex-grow-1">{asset.title}</div>
        </div>
        <Highlights className="search-highlights">{hit.highlights}</Highlights>
      </div>
      <Button
        size="sm"
        className="d-flex p-2 flex-shrink-0 border-0 br-50 unhover-bg-transparent"
        title="Preview Content"
        onClick={e => {
          e.stopPropagation();
          onPreview();
        }}
      >
        <IoEyeOutline size="1rem" />
      </Button>
    </div>
  );
};

export default HitRow;
