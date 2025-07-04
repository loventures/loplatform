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

package loi.authoring.search

import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.search.model.Ordering
import loi.authoring.workspace.ReadWorkspace

import scala.jdk.CollectionConverters.*

case class WorkspaceQuery(
  workspace: ReadWorkspace,
  typeIds: Seq[AssetTypeId] = Seq.empty,
  searchTerm: Option[String] = None,
  fields: List[String] = Nil,
  includeArchived: Boolean = false,
  groupExclusion: Option[(Asset[?], Group)] = None,
  ordering: Option[Ordering] = None,
  offset: Option[Long] = None,
  limit: Option[Long] = None
)

object WorkspaceQuery:

  def applyJava(
    workspace: ReadWorkspace,
    typeIds: java.util.List[AssetTypeId],
    searchTerm: String
  ): WorkspaceQuery =
    WorkspaceQuery(
      workspace,
      typeIds.asScala.toSeq,
      Some(searchTerm)
    )
end WorkspaceQuery
