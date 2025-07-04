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

import com.learningobjects.cpxp.listener.CpxpListener
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import com.learningobjects.cpxp.service.ServiceContext
import com.learningobjects.cpxp.service.overlord.ExternalOverlordService
import com.learningobjects.cpxp.util.ManagedUtils
import loi.cp.overlord.logging.RemoteLogsActor
import loi.cp.startup.StartupTaskActor

final class OverlordActorListener extends CpxpListener:

  /** Make the actor for this appserver. */
  override def postBootstrap(ctx: ServiceContext): Unit =
    RemoteLogsActor.initialize(
      eos = ManagedUtils.getService(classOf[ExternalOverlordService]),
      sm = ctx.getServiceMeta
    )
    // message the cluster actor early so this node comes to an early understanding of where the
    // singleton resides so as to not delay the first genuine cluster status request
    StartupTaskActor.clusterActor ! StartupTaskActor.Hollo
end OverlordActorListener
