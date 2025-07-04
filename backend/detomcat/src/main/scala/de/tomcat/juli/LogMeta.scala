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

package de.tomcat.juli

import argonaut.*
import Argonaut.*

final class LogMeta(meta: Json):

  /** Evaluate a thunk with captured bindings applied. */
  def apply[A](a: => A): A = LogMeta.let(meta)(a)

/** Thread-local log metadata (diagnostic context) to decorate log records. */
object LogMeta:

  private final val meta = new InheritableThreadLocal[Json]:
    override protected def initialValue(): Json = Json.jEmptyObject

  def get: Json = meta.get

  def put(key: String, value: Json): Unit = meta.set(meta.get.withObject(_ :+ (key := value)))

  def put(key: String, value: String): Unit = put(key, jStringOrNull(value))

  def put(key: String, value: Long): Unit = put(key, Json.jNumber(value))

  def put[A: EncodeJson](key: String, value: A): Unit = put(key, value.asJson)

  def ug(guid: String) = put(UG, guid)

  def session(id: String) = put(Session, id)

  def request(guid: String) = put(Request, guid)

  def domain(domain: Long): Unit = put(Domain, domain)

  def user(user: Long): Unit = put(User, user)

  def username(user: String): Unit = put(Username, user)

  def roles(roles: List[String], context: Long): Unit = put(Roles, (roles.mkString(";"), context))

  def event(event: Long): Unit = put(Event, event)

  def remove(key: String): Unit = meta.set(meta.get.withObject(_ - key))

  def clear(): Unit = meta.set(Json.jEmptyObject)

  /** Return a thing that has captured the current meta bindings and will rebind them during evaluation of a thunk */
  def capture = new LogMeta(meta.get)

  def let[A](bindings: (String, Json)*)(a: => A): A =
    let(meta.get.withObject(bindings.foldLeft(_)(_ :+ _)))(a)

  def let[A](json: Json)(a: => A): A =
    val old = meta.get
    try
      meta.set(json)
      a
    finally meta.set(old)

  private def jStringOrNull(value: String): Json = if value eq null then Json.jNull else Json.jString(value)

  final val UG = "ug"

  final val Session = "session"

  final val Request = "request"

  final val Domain = "domain"

  final val User = "user"

  final val Username = "username"

  final val Event = "event"

  final val Roles = "roles"
end LogMeta
