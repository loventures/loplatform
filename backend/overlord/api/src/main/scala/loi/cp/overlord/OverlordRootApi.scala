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

package loi.cp.overlord

import java.util.Date
import java.util.logging.Level

import javax.validation.groups.Default
import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, FileResponse, Method}
import com.learningobjects.cpxp.scala.json.SimpleConverter
import com.learningobjects.de.authorization.Secured
import com.learningobjects.de.web.{Queryable, QueryableId}
import loi.cp.user.UserComponent

import scala.concurrent.Future

/** Root controller for various overlord things. */
@Controller(value = "overlord", root = true)
@RequestMapping(path = "overlord")
@Secured(Array(classOf[OverlordRight]))
trait OverlordRootApi extends ApiRootComponent:
  @RequestMapping(path = "logs/byGuid/{guid}", method = Method.GET)
  def logsByGuid(@PathVariable("guid") guid: String): Future[String]

  @RequestMapping(path = "logs/byAge/{age}", method = Method.GET)
  def logsByAge(@PathVariable("age") age: Long): Future[String]

  @RequestMapping(path = "logs/logLevelChange", method = Method.POST)
  def logLevelAlter(@RequestBody level: AlterLevel): Unit

  @RequestMapping(path = "logs/download/{fileId}", method = Method.GET)
  def retrieveLogs(@PathVariable("fileId") fileId: String): Option[FileResponse[?]]

  @RequestMapping(path = "bannedIps", method = Method.GET)
  def getBannedIps(): ApiQueryResults[String]

  @RequestMapping(path = "bannedIps", method = Method.POST)
  def banIp(@RequestBody ip: String): Unit

  @RequestMapping(path = "bannedIps/delete", method = Method.POST)
  def unbanIp(@RequestBody ip: String): Unit

  // If we promote undelete to system admin, expose this request mapping
  // as a proper root component.

  @RequestMapping(path = "trashRecords", method = Method.GET)
  def getTrashRecords(query: ApiQuery): ApiQueryResults[TrashRecordComponent]

  @RequestMapping(path = "trashRecords/{id}/restore", method = Method.POST, async = true)
  def restoreTrashRecord(@PathVariable("id") id: Long): Unit
end OverlordRootApi

@Schema("trashRecord")
trait TrashRecordComponent extends ComponentInterface with QueryableId:
  import TrashRecordComponent.*

  @JsonProperty(trashIdProperty)
  @JsonView(Array(classOf[Default]))
  @Queryable
  def getTrashId: String

  @JsonProperty(createdProperty)
  @JsonView(Array(classOf[Default]))
  @Queryable
  def getCreated: Date

  @JsonProperty(creatorIdProperty)
  @JsonView(Array(classOf[Default]))
  @Queryable
  def getCreatorId: Long

  @RequestMapping(path = "creator", method = Method.GET)
  @Queryable(name = creatorProperty, joinComponent = classOf[UserComponent])
  def getCreator: UserComponent
end TrashRecordComponent

object TrashRecordComponent:
  final val trashIdProperty   = "trashId"
  final val createdProperty   = "created"
  final val creatorIdProperty = "creator_id"
  final val creatorProperty   = "creator"

final case class AlterLevel(
  name: String,
  @JsonDeserialize(converter = classOf[AlterLevel.Deserialize]) @JsonSerialize(
    converter = classOf[AlterLevel.Serialize]
  ) level: Level,
  expiresIn: Option[Int],
  allNodes: Boolean
)

object AlterLevel:
  final class Deserialize extends SimpleConverter((s: String) => Level.parse(s))
  final class Serialize   extends SimpleConverter((l: Level) => l.toString)
