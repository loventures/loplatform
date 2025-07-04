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

import { getEditedAsset } from '../../graphEdit';
import { useDcmSelector } from '../../hooks';
import { ProjectAccessRights, allAccess, noAccess } from '../contentStatus';

// This has to give all access when the project graph is not yet loaded because otherwise
// the dcm routes will not be available immediately at startup and so if you refresh the
// browser you'll be bounced out of any protected route.
export const useProjectAccess = (): ProjectAccessRights =>
  useDcmSelector(({ projectGraph, graphEdits, layout, configuration }) => {
    const projectStatus = getEditedAsset(projectGraph.rootNodeName, projectGraph, graphEdits)?.data
      .projectStatus;
    const roleAccess = configuration.projectRightsByRoleAndStatus[layout.role];
    return layout.role && projectGraph.rootNodeName
      ? (roleAccess?.['*'] ?? roleAccess?.[projectStatus] ?? noAccess)
      : allAccess;
  });
