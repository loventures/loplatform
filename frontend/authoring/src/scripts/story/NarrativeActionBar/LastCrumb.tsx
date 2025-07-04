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
import { Link } from 'react-router-dom';
import { DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { trackNarrativeNavHandler } from '../../analytics/AnalyticsEvents';
import {
  useAllEditedOutEdges,
  useEditedAssetTitle,
  useEditedAssetTypeId,
  useGraphEditSelector,
} from '../../graphEdit';
import { useDocumentTitle, useRouterPathVariable } from '../../hooks';
import { NodeName } from '../../types/asset';
import AssetDropdownItem from '../components/AssetDropdownItem';
import { childEdgeGroup, contextPathQuery, subPageNames } from '../story';
import { useRevisionCommit } from '../storyHooks';

export const LastCrumb: React.FC<{ parentName: NodeName; name: NodeName; contextPath: string }> = ({
  parentName,
  name,
  contextPath,
}) => {
  const current = useRouterPathVariable('name');
  const parentTypeId = useEditedAssetTypeId(parentName);
  const edgeGroup = childEdgeGroup(parentTypeId);
  const questionType = edgeGroup === 'questions';
  const title = useEditedAssetTitle(name);
  const allEdges = useAllEditedOutEdges(parentName);
  const siblings = useMemo(
    () => allEdges.filter(edge => edge.group === edgeGroup),
    [allEdges, edgeGroup]
  );
  const fullPath = contextPath ? `${contextPath}.${name}` : name;
  const page = useGraphEditSelector(graphEdits => graphEdits.contentTree.page[fullPath]);
  const commit = useRevisionCommit();
  const cp0 = contextPathQuery(contextPath, commit);

  const pageTitle = subPageNames[current]
    ? subPageNames[current]
    : questionType
      ? `Question ${page}`
      : title;
  useDocumentTitle(pageTitle);

  return (
    <>
      <span className="text-truncate minw-0 final-crumb">{pageTitle}</span>
      {!subPageNames[current] && siblings?.length ? (
        <UncontrolledDropdown>
          <DropdownToggle
            color="transparent"
            className="carrot text-muted down"
            title="Siblings"
            id="final-dropdown"
          >
            ▾
          </DropdownToggle>
          <DropdownMenu
            strategy="fixed"
            style={{ maxHeight: '75vh', overflow: 'auto' }}
          >
            {siblings.map((sibling, index) => (
              <AssetDropdownItem
                key={sibling.name}
                tag={Link}
                disabled={sibling.targetName === name}
                to={sibling.targetName + cp0}
                onClick={trackNarrativeNavHandler('Sibling')}
                name={questionType ? undefined : sibling.targetName}
                label={questionType ? `Question ${index + 1}` : undefined}
              />
            ))}
          </DropdownMenu>
        </UncontrolledDropdown>
      ) : null}
    </>
  );
};
