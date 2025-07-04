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

package loi.cp.lwgrade

import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.notification.{NotificationFacade, NotificationImplementation}
import scaloi.misc.TimeSource

@Component
class GradeNotificationImpl(
  val componentInstance: ComponentInstance,
  val self: NotificationFacade,
  ts: TimeSource,
  userDto: UserDTO,
) extends GradeNotification
    with NotificationImplementation
    with ComponentImplementation:

  import GradeNotification.*

  @PostCreate
  private def init(init: Init): Unit =
    self.setContext(Some(init.contextId))
    self.setData(Data(init.column.title, init.learnerId))
    self.setSender(None)
    self.setTime(ts.date)
    self.setTopic(Some(init.column.path.toString))

  override def aggregationKey: Option[String] = getTopic.map(t => s"${`type`}:$t")

  override def audience: Seq[Long] = Seq(data.learnerId)

  override lazy val title: String = data.title

  private lazy val data: Data = self.getData[Data]
end GradeNotificationImpl
