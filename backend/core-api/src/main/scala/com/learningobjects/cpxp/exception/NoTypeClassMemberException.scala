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

package com.learningobjects.cpxp.exception

import scala.reflect.ClassTag

/** There are some type classes being used for behavior that used to use subtype polymorphism. Therefore, to accommodate
  * the clients that were using dynamic dispatch, the type class has to have a member for a broad type (such as Asset or
  * even Any) and that member has to pattern match on all the "subtypes". This exception is for those type class members
  * to use for their default case. The transition from subtype polymorphism to type classes closes the system for
  * extension, so it might not be a good idea at all. But what is also a bad idea is forcing irrelevant traits into the
  * type hierarchy. For example, Asset is in authoringApi and it can't have supertraits from downstream verticals. Thus
  * those other verticals are forced to use something like type classes. And thus they loose their dynamic dispatch.
  */
case class NoTypeClassMemberException private (msg: String) extends RuntimeException(msg)
object NoTypeClassMemberException:
  def apply[A, TypeClassClass[A]](
    missingMemberClass: Class[?]
  )(implicit tccTag: ClassTag[TypeClassClass[A]]): NoTypeClassMemberException =

    NoTypeClassMemberException(s"No type class member of $tccTag for $missingMemberClass")
