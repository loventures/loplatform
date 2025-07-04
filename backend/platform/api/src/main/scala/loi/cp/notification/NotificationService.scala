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

package loi.cp.notification

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.PK

import scala.reflect.ClassTag
import scaloi.syntax.ClassTagOps.*

@Service
trait NotificationService:

  /** Fire a notification event.
    *
    * @tparam N
    *   the type of notification to fire
    * @param parent
    *   some appropriate entity whose lifecycle should align with the created notification's
    * @param init
    *   data with which to initialise the notification
    */
  def nοtify[N <: Notification]: Notifying[N] = new Notifying[N] // making this final breaks mocks

  /* implementation class for `nοtify`, b/c targs are all-or-nothing in scala 2 */
  class Notifying[N <: Notification] private[NotificationService]:
    import PK.ops.*
    def apply[Id: PK](parent: Id, init: NotificationInit[N])(implicit tt: ClassTag[N]): Unit =
      NotificationService.this.notify(() => parent.pk, classTagClass[N], init)

  /** Fire a notification event. The parent should be some appropriate entity whose lifecycle should align with this
    * notification.
    */
  def notify(parent: Id, notification: Class[? <: Notification], init: Any): Unit

  /** Fire a notification event and deliver it synchronously. This may be slow so should be used with caution, typically
    * only for single-recipient events where receipt of the notification will be immediately visible to the end user
    * (i.e. a direct response to their own action).
    */
  def notifyImmediate(item: Id, notification: Class[? <: Notification], init: Any): Unit

  /** Get a notification by PK.
    * @param id
    *   the PK
    * @return
    *   the notification, if found
    */
  def notification(id: Long): Option[Notification]
end NotificationService
