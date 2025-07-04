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

import org.apache.pekko.actor.{Actor, ActorRef, ActorRefFactory, PoisonPill, Props, ReceiveTimeout, Terminated}
import argonaut.Argonaut.*
import argonaut.{CodecJson, DecodeJson, DecodeResult, EncodeJson}
import com.learningobjects.cpxp.scala.json.ArgoOps.*
import com.learningobjects.cpxp.service.presence.EventType
import loi.cp.presence.SceneActor.SceneId
import loi.cp.reference.EdgePath
import scalaz.{@@, Tag}
import scaloi.json.ArgoExtras
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.DateOps.*

import java.util.Date
import scala.collection.mutable
import scala.concurrent.duration.*

/** A scene actor aggregates the presence of all users who are active in the scene (course or activity context) and
  * broadcasts this to interested listeners.
  *
  * @param id
  *   the scene id
  */
class SceneActor(id: SceneId) extends Actor:
  import SceneActor.*

  /** Actors who are following this scene. */
  private val followers = mutable.Set.empty[ActorRef]

  /** Presence sessions that are active in this scene. */
  private val presences = mutable.Map.empty[ActorRef, PresenceInScene]

  /** Schedule a periodic timeout check at startup.
    */
  override def preStart(): Unit =
    context.setReceiveTimeout(ExpireTimeout)

  /** The actor message handler.
    */
  override val receive: Receive = {
    case Follow                   => onFollow()
    case Unfollow                 => onUnfollow()
    case session: PresenceInScene => onPresenceInScene(session)
    case LeaveScene               => onLeaveScene()
    case message: DeliverMessage  => onDeliverMessage(message)
    case Terminated(actor)        => onTerminated(actor)
    case ReceiveTimeout           => onReceiveTimeout()
  }

  /** Add a new follower.
    */
  private def onFollow(): Unit =
    if followers add sender() then
      context watch sender()
      sender() ! PresenceActor.DeliverMessage(ScenePresence(id, userList))

  /** Remove a follower.
    */
  private def onUnfollow(): Unit =
    if (followers remove sender()) && !(presences contains sender()) then context unwatch sender()

  /** Handle when a user's presence is reported in.
    *
    * @param presence
    *   the presence information
    */
  private def onPresenceInScene(presence: PresenceInScene): Unit =
    // TODO: changes in just active time from multiple users should be batched up
    val existing = presences.put(sender(), presence)
    // watch new sessions
    if existing.isEmpty then context watch sender()
    // broadcast visible presence changes
    if existing.forall(e => different(e, presence)) then broadcastState()

  /** Handle when a user leaves a scene.
    */
  private def onLeaveScene(): Unit =
    val existing = presences.remove(sender())
    // unwatch known sessions
    if existing.nonEmpty && !(followers contains sender()) then context unwatch sender()
    // broadcast visible presence changes
    if existing.exists(_.visible) then broadcastState()

  /** Handle a deliver message request.
    *
    * @param message
    *   the message to forward
    */
  private def onDeliverMessage(message: DeliverMessage): Unit =
    presences.keys foreach { presence =>
      logger.debug(s"SceneActor $self deliver $message to presence $presence from session for scene $id")
      presence forward message
    }

  /** Handle when a presence session or follower dies.
    *
    * @param actor
    *   the dead actor
    */
  private def onTerminated(actor: ActorRef): Unit =
    followers remove actor
    if presences.remove(actor).isDefined then broadcastState()

  /** Expired idle sessions. This is mostly a protection against loss or Terminated messages.
    */
  private def onReceiveTimeout(): Unit =
    // TODO: is 1 hour really the right interval for a suicide check?
    val now = new Date()
    presences.filterInPlace { case (actor, sessionInScene) =>
      (now - sessionInScene.lastReported < ExpireTimeout) <|! {
        // unwatch any actor that we drop
        if !(followers contains sender()) then context unwatch actor
      }
    }
    broadcastState()
    suicideCheck()
  end onReceiveTimeout

  /** Poison this scene if it has no sessions.
    */
  private def suicideCheck(): Unit =
    if presences.isEmpty then self ! PoisonPill

  /** Broadcast presence of all the visible users in this scene.
    */
  private def broadcastState(): Unit =
    val s = PresenceActor.DeliverMessage(ScenePresence(id, userList))
    followers foreach { _ ! s }

  /** Get the visible users present in this scene.
    *
    * @return
    *   the visible users and their presence
    */
  private def userList: Seq[UserLastActive] =
    val now = new Date().getTime
    presences.values
      .filter(_.visible)
      .groupBy(_.userHandle)
      .toSeq
      .map { case (handle, presences) =>
        // In which we only report your presence one time to the world, so your
        // singular location is that of the most-recently active tab...
        val latest = presences.maxBy(_.lastActive)
        (handle, now - latest.lastActive.getTime, latest.location)
      }
  end userList
