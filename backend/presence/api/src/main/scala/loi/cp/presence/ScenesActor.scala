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

import org.apache.pekko.actor.{Actor, ActorRef, Props, Terminated}
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import com.learningobjects.cpxp.scala.actor.{ClusterActors, CpxpActorSystem}
import loi.cp.presence.SceneActor.{InBranch, InContextWithEdgePath, SceneId}
import scalaz.syntax.tag.*
import scalaz.{@@, Tag}
import scaloi.data.SetDelta
import scaloi.syntax.AnyOps.*

import java.util.Date
import scala.collection.mutable

/** The scenes actor. This is a singleton actor responsible for the management of all scene actors in the system.
  */
class ScenesActor extends Actor:
  import ScenesActor.*

  /** A map of scene ids to the associated scene actors. New actors are created on demand.
    */
  val sceneActors: mutable.Map[SceneId, SceneActor.Ref] =
    mutable.Map.empty.withDefault(createScene)

  /** The actor message handler.
    */
  override val receive: Receive = {
    case GetScene(id)                          => onGetScene(id)
    case pis: PresenceInScenes                 => onPresenceInScenes(pis)
    case DeliverMessage(messageType, body, id) => onDeliverMessage(messageType, body, id)
    case Terminated(actor)                     => onTerminated(actor)
  }

  /** Get a scene actor.
    *
    * @param id
    *   the scene id
    */
  private def onGetScene(id: SceneId): Unit =
    sender() ! SceneRef(id, sceneActors(id))

  /** Handle when a session registers the user's presence in a set of scenes. This updates all the relevant scene actors
    * with the user's presence.
    *
    * @param pis
    *   the session message
    */
  private def onPresenceInScenes(pis: PresenceInScenes): Unit =
    // Unfollow scenes the user no longer cares about
    pis.followScenes.remove foreach { id =>
      sceneActors.get(id) foreach { ctx =>
        ctx `forward` SceneActor.Unfollow
      }
    }
    // Unsubscribe from scenes the user has left
    pis.unscenes foreach { id =>
      sceneActors.get(id) foreach { ctx =>
        ctx `forward` SceneActor.LeaveScene
      }
    }
    // Back in the day we maintained presence within each course/edge-path so that
    // we could have collaborative activities but that's not a thing so we're now
    // just tracking presence in the major location and a location which is the
    // singular thing you're viewing. For now I'm discarding that for courses because
    // it seems illegit to show students where each other is in the course until
    // we decide that's a thing. Note that the time-on-task does effectively the same
    // thing but in its own special way.
    val (locations, principalScenes) = pis.scenes.foldLeft(Map.empty[SceneId, String] -> List.empty[SceneId]) {
      case ((locations, scenes), InBranch(branch, Some(location)))            =>
        (locations + (InBranch(branch, None) -> location)) -> scenes
      case ((locations, scenes), InContextWithEdgePath(context, edgePath, _)) =>
        locations -> scenes // (locations + (InContext(context) -> edgePath.toString)) -> scenes
      case ((locations, scenes), id)                                          =>
        locations -> (id :: scenes)
    }
    // Register with scenes the user is active in
    principalScenes foreach { id =>
      val state =
        SceneActor.PresenceInScene(pis.userHandle, pis.lastActive, pis.visible, pis.lastReported, locations.get(id))
      sceneActors(id) `forward` state
    }
    // Follow scenes the user is interested in
    pis.followScenes.add foreach { id =>
      sceneActors(id) `forward` SceneActor.Follow
    }
  end onPresenceInScenes

  /** Deliver a message to a scene.
    *
    * @param eventType
    *   the event type
    * @param message
    *   the message
    * @param id
    *   the scene id
    */
  private def onDeliverMessage(eventType: String, message: Any, id: SceneId): Unit =
    sceneActors.get(id) foreach { scene =>
      logger.debug(s"ScenesActor $self deliver $eventType / $message to scene $id / $scene")
      scene `forward` PresenceActor.DeliverMessage(eventType, message)
    }

  /** Unmap a scene actor upon its death.
    *
    * @param actor
    *   the scene actor
    */
  private def onTerminated(actor: ActorRef): Unit =
    sceneActors.filterInPlace { case (_, v) => v.unwrap != actor }
    ()

  /** Create and watch a scene actor.
    *
    * @param id
    *   the scene id
    * @return
    *   the new scene actor
    */
  private def createScene(id: SceneId): SceneActor.Ref =
    SceneActor.create(id) <| { actor =>
      context `watch` actor
      sceneActors += id -> actor
    }
end ScenesActor

/** The scenes actor companion.
  */
object ScenesActor:

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** Scenes actor reference. */
  type Ref = ActorRef @@ ScenesActor

  /** Properties to create a new scenes actor. */
  val props: Props = Props(new ScenesActor)

  /** The singleton scenes actor. */
  lazy val clusterActor: Ref =
    Tag.of[ScenesActor](ClusterActors.singleton(props, "scenes")(using CpxpActorSystem.system))
  // TODO: should this shard by context... should there by a per-domain contexts actor.. singletons bad..

  /** Request to get a reference to a scene actor. The actor will be created if it does not exist. No validation of the
    * scene's id is done.
    *
    * @param id
    *   the scene id
    */
  case class GetScene(id: SceneId)

  /** Response with a scene reference.
    *
    * @param id
    *   the scene's id
    * @param scene
    *   the context actor
    */
  case class SceneRef(id: SceneId, scene: SceneActor.Ref)

  /** Notification of a user's presence in a set of scenes.
    *
    * @param userHandle
    *   the user handle
    * @param lastActive
    *   when the user was last active
    * @param visible
    *   whether the user is visible
    * @param lastReported
    *   when the client last reported state
    * @param scenes
    *   the scenes in which the user is active
    * @param unscenes
    *   the scenes that the user has left
    * @param followScenes
    *   the change in followed scenes
    */
  case class PresenceInScenes(
    userHandle: String,
    lastActive: Date,
    visible: Boolean,
    lastReported: Date,
    scenes: Set[SceneId], // not a delta
    unscenes: Set[SceneId],
    followScenes: SetDelta[SceneId],
  )

  /** Deliver a message on to a scene.
    *
    * @param eventType
    *   the event type
    * @param message
    *   the message
    * @param sceneId
    *   the scene id
    */
  case class DeliverMessage(eventType: String, message: Any, sceneId: SceneId)
end ScenesActor
