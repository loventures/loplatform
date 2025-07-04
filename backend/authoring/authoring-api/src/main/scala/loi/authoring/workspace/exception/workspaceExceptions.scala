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

package loi.authoring.workspace.exception

import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.edge.Group
import loi.cp.i18n.AuthoringBundle

import java.util.UUID

case class NoSuchNodeInWorkspaceException(name: UUID, commitId: Long)
    extends UncheckedMessageException(
      AuthoringBundle.message("workspace.noSuchNodeName", name.toString, long2Long(commitId))
    )

case class WorkspaceNodeNotFound(id: Long, commitId: Long)
    extends UncheckedMessageException(
      AuthoringBundle.message("workspace.nodeNotFound", long2Long(id), long2Long(commitId))
    )

case class WorkspaceEdgeNotFound(id: Long, commitId: Long)
    extends UncheckedMessageException(
      AuthoringBundle.message("workspace.edgeNotFound", long2Long(id), long2Long(commitId))
    )

case class NoRemoteInLayeredException(remote: Long)
    extends UncheckedMessageException(
      AuthoringBundle.message("workspace.noRemoteInLayered", long2Long(remote))
    )

case class NoMultiverseGroupInLayeredException(group: Group)
    extends UncheckedMessageException(
      AuthoringBundle.message("workspace.noMultiverseGroupInLayered", group.entryName)
    )
