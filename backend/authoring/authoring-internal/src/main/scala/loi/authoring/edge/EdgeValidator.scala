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

package loi.authoring.edge

import loi.authoring.AssetType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.service.exception.EdgeException.*
import scaloi.syntax.OptionOps.*

import scala.util.{Failure, Success, Try}

object EdgeValidator:

  def groupRule[A](assetType: AssetType[A], group: Group): Try[EdgeRule] =
    assetType.edgeRules.get(group).toTry(NoSuchGroup(group, Some(assetType.id)))

  def checkRuleAllowsTargetTypes(
    rule: EdgeRule,
    targetTypes: Seq[AssetTypeId]
  ): Try[Unit] =
    targetTypes.find(typeId => !rule.typeIds.contains(typeId)) match
      case Some(tId) => Failure(GroupDisallowsType(rule.group, tId))
      case None      => Success(())
end EdgeValidator
