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

package loi.authoring.project

import cats.syntax.option.*
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import loi.authoring.branch.{Branch, BranchType}
import scaloi.syntax.localDateTime.*

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

final case class Project2(
  id: Long,
  name: String,
  head: Commit2,
  created: LocalDateTime,
  createdBy: Option[Long],
  ownedBy: Long,
  contributedBy: Map[Long, Option[String]],
  archived: Boolean,
  published: Boolean,
  code: Option[String],
  productType: Option[String],
  category: Option[String],
  subCategory: Option[String],
  revision: Option[Int],
  launchDate: Option[LocalDate],
  liveVersion: Option[String],
  s3: Option[String],
  configuration: Option[JsonNode],
  maintenance: Boolean,
) extends Project:
  // head's creator is included because it could be a superuser who isn't an owner/contributor
  override lazy val userIds: Set[Long] = contributedBy.keySet + ownedBy ++ head.createdBy.toSet

  override val homeName: UUID     = head.homeName
  override val rootName: UUID     = head.rootName
  override val homeNodeName: UUID = homeName
  override val rootNodeName: UUID = rootName

  @JsonIgnore
  lazy val asBranch: Branch = Branch(
    id,
    name,
    this.some,
    head.asLegacy,
    BranchType.Master,
    created.asDate,
    !archived,
    published,
  )
end Project2
