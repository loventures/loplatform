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
package misc

import java.{util, lang}
import scala.collection.Factory
import scala.jdk.CollectionConverters.*
import scala.collection.mutable

/** Generic implicit builders for java collection types.
  *
  * This allows you to write, for instance:
  * {{{
  *   scala> ("a" :: "b" :: "c" :: Nil).to[java.util.List]
  *   res0: java.util.List[String] = [a, b, c]
  * }}}
  */
trait JavaBuilders:

  /** A builder factory for `java.util.LinkedList`s. */
  implicit final def JavaList[A]: Factory[A, util.List[A]] =
    new Factory[A, util.List[A]]:
      override def fromSpecific(it: IterableOnce[A]): util.List[A] =
        val cc = new util.LinkedList[A]
        it.iterator.foreach(cc.add)
        cc

      def newBuilder: mutable.Builder[A, util.List[A]] = new mutable.Builder[A, util.List[A]]:
        val cc = new util.LinkedList[A]

        override def clear(): Unit = cc.clear()

        override def result(): util.List[A] = cc

        override def addOne(a: A): this.type =
          cc.add(a)
          this

  /** A builder factory for `java.util.HashMap`s. */
  implicit final def JavaMap[K, V]: Factory[(K, V), util.Map[K, V]] =
    new Factory[(K, V), util.Map[K, V]]:
      override def fromSpecific(it: IterableOnce[(K, V)]): util.Map[K, V] =
        val cc = new util.HashMap[K, V]
        it.iterator.foreach(kv => cc.put(kv._1, kv._2))
        cc

      def newBuilder: mutable.Builder[(K, V), util.Map[K, V]] = new mutable.Builder[(K, V), util.Map[K, V]]:
        val cc = new util.HashMap[K, V]

        override def clear(): Unit = cc.clear()

        override def result(): util.Map[K, V] = cc

        override def addOne(kv: (K, V)): this.type =
          cc.put(kv._1, kv._2)
          this

  /** A builder factory for `java.util.HashSet`s. */
  implicit final def JavaSet[A]: Factory[A, util.Set[A]] =
    new Factory[A, util.Set[A]]:
      override def fromSpecific(it: IterableOnce[A]): util.Set[A] =
        val cc = new util.HashSet[A]
        it.iterator.foreach(cc.add)
        cc

      def newBuilder: mutable.Builder[A, util.Set[A]] = new mutable.Builder[A, util.Set[A]]:
        val cc = new util.HashSet[A]

        override def clear(): Unit = cc.clear()

        override def result(): util.Set[A] = cc

        override def addOne(a: A): this.type =
          cc.add(a)
          this

  import language.implicitConversions

  implicit final def ToJavaBuildingSyntax[A](ji: lang.Iterable[A]): JavaBuildingSyntax[A] =
    new JavaBuildingSyntax[A](ji)
end JavaBuilders

final class JavaBuildingSyntax[A](private val self: lang.Iterable[A]) extends AnyVal:
  def to[CC[_]](implicit fac: Factory[A, CC[A]]): CC[A] =
    self.iterator.asScala.to(fac)

object JavaBuilders extends JavaBuilders
