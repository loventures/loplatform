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

package scaloi

import scala.collection.Factory

object std:

  object collection:

    /** Zero evidence for a collection type. */
    implicit def collectionZero[CC[_] <: IterableOnce[?], T](implicit fac: Factory[T, CC[T]]): Zero[CC[T]] =
      new Zero[CC[T]]:
        override def zero: CC[T]               = fac.newBuilder.result()
        override def isZero(a: CC[T]): Boolean = a.iterator.isEmpty

  object ju:
    import java.util as jutil

    /** Zero evidence for [[jutil.List]]. */
    implicit def juZero[A]: Zero[jutil.List[A]] = Zero.instance(new jutil.ArrayList[A](), _.isEmpty)
end std
