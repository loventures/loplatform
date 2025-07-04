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

package loi.authoring.search.web

import cats.instances.either.*
import cats.instances.list.*
import cats.instances.option.*
import cats.instances.try_.*
import cats.syntax.traverse.*
import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.asset.exception.NoSuchAssetType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.node.AssetNodeService
import loi.authoring.search.WorkspaceQuery
import loi.authoring.search.model.Ordering
import loi.authoring.search.web.exception.WebQueryParseException
import loi.authoring.workspace.ReadWorkspace
import loi.cp.i18n.AuthoringBundle
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*

import scala.util.Try

@Service
class WebQueryService(
  assetNodeService: AssetNodeService
):

  def validate(webQuery: GraphWebQuery, workspace: ReadWorkspace): Try[WorkspaceQuery] =

    for
      typeIds        <- validateAssetTypes(webQuery.typeIds)
      groupExclusion <- validateExcludeTargetsOf(webQuery.excludeTargetsOf, workspace)
      orderedBy      <- validateOrderedBy(webQuery.orderedBy)
      fields         <- validateFields(webQuery.fields)
    yield WorkspaceQuery(
      workspace,
      typeIds,
      webQuery.searchTerm,
      fields | Nil,
      webQuery.includeArchived.isTrue,
      groupExclusion,
      orderedBy,
      webQuery.firstResult,
      webQuery.limit
    )

  private def validateAssetTypes(assetTypes: Seq[String]): Try[Seq[AssetTypeId]] =
    assetTypes
      .map(AssetTypeId.withName)
      .toList
      .traverse(typeId => AssetType.types.get(typeId).toRight(NoSuchAssetType(typeId.entryName)))
      .toTry
      .recover { case ex: NoSuchAssetType => throw new WebQueryParseException(ex.getErrorMessage) }
      .map(_.map(_.id))

  private def validateExcludeTargetsOf(
    excludeTargetsOf: Option[ExcludeTargetsOf],
    workspace: ReadWorkspace
  ): Try[Option[(Asset[?], Group)]] =
    excludeTargetsOf traverse { case ExcludeTargetsOf(srcName, group) =>
      assetNodeService
        .load(workspace)
        .byName(srcName)
        .map(src =>
          if !src.assetType.edgeRules.contains(group) then
            throw new WebQueryParseException(AuthoringBundle.message("edge.NoSuchGroup", group))
          (src, group)
        )
    }

  private def validateOrderedBy(ordering: Option[Ordering]): Try[Option[Ordering]] =
    ordering traverse { ord =>
      (PropRe.matches(ord.propertyName) && DirectionRe.matches(ord.direction)) either ord `orFailure`
        new WebQueryParseException(AuthoringBundle.message("query.BadOrdering", ord))
    }

  private def validateFields(fields: Option[List[String]]): Try[Option[List[String]]] =
    fields traverse { fs =>
      fs traverse { field =>
        Fields.contains(field) either field `orFailure`
          new WebQueryParseException(AuthoringBundle.message("query.BadField", field))
      }
    }

  private final val PropRe      = "[a-zA-Z]+".r // could reduce this to named known properties
  private final val DirectionRe = "(?i)asc|desc".r
  private final val Fields      = Set("title", "subtitle", "keywords", "name")
end WebQueryService
