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

package com.learningobjects.cpxp.scala.environment

import com.learningobjects.cpxp.BaseWebContext
import com.learningobjects.cpxp.scala.cpxp.Service.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.domain.DomainWebService
import com.learningobjects.cpxp.service.script.ScriptService
import org.log4s.Logger

import scala.language.implicitConversions
import scala.util.Try

object DomainEnvironment:
  val logger = org.log4s.getLogger

  def startEnvironment(domain: Option[Long]) =
    for domainId <- domain
    yield
      logger warn s"Setting ComponentEnvironment for domain: $domainId"
      service[ScriptService].initComponentEnvironment()

  def stopEnvironment(): Unit =
    Current.clear()
    BaseWebContext.getContext.clear()

  def domain[T](domainId: Long)(op: => T): T =
    val env = new BaseEnvironment[Unit, T] with TransactionEnvironment[Unit, T] with DomainEnvironment[Unit, T]:
      override implicit def domainIdEvidence(input: Unit): Long = domainId

      override def logger: Logger = DomainEnvironment.logger
    env.performNoParam(op)
end DomainEnvironment

/** Initializes Domain-specific environment settings for a operation.
  */
trait DomainEnvironment[V, R] extends Environment[V, R]:
  import DomainEnvironment.*

  implicit def domainIdEvidence(input: V): Long

  def logger: Logger

  abstract override def before(input: V): V            =
    val superBefore     = super.before(input)
    val longInput: Long = implicitly[Long](using input)
    Current.setDomainDTO(service[DomainWebService].getDomainDTO(longInput))
    startEnvironment(Option(longInput))
    superBefore
  abstract override def after[RR <: R](postOp: RR): RR =
    val superPostOp = super.after(postOp)
    logger info s"Clearing Current"
    Try(stopEnvironment())
    superPostOp
end DomainEnvironment
