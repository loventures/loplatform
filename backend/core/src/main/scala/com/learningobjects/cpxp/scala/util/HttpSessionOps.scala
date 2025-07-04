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

package com.learningobjects.cpxp.scala.util

import jakarta.servlet.http.HttpSession
import scalaz.{@@, Tag}

import scala.language.implicitConversions
import scala.reflect.ClassTag

/** Enhancements on HTTP sessions.
  *
  * @param session
  *   the HTTP session
  */
final class HttpSessionOps(val session: HttpSession) extends AnyVal:
  import scaloi.syntax.AnyOps.*
  import scaloi.syntax.ClassTagOps.*
  import scaloi.syntax.OptionOps.*

  /** Get the value of a session attribute, if present and of the right type.
    *
    * @param name
    *   the attribute name
    * @tparam T
    *   the expected type
    * @return
    *   the attribute value, if present
    */
  def attr[T](name: String)(implicit ct: ClassTag[T]): Option[T] =
    Option(session.getAttribute(name)) flatMap { attr => ct.option(attr) }

  def attrTag[B, T: ClassTag](name: String): Option[T @@ B] =
    attr[T](name).map(a => Tag.apply[T, B](a))

  /** Remove a session attribute, if present and of the right type.
    *
    * @param name
    *   the attribute name
    * @tparam T
    *   the expected type
    * @return
    *   the previous attribute value, if present
    */
  def remove[T: ClassTag](name: String): Option[T] = attr(name) <|? { t =>
    session.removeAttribute(name)
  }

  /** Get the value of a session attribute, if present and of the right type, or else create and bind a new value into
    * the session. This synchronizes on the session to be threadsafe, so creation should not be an expensive or blocking
    * operation.
    *
    * @param name
    *   the attribute name
    * @param f
    *   the value factory
    * @tparam T
    *   the expected type
    * @return
    *   the attribute value
    */
  def attrOr[T: ClassTag](name: String)(f: => T): T = session.synchronized {
    // if we really cared about minimizing synczn we could do an additional getOrElse
    // outside the synchronized block and things would remain threadsafe.
    attr[T](name) getOrElse {
      f <| (session.setAttribute).curried(name)
    }
  }
end HttpSessionOps

/** HTTP session operations companion.
  */
object HttpSessionOps extends ToHttpSessionOps

/** Implicit conversion for http session operations.
  */
trait ToHttpSessionOps:

  /** Implicit conversion from a session to enhancements.
    * @param session
    *   the session
    */
  implicit def toHttpSessionOps(session: HttpSession): HttpSessionOps = new HttpSessionOps(session)
