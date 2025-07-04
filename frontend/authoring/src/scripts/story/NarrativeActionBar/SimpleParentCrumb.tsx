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
import { Link } from 'react-router-dom';

import { trackNarrativeNavHandler } from '../../analytics/AnalyticsEvents';
import { useEditedAssetTitle, useEditedAssetTypeId } from '../../graphEdit';
import { NodeName } from '../../types/asset';
import { contextPathQuery, truncateAssetTitle } from '../story';
import { useRevisionCommit } from '../storyHooks';

export const SimpleParentCrumb: React.FC<{
  parentName: NodeName;
  contextPath: string;
}> = ({ parentName, contextPath }) => {
  const parentTitle = useEditedAssetTitle(parentName);
  const parentTypeId = useEditedAssetTypeId(parentName);
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
      <span className="text-muted me-2 ms-2">/</span>
    </>
  );
};
