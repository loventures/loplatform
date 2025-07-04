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

package com.learningobjects.cpxp.scala.actor

import org.apache.pekko.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import org.apache.pekko.cluster.singleton.{
  ClusterSingletonManager,
  ClusterSingletonManagerSettings,
  ClusterSingletonProxy,
  ClusterSingletonProxySettings
}

/** Support for cluster-wide actors.
  */
object ClusterActors:

  /** Create a new cluster-wide singleton actor. This is technically a reference to a local proxy that will forward to
    * the actual singleton.
    *
    * @param props
    *   the actor creation properties
    * @param name
    *   the actor name
    * @return
    *   the singleton actor
    */
  def singleton(props: Props, name: String)(implicit system: ActorSystem): ActorRef =
    system.actorOf(
      ClusterSingletonManager
        .props(props, PoisonPill, ClusterSingletonManagerSettings(system)),
      s"cluster-$name"
    )
    system.actorOf(ClusterSingletonProxy.props(s"/user/cluster-$name", ClusterSingletonProxySettings(system)), name)
end ClusterActors
