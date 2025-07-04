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

import { take } from 'lodash';
import React from 'react';
import { Link } from 'react-router-dom';

import { usePolyglot } from '../../hooks';
import { SearchWebHit } from '../../modals/narrative/types';
import { getIcon } from '../AddAsset';
import { storyTypeName, truncateAssetTitle } from '../story';
import { Highlights } from './Highlights';

export const SearchRow: React.FC<{
  hit: SearchWebHit;
  global?: boolean;
}> = ({ hit, global }) => {
  const asset = hit.path[0];
  const Icon = getIcon(asset.typeId as any);
  const polyglot = usePolyglot();
  const context = hit.path.slice(1, -1).reverse();
  return (
    <div className="story-index-item mx-3 mx-lg-5 mb-3 depth-1 search-hit-row position-relative">
      <div className="d-flex align-items-center gap-2">
        <Icon
          className="flex-shrink-0 text-muted"
          title={storyTypeName(polyglot, hit.path[0].typeId)}
          size="1.5rem"
        />
        <div className="flex-shrink-1 minw-0">
          <div className="search-context text-truncate">
            {global && (
              <Link
                to={`/branch/${hit.branch}/story/${hit.course}`}
                className="course-context"
              >
                {hit.projectCode ? `${hit.projectCode}: ` : ''}
                {hit.projectName}
              </Link>
            )}
            {context.map((el, idx) => (
              <React.Fragment key={el.name}>
                {(global || idx > 0) && <span className="text-muted mx-1">/</span>}
                <Link
                  to={`/branch/${hit.branch}/story/${el.name}?contextPath=${take(context, idx)
                    .map(c => c.name)
                    .join('.')}`}
                >
                  {idx < context.length - 1 ? truncateAssetTitle(el.title, el.typeId) : el.title}
                </Link>
              </React.Fragment>
            ))}
          </div>
          <div className="d-flex">
            <Link
              to={`/branch/${hit.branch}/story/${hit.path[0].name}?contextPath=${context
                .map(c => c.name)
                .join('.')}`}
              className="regular-block-link text-truncate flex-shrink-1"
            >
              {asset.title}
            </Link>
          </div>
        </div>
      </div>
      <Highlights className="search-highlights mt-2">{hit.highlights}</Highlights>
    </div>
  );
};
