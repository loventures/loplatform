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

package loi.authoring.edge.service.exception

import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.cp.i18n.{AuthoringBundle, BundleMessage}

import java.util.{ResourceBundle, UUID}

object EdgeException:
  implicit private val bundleMessage: ResourceBundle = AuthoringBundle.bundle

  case class NoSuchEdge(idOrName: String)
      extends UncheckedMessageException(
        BundleMessage("edge.noSuchEdge", idOrName)
      )

  case class NoSuchEdgeId(edgeId: String)
      extends UncheckedMessageException(
        BundleMessage("edge.noSuchEdgeId", edgeId)
      )

  case class NoSuchGroup(group: String, sourceType: Option[AssetTypeId])
      extends UncheckedMessageException(
        /* annoyingly `load...RawSource` methods don't give us a source type */
        sourceType match
          case Some(st) => BundleMessage("edge.noSuchGroup", group, st.toString)
          case None     => BundleMessage("edge.noSuchGroup.unknownSourceType", group)
      )
  object NoSuchGroup:
    def apply(group: Group, source: Asset[?]): NoSuchGroup =
      NoSuchGroup(group, Some(source.info.typeId))

    def apply(group: Group, sourceType: Option[AssetTypeId]): NoSuchGroup =
      NoSuchGroup(group.entryName, sourceType)

  // TODO it would be nice if the target name was on here
  case class GroupDisallowsType(group: Group, typeId: AssetTypeId)
      extends UncheckedMessageException(
        BundleMessage("edge.groupDisallowsType", group.entryName, typeId.entryName)
      )

  case class UnreachableTargetType(srcTypeId: AssetTypeId, tgtTypeId: AssetTypeId)
      extends UncheckedMessageException(
        BundleMessage("edge.unreachableTargetType", srcTypeId.entryName, tgtTypeId.entryName)
      )

  case class DuplicateTargets(group: Group, duplicates: Map[UUID, Int])
      extends UncheckedMessageException(
        AuthoringBundle.message("edge.duplicateTargets", group.entryName, duplicates.mkString("[", ",", "]"))
      )

  case class MaximumGroupSizeExceeded(
    size: Long,
    maximumSize: Long,
    group: Group,
    sourceType: AssetTypeId,
  ) extends UncheckedMessageException(
        AuthoringBundle
          .message("edge.maximumGroupSizeExceeded", Long box size, Long box maximumSize, group.entryName, sourceType)
      )
end EdgeException
