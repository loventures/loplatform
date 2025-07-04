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

import { formatFullDate } from '../../dateUtil';
import { usePolyglot } from '../../hooks';
import { useProjectGraph } from '../../structurePanel/projectGraphActions';
import { NodeName, TypeId } from '../../types/asset';
import { NarrativeMode, storyTypeName } from '../story';
import { useDiffCommit, useRevisionHistory } from '../storyHooks';

// also spelunking history
export const RevisionTitle: React.FC<{
  name: NodeName;
  typeId: TypeId;
  mode: NarrativeMode;
  commit?: number;
}> = ({ name, typeId, mode, commit }) => {
  const polyglot = usePolyglot();
  const projectGraph = useProjectGraph();
  const diff = useDiffCommit();

  const revisions = useRevisionHistory(name);
  const date = useMemo(
    () =>
      mode === 'revision'
        ? revisions.find(c => !commit || commit === c.first)?.created
        : projectGraph.commit.created,
    [mode, revisions, commit, projectGraph]
  );

  return (
    <div className="d-flex align-items-center justify-content-center minw-0 text-sienna">
      {storyTypeName(polyglot, typeId)}
      {diff ? ' diff ' : ' as of '}
      {formatFullDate(date) ?? 'now'}
    </div>
  );
};
