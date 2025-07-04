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

import Loading from '../authoringUi/Loading';
import { useCurrentAssetName, useCurrentContextPath, useEditedAssetTypeId } from '../graphEdit';
import { NarrativeAsset } from '../story/NarrativeAsset';
import { useDiffCommit, useRevisionCommit } from '../story/storyHooks';
import { useProjectGraphLoading } from '../structurePanel/projectGraphHooks';
import { NarrativeDeleted } from './NarrativeDeleted';

export const RevisionMode: React.FC = () => {
  const name = useCurrentAssetName();
  const contextPath = useCurrentContextPath();
  const typeId = useEditedAssetTypeId(name);
  const commit = useRevisionCommit();
  const diff = useDiffCommit();
  const loading = useProjectGraphLoading();

  return (
    <div className="narrative-editor">
      <div className="py-0 py-lg-4">
        {loading ? (
          <Loading />
        ) : !typeId ? (
          <NarrativeDeleted />
        ) : (
          <NarrativeAsset
            key={`${name}-${commit}-${diff}`}
            name={name}
            contextPath={contextPath}
            mode="revision"
          />
        )}
      </div>
    </div>
  );
};
