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

import com.learningobjects.cpxp.dto.BaseOntology
import com.learningobjects.cpxp.util.ManagedUtils
import jakarta.persistence.Persistence

/** Initailizes a minimal persistence context. XXX: For use with unit tests
  */
trait PersistenceEnvironment[V, R] extends Environment[V, R]:
  def contextName: String
  def annotatedClasses: List[Class[?]]
  def classLoader: ClassLoader              = getClass.getClassLoader
  def configMap: Map[?, ?]                  = Map.empty
  abstract override def before(input: V): V =
    val superBefore = super.before(input)
    annotatedClasses.foreach(clazz => BaseOntology.getOntology.analyzeClass(clazz))
    BaseOntology.getOntology.updateMaps()
    val emf         = Persistence.createEntityManagerFactory(contextName)
    ManagedUtils.init(emf)
    superBefore
end PersistenceEnvironment

object TransactionEnvironment:
  def tx[T](op: => T): T =
    val env = new BaseEnvironment[Unit, T] with TransactionEnvironment[Unit, T]
    env.performNoParam(op)

trait TransactionEnvironment[V, R] extends Environment[V, R]:
  abstract override def before(input: V): V =
    val superBefore = super.before(input)
    ManagedUtils.JTACompliantBegin()
    superBefore

  abstract override def after[RR <: R](returnVal: RR): RR =
    val superAfter = super.after(returnVal)
    ManagedUtils.JTACompliantEnd()
    superAfter

  abstract override def onError(th: Throwable): Throwable =
    val superThrow = super.onError(th)
    ManagedUtils.getEntityContext.setRollbackOnly()
    ManagedUtils.JTACompliantEnd()
    superThrow
end TransactionEnvironment
