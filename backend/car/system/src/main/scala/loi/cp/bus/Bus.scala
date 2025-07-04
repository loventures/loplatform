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

package loi.cp.bus

/** Abstraction for things that can contain messages
  */
trait Bus:
  def getBusName: String
  def getId: Long
  def getRootId: Long

object Bus:

  def apply(mbf: MessageBusFacade): Bus = new Bus:
    override def getBusName: String = mbf.getSystem.getName

    override def getId: Long = mbf.getId

    override def getRootId: Long = mbf.getRootId

class SimpleBus(busName: String, id: Long, rootId: Long) extends Bus:
  override def getBusName: String = busName

  override def getId: Long = id

  override def getRootId: Long = rootId

object SimpleBus:
  def apply(busName: String, id: Long, rootId: Long): SimpleBus = new SimpleBus(busName, id, rootId)
