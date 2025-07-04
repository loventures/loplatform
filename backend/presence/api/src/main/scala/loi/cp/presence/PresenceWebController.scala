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

package loi.cp.presence

import argonaut.CodecJson
import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight
import loi.cp.presence.SceneActor.SceneId
import loi.cp.sse.SseEvent
import scalaz.\/
import scaloi.json.ArgoExtras

import scala.concurrent.Future

/** Global API for managing presence sessions.
  *
  * This is not a pure REST API because the presence actors are local to the current http session.
  */
@Controller(value = "presence", root = true)
@RequestMapping(path = "presence")
trait PresenceWebController extends ApiRootComponent:
  import PresenceWebController.*

  /** Create a new presence.
    *
    * @param create
    *   the initial session data
    * @param http
    *   the http context
    * @return
    *   the future presence identifier
    */
  @RequestMapping(path = "sessions", method = Method.POST)
  def createPresence(@RequestBody create: ArgoBody[PresenceIn], http: HttpContext): Future[Unit \/ ArgoBody[PresenceId]]

  /** Get information about presence.
    *
    * @param presenceId
    *   the presence identifier
    * @param http
    *   the http context
    * @return
    *   the future session information
    */
  @RequestMapping(path = "sessions/{presenceId}", method = Method.GET)
  def getPresence(
    @PathVariable("presenceId") presenceId: String,
    http: HttpContext
  ): ErrorResponse \/ Future[ArgoBody[PresenceOut]]

  /** Update presence.
    *
    * @param presenceId
    *   the presence identifier
    * @param update
    *   the session update data
    * @param http
    *   the http context
    */
  @RequestMapping(path = "sessions/{presenceId}", method = Method.PUT)
  def updatePresence(
    @PathVariable("presenceId") presenceId: String,
    @RequestBody(log = false) update: ArgoBody[PresenceIn],
    http: HttpContext
  ): ErrorResponse \/ Unit

  /** Delete presence.
    *
    * @param presenceId
    *   the presence identifier
    * @param http
    *   the http context
    */
  @RequestMapping(path = "sessions/{presenceId}", method = Method.DELETE)
  def deletePresence(@PathVariable("presenceId") presenceId: String, http: HttpContext): ErrorResponse \/ Unit

  /** Delete presence via POST. Intended for use by browser sendBeacon.
    *
    * @param presenceId
    *   the presence identifier
    * @param http
    *   the http context
    */
  @RequestMapping(path = "sessions/{presenceId}/delete", method = Method.POST, csrf = false)
  def postlyDeletePresence(@PathVariable("presenceId") presenceId: String, http: HttpContext): ErrorResponse \/ Unit

  /** Stream the SSE events for a particular presence.
    *
    * @param presenceId
    *   the presence identifier
    * @param lastEventId
    *   the last-received SSE event id
    * @param http
    *   the http context
    * @return
    *   a future Web response
    */
  @RequestMapping(path = "sessions/{presenceId}/events", method = Method.GET, csrf = false)
  @Secured(allowAnonymous = true) // allow anonymous so we can fail the EventSource with a 404
  def presenceEvents(
    @PathVariable("presenceId") presenceId: String,
    @HttpHeader(LastEventId) lastEventId: Option[String],
    http: HttpContext
  ): ErrorResponse \/ WebResponse

  /** Poll for events from a particular presence.
    *
    * @param presenceId
    *   the presence identifier
    * @param lastEventId
    *   the last received event id
    * @param http
    *   the http context
    * @return
    *   a future containing a list of events
    */
  @RequestMapping(path = "sessions/{presenceId}/poll", method = Method.GET)
  @Secured(allowAnonymous = true) // allow anonymous so we can fail with a 404
  def pollPresence(
    @PathVariable("presenceId") presenceId: String,
    @HttpHeader(LastEventId) lastEventId: Option[String],
    http: HttpContext
  ): ErrorResponse \/ Future[ArgoBody[List[SseEvent]]]

  /** Testing hack: Send a maintenance message to everything.
    */
  @VisibleForTesting
  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "simulateMaintenance", method = Method.POST)
  def simulateMaintenance(): Unit
end PresenceWebController

/** Presence root API companion.
  */
object PresenceWebController:

  /** The SSE last event id header name. */
  private final val LastEventId = "Last-Event-ID"

  /** Wrapper for a presence identifier.
    *
    * @param presenceId
    *   the presence identifier
    */
  case class PresenceId(presenceId: String)
  object PresenceId:
    implicit val presenceIdCodec: CodecJson[PresenceId] =
      CodecJson.casecodec1(PresenceId.apply, ArgoExtras.unapply1)("presenceId")

  /** Inbound presence state.
    *
    * @param visible
    *   whether presence should be visible to other users
    * @param millisSinceActive
    *   number of milliseconds since last active
    * @param inScenes
    *   the scenes in which the user is present
    * @param followScenes
    *   the scenes in which the user is interested
    * @param lastEventId
    *   the last seen SSE event identifier
    */
  case class PresenceIn(
    visible: Boolean,
    millisSinceActive: Option[Long],
    activeMillis: Option[Long],
    inScenes: Option[List[SceneId]],
    followScenes: Option[List[SceneId]],
    lastEventId: Option[Long]
  )
  object PresenceIn:
    implicit val presenceInCodec: CodecJson[PresenceIn] =
      CodecJson.casecodec6(
        PresenceIn.apply,
        ArgoExtras.unapply
      )("visible", "millisSinceActive", "activeMillis", "inScenes", "followScenes", "lastEventId")

  /** Outbound presence state.
    *
    * @param visible
    *   whether presence is visible to other users
    * @param millisSinceActive
    *   number of millis since last active
    * @param inScenes
    *   the scenes in which the user is present
    */
  case class PresenceOut(
    visible: Boolean,
    millisSinceActive: Long,
    inScenes: Set[SceneId]
  )
  object PresenceOut:
    implicit val codec: CodecJson[PresenceOut] =
      CodecJson.casecodec3(
        PresenceOut.apply,
        ArgoExtras.unapply
      )("visible", "millisSinceActive", "inScenes")
end PresenceWebController
