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
import React, { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { trackNarrativeNavHandler } from '../../analytics/AnalyticsEvents';
import { useAllEditedOutEdges, useEditedAssetTitle, useEditedAssetTypeId } from '../../graphEdit';
import { NodeName } from '../../types/asset';
import AssetDropdownItem from '../components/AssetDropdownItem';
import { childEdgeGroup, contextPathQuery, truncateAssetTitle } from '../story';
import { useRevisionCommit } from '../storyHooks';

export const ParentCrumb: React.FC<{
  grandparentName: NodeName;
  parentName: NodeName;
  contextPath: string;
}> = ({ grandparentName, parentName, contextPath }) => {
  const parentTitle = useEditedAssetTitle(parentName);
  const parentTypeId = useEditedAssetTypeId(parentName);
  const grandparentTypeId = useEditedAssetTypeId(grandparentName);
  const edgeGroup = childEdgeGroup(grandparentTypeId);
  const allEdges = useAllEditedOutEdges(grandparentName);
  const aunts = useMemo(
    () => allEdges.filter(edge => edge.group === edgeGroup),
    [allEdges, edgeGroup]
  );
  const commit = useRevisionCommit();
  const cp1 = contextPathQuery(contextPath, commit);

  return (
    <>
      <Link
        to={parentName + cp1}
        onClick={trackNarrativeNavHandler('Parent')}
        className="minw-0 text-truncate parent-crumb"
      >
        {truncateAssetTitle(parentTitle, parentTypeId)}
      </Link>
      {!!aunts?.length && (
        <UncontrolledDropdown>
          <DropdownToggle
            color="transparent"
            className="carrot text-muted down"
            title="Siblings"
            id="parent-dropdown"
          >
            ▾
          </DropdownToggle>
          <DropdownMenu
            strategy="fixed"
            style={{ maxHeight: '75vh', overflow: 'auto' }}
          >
            {aunts.map(aunt => (
              <AssetDropdownItem
                key={aunt.name}
                tag={Link}
                disabled={aunt.targetName === parentName}
                to={aunt.targetName + cp1}
                onClick={trackNarrativeNavHandler('Aunt')}
                name={aunt.targetName}
              />
            ))}
          </DropdownMenu>
        </UncontrolledDropdown>
      )}
      <span className={classNames('text-muted me-2', !aunts?.length && 'ms-2')}>/</span>
    </>
  );
};
