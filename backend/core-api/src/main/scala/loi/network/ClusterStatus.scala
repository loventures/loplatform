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

package loi.network

/** Effects on the status of cluster services for the application.
  *
  * We assume there is some sort of leader or central host in the cluster we can ask questions about. Any implementation
  * must attempts to become that leader.
  */
trait ClusterStatus[F[_], Host]:

  def centralHost: F[Host]

  /** Attempt to become the central host. Return true if this attempt was sucessful.
    */
  def acquireCentralHost: F[Boolean]

  /** Release central hosts
    */
  def releaseCentralHost: F[Unit]

  /** Return a list of recently seen hosts.
    */
  def recentHosts: F[List[Host]]

  /** Send a heartbeat to the cluster. Acknowledging you're still alive.
    */
  def heartbeat: F[Unit]

  /*
   * Return the address of the host machine currently running.
   */
  def localhost: F[Host]
end ClusterStatus
