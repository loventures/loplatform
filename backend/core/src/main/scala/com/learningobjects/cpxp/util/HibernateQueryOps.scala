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

import org.hibernate.query.Query

import scala.language.implicitConversions

/** @author
  *   mkalish
  */

final class HibernateQueryOps[T](value: Query[T]):

  def setParameterWhen(condition: Boolean): (String, Any) => Query[T] = (parameterName, parameter) =>
    if condition then value.setParameter(parameterName, parameter)
    value

  def setParameterOpt[A](opt: Option[A]): (String, A => Any) => Query[T] = (parameterName, function) =>
    opt foreach { a =>
      value.setParameter(parameterName, function(a))
    }
    value
end HibernateQueryOps

object HibernateQueryOps extends ToHibernateQueryOps

trait ToHibernateQueryOps:
  implicit def toHibernateQueryOps[T](query: Query[T]): HibernateQueryOps[T] = new HibernateQueryOps[T](query)
