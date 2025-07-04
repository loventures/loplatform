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

import com.learningobjects.cpxp.dto.Facade as FacadeInterface
import com.learningobjects.cpxp.service.facade.FacadeService
import scaloi.syntax.ClassTagOps.*

import java.util.function.Consumer
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag
import scala.util.Try

object Facade:

  implicit class PKFacadeOps[A](val a: A) extends AnyVal:
    def facade[F <: FacadeInterface](implicit tt: ClassTag[F], fs: FacadeService, pk: PK[A]): F =
      fs.getFacade(pk.pk(a), classTagClass[F])

    def facade_?[F <: FacadeInterface](implicit tt: ClassTag[F], fs: FacadeService, pk: PK[A]): Option[F] =
      Option(facade[F])

    def facade_![F <: FacadeInterface](implicit tt: ClassTag[F], fs: FacadeService, pk: PK[A]): Try[F] =
      Try(facade_?[F].getOrElse(throw new NullPointerException))

    def addFacade[F <: FacadeInterface](f: F => Unit)(implicit tt: ClassTag[F], fs: FacadeService, pk: PK[A]): F =
      fs.addFacade(pk.pk(a), classTagClass[F], consumer(f))

    private def consumer[F](f: F => Unit): Consumer[F] = t => f(t)
  end PKFacadeOps

  implicit class PKFacadesOp[A](val c: Iterable[A]) extends AnyVal:

    def facades[F <: FacadeInterface](implicit tt: ClassTag[F], pk: PK[A], fs: FacadeService): Iterable[F] =
      fs.getFacades(c.map(pk.pk).asJava, classTagClass[F]).asScala

  implicit class StringFacadeOps(val idStr: String) extends AnyVal:
    def facade[F <: FacadeInterface](implicit tt: ClassTag[F], fs: FacadeService): F =
      fs.getFacade(idStr, tt.runtimeClass.asInstanceOf[Class[F]])

  implicit class FacadeMapOps(idToId: Map[Long, Long]):
    def facades[K <: FacadeInterface, V <: FacadeInterface](implicit
      fs: FacadeService,
      tk: ClassTag[K],
      tv: ClassTag[V]
    ): Map[K, V] =
      val keys: Seq[K]          = idToId.keySet.facades[K].toSeq
      val keyById: Map[Long, K] = keys.map(entry => entry.getId.toLong -> entry).toMap

      val values: Seq[V]          = idToId.values.facades[V].toSeq
      val valueById: Map[Long, V] = values.map(entry => entry.getId.toLong -> entry).toMap

      idToId
        .filter(entry => keyById.contains(entry._1) && valueById.contains(entry._2))
        .map(entry => keyById(entry._1) -> valueById(entry._2))
    end facades

    def facadeValues[V <: FacadeInterface: ClassTag](implicit fs: FacadeService): Map[Long, V] =
      val values: Seq[V]          = idToId.values.facades[V].toSeq
      val valueById: Map[Long, V] = values.map(entry => entry.getId.toLong -> entry).toMap

      idToId
        .filter(entry => valueById.contains(entry._2))
        .map(entry => entry._1 -> valueById(entry._2))
  end FacadeMapOps

  implicit class FacadeMultiMapOps(idToIds: Map[Long, Seq[Long]]):
    def facades[K <: FacadeInterface, V <: FacadeInterface](implicit
      fs: FacadeService,
      tk: ClassTag[K],
      tv: ClassTag[V]
    ): Map[K, Seq[V]] =
      val keys: Seq[K]          = idToIds.keySet.facades[K].toSeq
      val keyById: Map[Long, K] = keys.map(entry => entry.getId.toLong -> entry).toMap

      val values: Seq[V]          = idToIds.values.flatten.facades[V].toSeq
      val valueById: Map[Long, V] = values.map(entry => entry.getId.toLong -> entry).toMap

      idToIds
        .filter(entry => keyById.contains(entry._1) && entry._2.forall(valueById.contains))
        .map(entry => keyById(entry._1) -> entry._2.map(valueById))
    end facades

    def facadeValues[V <: FacadeInterface](implicit fs: FacadeService, tv: ClassTag[V]): Map[Long, Seq[V]] =
      val values: Seq[V]          = idToIds.values.flatten.facades[V].toSeq
      val valueById: Map[Long, V] = values.map(entry => entry.getId.toLong -> entry).toMap

      idToIds
        .filter(entry => entry._2.forall(valueById.contains))
        .map(entry => entry._1 -> entry._2.map(valueById))
  end FacadeMultiMapOps
end Facade
