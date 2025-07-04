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

package com.learningobjects.cpxp.scala.cpxp

import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.entity.EntityUtils
import com.learningobjects.cpxp.service.finder.Finder as CpxpFinder
import com.learningobjects.cpxp.service.item.{ItemService, ItemWebService, Item as CpxpItem}
import scaloi.syntax.ClassTagOps.*

import scala.collection.BuildFrom
import scala.reflect.ClassTag

/** For the profoundly lazy. */
final class ItemOps[A](private val a: A) extends AnyVal:
  import PK.ops.*

  @inline def item(implicit is: ItemService, pk: PK[A]): CpxpItem =
    is `get` a.pk

  @inline def item_?(implicit is: ItemService, pk: PK[A]): Option[CpxpItem] = Option(item)

  @inline def finder[F <: CpxpFinder: ClassTag](implicit is: ItemService, pk: PK[A]): F =
    is.get(a.pk, classTagClass[F])

  @inline def finder_?[F <: CpxpFinder: ClassTag](implicit is: ItemService, pk: PK[A]): Option[F] = Option(finder[F])

  @inline def parent(implicit is: ItemService, pk: PK[A]): CpxpItem =
    item.getParent

  @inline def root(implicit is: ItemService, pk: PK[A]): CpxpItem =
    item.getRoot

  @inline def addChild[F <: CpxpFinder: ClassTag](
    f: F => Unit
  )(implicit is: ItemService, onto: Ontology, pk: PK[A]): F =
    is.create(item, onto.getItemTypeForFinder(classTagClass[F]), (i: CpxpItem) => f(i.getFinder.asInstanceOf[F]))
      .getFinder
      .asInstanceOf[F]
end ItemOps

final class ItemsOps[CC[X] <: IterableOnce[X], A](private val as: CC[A]) extends AnyVal:
  import PK.ops.*

  import scala.jdk.CollectionConverters.*

  @inline def finders[F <: CpxpFinder: ClassTag](implicit
    is: ItemService,
    pk: PK[A],
    bf: BuildFrom[CC[A], F, CC[F]]
  ): CC[F] =
    bf.fromSpecific(as)(
      is.map(as.iterator.toSeq.map(_.pk).asJava, EntityUtils.getItemType(classTagClass[F]))
        .asScala
        .values
        .map(item => classTagClass[F].cast(item.getFinder))
    )
end ItemsOps

final class StringItemOps(private val self: String) extends AnyVal:
  @inline def item(implicit iws: ItemWebService): CpxpItem = iws `getItem` (iws `getById` self)

  @inline def item_?(implicit iws: ItemWebService): Option[CpxpItem] = Option(item)

object Item extends ToItemOps

trait ToItemOps:
  import language.implicitConversions

  @inline implicit final def ToItemOps[A](a: A)(implicit pk: PK[A]): ItemOps[A] =
    new ItemOps[A](a)

  @inline implicit final def ToItemsOps[CC[X] <: IterableOnce[X], A](a: CC[A])(implicit pk: PK[A]): ItemsOps[CC, A] =
    new ItemsOps[CC, A](a)

  @inline implicit final def ToStringItemOps(a: String): StringItemOps =
    new StringItemOps(a)
end ToItemOps
