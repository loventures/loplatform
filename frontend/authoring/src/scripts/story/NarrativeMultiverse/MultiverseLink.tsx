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
import React from 'react';
import { Link } from 'react-router-dom';

import { TreeAsset } from '../../graphEdit';
import { useBranchId } from '../../hooks';
import { getIcon } from '../AddAsset';
import { Stornado } from '../badges/Stornado';
import { isQuestion } from '../questionUtil';
import { editorUrl } from '../story';

// Should this offer to link to both the in-project and extra-project version?
export const MultiverseLink: React.FC<{ asset: TreeAsset }> = ({ asset }) => {
  const branchId = useBranchId();
  const Icon = isQuestion(asset.typeId) ? undefined : getIcon(asset.typeId);

  return (
    <Link
      to={editorUrl('story', branchId, asset, asset.context)}
      className={classNames(
        'story-index-item d-flex align-items-center text-decoration-none',
        `depth-${asset.depth + 1}`,
        `story-nav-${asset.typeId.replace(/\..*/, '')}`
      )}
      style={/*!asset.edge.remote ? { opacity: 0.6 } : */ undefined}
    >
      {Icon && <Icon className="text-muted me-1" />}
      <span className="hover-underline flex-shrink-1 text-truncate">
        {asset.edge.group === 'questions' ? (
          <>
            {`Question ${asset.index + 1} – `}
            <span className="unhover-muted">{asset.data.title || 'Untitled'}</span>
          </>
        ) : (
          asset.data.title
        )}
      </span>
      <Stornado name={asset.name} />
    </Link>
  );
};