end SceneActor

/** The scene actor companion.
  */
object SceneActor:

  sealed trait SceneId

  final case class InContext(context: Long) extends SceneId

  object InContext:
    implicit val codec: CodecJson[InContext] =
      CodecJson.casecodec1(
        InContext.apply,
        ArgoExtras.unapply1
      )("context")

  final case class InContextWithEdgePath(context: Long, edgePath: EdgePath, assetId: Long) extends SceneId

  object InContextWithEdgePath:
    implicit val codec: CodecJson[InContextWithEdgePath] =
      CodecJson.casecodec3(
        InContextWithEdgePath.apply,
        ArgoExtras.unapply
      )("context", "edgePath", "assetId")

  final case class InBranch(branch: Long, asset: Option[String]) extends SceneId

  object InBranch:
    implicit val codec: CodecJson[InBranch] =
      CodecJson.casecodec2(
        InBranch.apply,
        ArgoExtras.unapply
      )("branch", "asset")

  object SceneId:
    implicit val codec: CodecJson[SceneId] =
      val encode = EncodeJson[SceneId] {
        case c: InContext                => c.asJson
        case cwep: InContextWithEdgePath => cwep.asJson
        case b: InBranch                 => b.asJson
      }
      val decode = DecodeJson[SceneId] { a =>
        (a.as[InContextWithEdgePath].upTo[SceneId] ||| a.as[InContext].upTo[SceneId] ||| a.as[InBranch].upTo[SceneId])
          .withMessage(s"scene ID ${a.focus} is not valid")
      }
      CodecJson.derived[SceneId](using encode, decode)
    end codec
  end SceneId

  /** Raise a decode result up to an upper type. */
  implicit class DecodeResultOps[A](private val self: DecodeResult[A]) extends AnyVal:
    def upTo[AA >: A]: DecodeResult[AA] = self.map(a => a: AA)

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** A reference to a scene actor. */
  type Ref = ActorRef @@ SceneActor

  /** Create a scene actor.
    *
    * @param id
    *   the scene id
    * @param factory
    *   the actor factory
    * @return
    *   the scene actor
    */
  def create(id: SceneId)(implicit factory: ActorRefFactory): Ref =
    Tag.of[SceneActor] {
      factory.actorOf(Props(new SceneActor(id)))
    }

  /** Return if two presences are observably different. Either their visibility is different or they are visible and
    * different.
    *
    * @param s1
    *   the first presence
    * @param s2
    *   the second presence
    * @return
    *   whether two presences are observably different
    */
  private def different(s1: PresenceInScene, s2: PresenceInScene): Boolean =
    (s1.visible != s2.visible) || s2.visible && (s1.lastActive != s2.lastActive || s1.location != s2.location)

  /** A follow request. The sender() will receive {PresenceInScene} messages.
    */
  case object Follow

  /** An unfollow request. The sender() will no longer receive scene messages.
    */
  case object Unfollow

  /** Inbound information about a user's presence in a scene.
    *
    * @param userHandle
    *   the user handle
    * @param lastActive
    *   the last active time
    * @param visible
    *   the user visibility
    * @param lastReported
    *   when the client last reported in
    * @param location
    *   optional opaque location
    */
  case class PresenceInScene(
    userHandle: String,
    lastActive: Date,
    visible: Boolean,
    lastReported: Date,
    location: Option[String]
  )

  /** Inbound notification that a user left the scene.
    */
  case object LeaveScene

  /** A pair of a user handle and their last activity time. */
  type UserLastActive = (String, Long, Option[String]) // compact structure for space-efficient serialization?

  /** Broadcast information about users in a scene.
    *
    * @param scene
    *   the scene id
    * @param users
    *   the users and their presence
    */
  case class ScenePresence(scene: SceneId, users: Seq[UserLastActive])

  /** Scene presence companion. */
  object ScenePresence:

    /** Scene presence event type. */
    final val Type = "ScenePresence"

    /** Scene present event type evidence. */
    implicit val ScenePresenceEventType: EventType[ScenePresence] = EventType(Type)

  /** Deliver a message onto all the presence actors. This is just an alias for the type in PresenceActor.
    */
  type DeliverMessage = PresenceActor.DeliverMessage

  /** How much idle time should cause a session to be expired.
    */
  private final val ExpireTimeout = 1.hour
end SceneActor
