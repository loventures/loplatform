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

package loi.cp.message

import com.learningobjects.cpxp.component.annotation.Service

/** This class supports sending internal messages within the platform. For now you are limited to emailing yourself,
  * someone in one of your own courses, or people on a reply list. Need sane requirements. Probably those restrictions
  * should not be applied inside this class.
  */
@Service
trait MessageService:

  /** @param message
    *   the message to send
    * @param parent
    *   the thread parent
    * @param emailCopy
    *   whether to email self a copy of the message
    */
  def sendMessage(message: NewMessage, parent: Option[Long], emailCopy: Boolean): Message
end MessageService
