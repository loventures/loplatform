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

package com.learningobjects.cpxp.scala.concurrent

import com.learningobjects.cpxp.BaseWebContext
import com.learningobjects.cpxp.component.ComponentEnvironment
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import scalaz.std.string.*
import scaloi.syntax.AnyOps.*

import scala.concurrent.ExecutionContext

/** An execution context that tracks various thread-localities. */
final class CpxpExecutionContext(dele: ExecutionContext)(implicit
  domain: DomainDTO,
  user: UserDTO,
  env: ComponentEnvironment /* and others? */
) extends ExecutionContext:
  import CpxpExecutionContext.*

  def execute(runnable: Runnable): Unit =
    dele execute (() =>
      Current.setDomainDTO(domain)
      Current.setUserDTO(user)
      BaseWebContext.getContext.setComponentEnvironment(env)
      runnable.run()
    )

  lazy val failureContextMsg =
    s"Async error${domain ?| s" in domain ${domain.id}"}${user ?| s" as user ${user.id}"}:"

  /* By default this just `println`s `cause`... we can do slightly better. */
  def reportFailure(cause: Throwable): Unit =
    logger.warn(cause)(failureContextMsg)
end CpxpExecutionContext

object CpxpExecutionContext:
  val logger = org.log4s.getLogger

  /** Create a new `CpxpExecutionContext` with the given environment and the current thread's domain and user. */
  def create(implicit env: ComponentEnvironment): ExecutionContext =
    new CpxpExecutionContext(ExecutionContext.global)(using Current.getDomainDTO, Current.getUserDTO, env)
