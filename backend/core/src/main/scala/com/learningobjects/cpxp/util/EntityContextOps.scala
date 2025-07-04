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

package com.learningobjects.cpxp.util

import com.learningobjects.cpxp.util.tx.TransactionCompletion

import scala.concurrent.duration.FiniteDuration

/** Entity context enhancements.
  */
final class EntityContextOps(val self: EntityContext) extends AnyVal:

  /** Run a function after the transaction commits.
    * @param f
    *   the function to run
    * @tparam T
    *   the return type
    */
  def afterCommit[T](f: => T): Unit = self `pushCompletion` new TransactionCompletion:
    override def onCommit(): Unit = f

  /** Run a function with a given statement timeout.
    * @param timeout
    *   the timeout
    * @param f
    *   the function to run
    * @tparam T
    *   the return type
    * @return
    *   the result of f
    */
  def withTimeout[T](timeout: FiniteDuration)(f: => T): T =
    import scaloi.syntax.FiniteDurationOps.*
    val old = self.getStatementTimeout
    try
      self `setStatementTimeout` timeout.asJava; f
    finally self `setStatementTimeout` old
end EntityContextOps

/** Entity context operations companion.
  */
object EntityContextOps extends ToEntityContextOps

/** Implicit conversion for entity context operations.
  */
trait ToEntityContextOps:
  import language.implicitConversions

  /** Implicit conversion from entity context to the enhancements.
    * @param ec
    *   the entity context
    */
  @inline
  implicit final def toEntityContextOps(ec: EntityContext): EntityContextOps =
    new EntityContextOps(ec)

  /** Implicit conversion from an entity context supplier to the enhancements.
    * @param prov
    *   the supplier
    */
  @inline
  implicit final def toEntityContextOpsP(prov: () => EntityContext): EntityContextOps =
    new EntityContextOps(prov())

  /* I want to be in the EntityContext companion but I am forbidden */
  @inline
  implicit final def unprovideEC(implicit prov: () => EntityContext): EntityContext =
    prov()
end ToEntityContextOps
