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

import { selectRouterQueryParam, useDcmSelector } from '../hooks';
import { ProjectGraph } from './projectGraphReducer';

export const useProjectGraphSelector = <A>(selector: (projectGraph: ProjectGraph) => A) =>
  useDcmSelector(state => selector(state.projectGraph));

export const useProjectGraphLoading = () =>
  useDcmSelector(state => {
    const commit = selectRouterQueryParam('commit')(state);
    const branchId = state.layout.branchId ? parseInt(state.layout.branchId) : 0;
    return (
      branchId <= 0 ||
      !state.projectGraph.homeNodeName ||
      state.projectStructure.fetchedCommit !== (commit ? parseInt(commit) : null)
    );
  });
