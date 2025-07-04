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

package loi.cp.imports

import org.apache.pekko.actor.ActorRef
import cats.effect.IO
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import com.learningobjects.cpxp.async.async.AsyncSSEOperation
import com.learningobjects.cpxp.component.ComponentDescriptor
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import fs2.*
import loi.cp.imports.errors.GenericError
import scalaz.\/

/** A service for invoking & managing imports in Difference Engine. Imports invoked via this service are stored in the
  * database in a folder under the domain, and viewable on the "Manage Imports" administration page.
  */
@Service
trait ImportCoordinator:

  /** Takes a stream of items that could come from various sources (CsvFiles, JsonArrays, etc) and imports them.
    *
    * @param items
    *   A stream of items to import.
    * @param `type`
    *   The classname of the type of items, if any, in the import. Only applies to imports where all of the items are
    *   the same type of item. Examples are: UserImportItem CourseSectionImportItem, etc
    * @param invoker
    *   The party responsible for creating this import.
    * @return
    *   A left value if an exception interrupted the import, causing it to not finish, a right value if the import
    *   successfully ran. NOTE: Imports will not fail when one item fails validation. Imports will be considered to have
    *   'successfully' run even if every item failed validation.
    */
  def importStream(
    items: Stream[IO, GenericError \/ ImportItem],
    `type`: Option[String],
    invoker: UserDTO,
    identifier: Option[String],
    indexOffset: Option[Long] = None
  )(
    whenFinished: Throwable \/ ImportComponent => Unit
  ): ImportComponent

  /** Takes a stream of items that could come from various sources (CsvFiles, JsonArrays, etc) and imports them.
    * @param items
    *   A stream of items to import.
    * @param forEach
    *   a callback that will be invoked after every item is validated. the passed [[StreamStatusReport]] contains
    *   contextual information about the validated row (successful or not). This method is useful for live-updating user
    *   interfaces over a SSE stream.
    * @return
    *   A summary of the validation
    */
  def validateStream(
    items: Stream[IO, GenericError \/ ImportItem],
    forEach: Option[StreamStatusReport => Unit] = None,
    indexOffset: Option[Long] = None
  ): Stream[IO, StreamStatusReport]

  /** Retrieves a list of Import Types along with their human readable labels that are supported ImportItem types.
    * @return
    */
  def getImportTypes: Map[ImportType, Importer[?]]
end ImportCoordinator

case class ActorInfo[T](taskActor: ActorRef, operation: AsyncSSEOperation[T])

case class StreamStatusReport(
  status: ImportStatus,
  total: Long,
  completed: Long = 0,
  errorCount: Long = 0,
  errors: Seq[GenericErrorWrapper] = List()
)

/** Allows GenericError's i18n messages to be serialized when sent to the front end. Also holds index information.
  */
@JsonIgnoreProperties(Array("cd"))
case class GenericErrorWrapper(error: GenericError, lineNumber: Long)(
  @JsonProperty("cd") implicit val cd: ComponentDescriptor
):
  @JsonProperty("messages") def messages: Seq[String] = error.messages
