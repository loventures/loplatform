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

/** Environment is an abstraction over thread local and contextual set up of a method invocation.
  * @tparam V
  *   type of input into the function op
  * @tparam R
  *   return type of op
  */
abstract class Environment[V, R]:

  def before(value: V): V // input and to allow after to transform the return value

  def after[RR <: R](returnVal: RR): RR

  def onError(th: Throwable): Throwable

  def perform(op: V => R, input: V): R =
    try
      val transformedInput = before(input)
      val returnVal        = op(transformedInput)
      after(returnVal)
    catch case th: Throwable => throw onError(th)

  // XXX:An alternative signature to work around parameterless methods when input type is Unit
  def performNoParam[RR <: R](op: => RR)(implicit ev: Unit <:< V): RR =
    try
      before(())
      after(op)
    catch case th: Throwable => throw onError(th)
end Environment

//A Base class for stacking traits to override.
class BaseEnvironment[V, R] extends Environment[V, R]:
  def before(input: V): V               = input
  def after[RR <: R](returnVal: RR)     = returnVal
  def onError(th: Throwable): Throwable = th
