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

import com.learningobjects.cpxp.component.*
import com.learningobjects.cpxp.dto.Facade as FacadeIface
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.{Id, IdType}
import scaloi.syntax.ClassTagOps.classTagClass

import java.lang as jl
import scala.collection.Factory
import scala.jdk.CollectionConverters.*
import scala.reflect.*
import scala.util.Try
import scala.util.control.NonFatal

/** Various extension methods relating to components. */
object Component extends Component:
  import language.implicitConversions

  def component[C <: ComponentInterface: ClassTag]: C =
    ComponentSupport.get(classTagClass[C])

  def instance[C: ClassTag]: C =
    ComponentSupport.newInstance(classTagClass[C])

  // ================================

  final class PKComponentOps[A](private val a: A) extends AnyVal:
    @inline private def id(implicit pk: PK[A]): jl.Long = pk.pk(a)

    def component[C <: ComponentInterface: ClassTag](implicit
      dm: DataModel[C] = null,
      pk: PK[A],
      cs: ComponentService
    ): C =
      cs.get(id, itemType, classTagClass[C])

    def component_?[C <: ComponentInterface](implicit
      tt: ClassTag[C],
      dm: DataModel[C] = null,
      pk: PK[A],
      cs: ComponentService
    ): Option[C] =
      Option(component[C])

    def component_![C <: ComponentInterface](implicit
      tt: ClassTag[C],
      dm: DataModel[C] = null,
      pk: PK[A],
      cs: ComponentService
    ): Try[C] =
      Try(component_?[C].getOrElse(throw NoSuchComponentException(classTagClass[C], id)))

    def isComponent[C <: ComponentInterface: ClassTag](implicit
      dm: DataModel[C] = null,
      pk: PK[A],
      cs: ComponentService
    ): Boolean =
      this.component[ComponentInterface] `isComponent` classTagClass[C]

    def tryComponent[C <: ComponentInterface: ClassTag](implicit
      dm: DataModel[C] = null,
      pk: PK[A],
      cs: ComponentService
    ): Option[C] =
      try component_?[C]
      catch case NonFatal(_) => None

    def componentService[C <: ComponentInterface: ClassTag](implicit cs: ComponentService, pk: PK[A]): C =
      cs.get(id, null, classTagClass[C])

    def addComponent[C <: ComponentInterface: ClassTag: DataModel](impl: Class[? <: C], init: Any)(implicit
      fs: FacadeService,
      pk: PK[A]
    ): C =
      fs.addComponent[C, C](id, classTagClass[C], DataModel[C], impl.asInstanceOf[Class[C]], init)

    def addComponent[C <: ComponentInterface: ClassTag: DataModel, D <: C](
      init: D
    )(implicit fs: FacadeService, pk: PK[A]): D =
      fs.addComponent(id, classTagClass[C], DataModel[C], init)
  end PKComponentOps

  @inline implicit def PKComponentOps[A: PK](a: A): PKComponentOps[A] =
    new PKComponentOps[A](a)

  // ================================

  implicit class IterableLongComponentOps(val ids: Iterable[Long]) extends AnyVal:
    def component[C <: ComponentInterface](implicit
      tt: ClassTag[C],
      dm: DataModel[C] = null,
      cs: ComponentService
    ): Seq[C] =
      ComponentSupport.get(ids.map(Long.box).asJava, itemType, tt.runtimeClass.asInstanceOf[Class[C]]).asScala.toSeq

    def componentIdMap[A <: ComponentInterface: ClassTag, B](
      f: A => B
    )(implicit pk: PK[A], cs: ComponentService): Map[Long, B] =
      ids.component[A].map(c => pk.pk(c).longValue() -> f(c)).toMap
  end IterableLongComponentOps

  // ================================

  implicit class IdTypeComponentOps(val idt: IdType) extends AnyVal:
    def component[C <: ComponentInterface](implicit tt: ClassTag[C], cs: ComponentService): C =
      ComponentSupport.get(idt, tt.runtimeClass.asInstanceOf[Class[C]])

    def componentService[C <: ComponentInterface](implicit tt: ClassTag[C], cs: ComponentService): C =
      cs.get(idt, tt.runtimeClass.asInstanceOf[Class[C]])

    def tryComponent[C <: ComponentInterface](implicit tt: ClassTag[C], cs: ComponentService): Option[C] =
      try Option(idt.component[C])
      catch case NonFatal(_) => None
  end IdTypeComponentOps

  // ================================

  implicit class ComponentInterfaceOps(val ci: ComponentInterface) extends AnyVal:
    def component[C <: ComponentInterface](implicit tt: ClassTag[C]): C =
      ci.asComponent(tt.runtimeClass.asInstanceOf[Class[C]])

    def tryComponent[C <: ComponentInterface](implicit tt: ClassTag[C]): Option[C] =
      if ci.isComponent(tt.runtimeClass.asInstanceOf[Class[C]]) then Some(ci.component[C])
      else None

  // ================================

  implicit class ClassOps[T <: ComponentInterface](c: Class[T]):
    def getComponentDescriptor(implicit cs: ComponentService): ComponentDescriptor =
      ComponentSupport.getComponentDescriptor(c.getName)

  // ================================

  implicit class ComponentStringOps(idString: String):
    def component[C <: ComponentInterface](implicit fs: FacadeService, tt: ClassTag[C], cs: ComponentService): C =
      ComponentSupport.getInstance(
        tt.runtimeClass.asInstanceOf[Class[C]],
        fs.getFacade(idString, classOf[FacadeIface]),
        null
      )

    def tryComponent[C <: ComponentInterface](implicit
      fs: FacadeService,
      tt: ClassTag[C],
      cs: ComponentService
    ): Option[C] =
      Option(component[C])

    def descriptor(implicit cs: ComponentService): Option[ComponentDescriptor] =
      Option(ComponentSupport.getComponentDescriptor(idString))
  end ComponentStringOps

  // ================================

  implicit class ComponentPKIterableOps[CC[X] <: Iterable[X], A: PK](ids: CC[A]):
    def components[C <: ComponentInterface](implicit
      fac: Factory[C, CC[C]],
      tt: ClassTag[C],
      A: PK[A],
      cs: ComponentService
    ): CC[C] =
      ComponentSupport
        .getById(ids.map(A.pk).asJava, classTagClass(using tt))
        .asScala
        .iterator
        .to(fac)
  end ComponentPKIterableOps

  private def itemType[C <: ComponentInterface](implicit dm: DataModel[C]): String =
    Option(dm).map(_.itemType).orNull

  // ================================

  implicit class ComponentMapOps(idToId: Map[Long, Long]):
    def components[K <: ComponentInterface & Id: ClassTag, V <: ComponentInterface & Id: ClassTag](implicit
      cs: ComponentService
    ): Map[K, V] =
      val keys: Seq[K]          = ComponentSupport.getById(idToId.keySet.map(long2Long).asJava, classTagClass[K]).asScala.toSeq
      val keyById: Map[Long, K] = keys.map(entry => entry.getId.toLong -> entry).toMap

      val values: Seq[V]          = ComponentSupport.getById(idToId.values.map(long2Long).asJava, classTagClass[V]).asScala.toSeq
      val valueById: Map[Long, V] = values.map(entry => entry.getId.toLong -> entry).toMap

      idToId
        .filter(entry => keyById.contains(entry._1) && valueById.contains(entry._2))
        .map(entry => keyById(entry._1) -> valueById(entry._2))
    end components

    def componentValues[V <: ComponentInterface & Id: ClassTag](implicit cs: ComponentService): Map[Long, V] =
      val values: Seq[V]          = ComponentSupport.getById(idToId.values.map(long2Long).asJava, classTagClass[V]).asScala.toSeq
      val valueById: Map[Long, V] = values.map(entry => entry.getId.toLong -> entry).toMap

      idToId
        .filter(entry => valueById.contains(entry._2))
        .map(entry => entry._1 -> valueById(entry._2))
  end ComponentMapOps

  implicit class ComponentMultiMapOps(idToIds: Map[Long, Seq[Long]]):
    def components[K <: ComponentInterface & Id: ClassTag, V <: ComponentInterface & Id: ClassTag](implicit
      cs: ComponentService
    ): Map[K, Seq[V]] =
      val keys: Seq[K]          = ComponentSupport.getById(idToIds.keySet.map(long2Long).asJava, classTagClass[K]).asScala.toSeq
      val keyById: Map[Long, K] = keys.map(entry => entry.getId.toLong -> entry).toMap

      val values: Seq[V]          =
        ComponentSupport.getById(idToIds.values.flatten.map(long2Long).asJava, classTagClass[V]).asScala.toSeq
      val valueById: Map[Long, V] = values.map(entry => entry.getId.toLong -> entry).toMap

      idToIds
        .filter(entry => keyById.contains(entry._1) && entry._2.forall(valueById.contains))
        .map(entry => keyById(entry._1) -> entry._2.map(valueById))
    end components

    def componentValues[V <: ComponentInterface & Id: ClassTag](implicit cs: ComponentService): Map[Long, Seq[V]] =
      val values: Seq[V]          =
        ComponentSupport.getById(idToIds.values.flatten.map(long2Long).asJava, classTagClass[V]).asScala.toSeq
      val valueById: Map[Long, V] = values.map(entry => entry.getId.toLong -> entry).toMap

      idToIds
        .filter(entry => entry._2.forall(valueById.contains))
        .map(entry => entry._1 -> entry._2.map(valueById))
  end ComponentMultiMapOps

  abstract class ComponentExtractor[C <: ComponentInterface: ClassTag]:
    final def unapply(c: ComponentInterface): Option[C] =
      c.tryComponent[C]

    final def unapply(id: Long)(implicit cs: ComponentService): Option[C] =
      id.tryComponent[C]

    // this won't resolve, ffs, whence the preceding
    // final def unapply[A](a: A)(implicit A: PK[A]): Option[C] =
    //  a.tryComponent[C]
  end ComponentExtractor
end Component

trait Component:
  type ComponentName       = String
  type ComponentIdentifier = String
  type ArchiveIdentifier   = String
