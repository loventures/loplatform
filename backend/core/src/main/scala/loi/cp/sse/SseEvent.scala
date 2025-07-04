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

package loi.cp.sse

import argonaut.CodecJson
import scaloi.json.ArgoExtras

/** Simple data model for SSE events.
  *
  * @param id
  *   the optional event identifier
  * @param event
  *   the optional event type
  * @param data
  *   the optional event data
  */
case class SseEvent(id: Option[String], `type`: Option[String], data: Option[String]):

  /** Get the event identifier.
    *
    * @return
    *   the event identifier, or null
    */
  def getId: String = id.orNull

  /** Get the event type.
    *
    * @return
    *   the event type, or null
    */
  def getType: String = `type`.orNull

  /** Get the event data.
    *
    * @return
    *   the event data, or null
    */
  def getData: String = data.orNull
end SseEvent

/** SSE event companion.
  */
object SseEvent:

  implicit val codec: CodecJson[SseEvent] =
    CodecJson.casecodec3(new SseEvent(_, _, _), ArgoExtras.unapply)("id", "type", "data")

  /** Create an SSE event from nullable values.
    *
    * @param id
    *   the event identifier, or null
    * @param event
    *   the event type, or null
    * @param data
    *   the event data, or null
    * @return
    *   the SSE event
    */
  def apply(id: String, event: String, data: String): SseEvent =
    SseEvent(Option(id), Option(event), Option(data))

  /** Create an SSE event from nullable type and data.
    *
    * @param event
    *   the event type, or null
    * @param data
    *   the event data, or null
    * @return
    *   the SSE event
    */
  def apply(event: String, data: String): SseEvent =
    SseEvent(None, Some(event), Some(data))

  /** The SSE event identifier signaling a channel close.
    */
  val CloseId = "CLOSE"

  /** An SSE channel close event.
    */
  val Close = SseEvent(Some(CloseId), None, None)
end SseEvent
