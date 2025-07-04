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

import { useEditedAssetContextPath } from '../../../graphEdit';
import { CommitAssetInfo } from '../../../revision/revision';
import { isQuestion } from '../../questionUtil';
import { editorUrl } from '../../story';

const MaxQuestion = 64;

export const OpLink: React.FC<{
  asset?: CommitAssetInfo;
  branch: number;
  commit?: number;
  diff?: number;
}> = ({ asset, branch, commit, diff }) => {
  const contextPath = useEditedAssetContextPath(asset?.name);
  const title = asset?.title;
  const assetTitle =
    title && isQuestion(asset?.typeId) && title.length > MaxQuestion
      ? title.substring(0, MaxQuestion) + '…'
      : (title ?? 'Unknown');
  if (!asset || !commit) return <span>{`"${assetTitle}"`}</span>;

  // If it has no contextPath it's not in the playlist so not in the project graph so I can only
  // view it by viewing a full prior commit rather than viewing just the revision history. Often
  // I will be going to the page revision history at a bogus commit because the page revision
  // history looks at modifications *to* a node; so if I have a link to a page added to a
  // parent, that commit (the add edge) didn't affect the page so won't be in the page commit
  // history so the UX goes wrong because it doesn't know what commit to highlight. If instead
  // I navigated to a commit date then I could highlight the next commit <= date.
  return (
    <>
      {!contextPath ? (
        <span>{`"${assetTitle}"`}</span>
      ) : (
        <Link to={editorUrl('story', branch, asset.name, contextPath)}>{`"${assetTitle}"`}</Link>
      )}
      {' ('}
      <Link
        to={
          !contextPath
            ? `/branch/${branch}/launch/${asset?.name}?commit=${commit}`
            : `/branch/${branch}/revision/${asset?.name}?commit=${commit}`
        }
      >
        history
      </Link>
      {diff && ', '}
      {diff && (
        <Link to={`/branch/${branch}/revision/${asset?.name}?commit=${commit}&diff=${diff}`}>
          diff
        </Link>
      )}
      {')'}
    </>
  );
};
