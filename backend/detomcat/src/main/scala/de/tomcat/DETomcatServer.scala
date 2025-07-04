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

package de.tomcat

import java.net.InetSocketAddress

import cats.effect.Sync
import cats.~>
import org.apache.catalina.startup.Tomcat
import org.apache.catalina.{Lifecycle, LifecycleEvent}
import org.http4s.server.Server

sealed class DETomcatServer[F[_]: Sync](tomcat: Tomcat, bindAddress: InetSocketAddress) extends Server:
  self =>

  def shutdown: F[Unit] =
    Sync[F].delay {
      tomcat.stop()
      tomcat.destroy()
    }

  /** */
  def onShutdown(f: => Unit): DETomcatServer.this.type =
    tomcat.getServer.addLifecycleListener((event: LifecycleEvent) =>
      if Lifecycle.AFTER_STOP_EVENT.equals(event.getType) then f
    )
    this

  override lazy val address: InetSocketAddress =
    val port = tomcat.getConnector.getLocalPort
    new InetSocketAddress(bindAddress.getHostString, port)

  override def isSecure: Boolean = tomcat.getService.findConnectors().exists(_.getSecure)

  def mapK[G[_]: Sync](fg: F ~> G): DETomcatServer[G] = new DETomcatServer[G](tomcat, bindAddress):
    override def shutdown: G[Unit] = fg(self.shutdown)
end DETomcatServer
