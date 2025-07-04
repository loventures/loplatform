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

package com.learningobjects.cpxp.component.registry

import com.google.common.reflect.TypeToken
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.internal.DelegateDescriptor

import java.lang.Iterable as jIterable
import java.lang.annotation.Annotation
import java.lang.reflect.ParameterizedType
import java.util as ju
import scala.collection.mutable.HashMap
import scala.jdk.CollectionConverters.*

object TypeParameterRegistry:
  def typeParamClass(t: ParameterizedType, i: Int): Class[?] =
    TypeToken.of(t.getActualTypeArguments()(i)).getRawType

  // Breadth first search of class and interface hierarchy starting with this class, including Object
  def superTypes(c: Class[?]): LazyList[Class[?]] =
    def recurse(l: LazyList[Class[?]]): LazyList[Class[?]] = l match
      case t #:: tail =>
        LazyList
          .cons(t, recurse(tail ++ Option(t.getSuperclass) ++ t.getInterfaces))
      case _          => LazyList.empty
    recurse(LazyList(c))

  // Breadth first search of superinterfaces not including this class
  def superInterfaces(c: Class[?]): LazyList[Class[?]] =
    def recurse(l: LazyList[Class[?]]): LazyList[Class[?]] = l match
      case i #:: tail => LazyList.cons(i, recurse(tail ++ i.getInterfaces))
      case _          => LazyList.empty
    recurse(c.getInterfaces.to(LazyList))

  // Superclasses starting with this class up to, but not including, Object
  def superClasses(a: Class[?]): LazyList[Class[?]] = Option(a) match
    case None | Some(ObjectClass) => LazyList.empty
    case Some(a)                  => LazyList.cons(a, superClasses(a.getSuperclass))

  val ComponentInterfaceType = TypeToken.of(classOf[ComponentInterface])

  val ObjectClass = classOf[Object]
end TypeParameterRegistry

// A registry that keys classes by the first type parameter of their generic
// component interface. Lookup traverses the class hierarchy to find first match.
abstract class TypeParameterRegistry extends Registry[Annotation, DelegateDescriptor]:
  import TypeParameterRegistry.*

  protected val registry = HashMap[Class[?], DelegateDescriptor]()

  // When registering a component implementation, bind it by its type parameter
  // The actual interface lookup is somewhat naïve: it picks the first parameterized component interface
  // I'm sure there's a way to make that inadequate...
  override def register(binding: Annotation, delegate: DelegateDescriptor): Unit =
    val parameterizedCIs = delegate.getDelegateClass.getGenericInterfaces collect {
      case pt: ParameterizedType
          if pt.getActualTypeArguments.length == 1
            && ComponentInterfaceType.isSupertypeOf(pt) =>
        pt
    }
    parameterizedCIs match
      case Array(t: ParameterizedType) =>
        registry.put(typeParamClass(t, 0), delegate)
        ()
      case _                           =>
        throw new IllegalArgumentException(s"Cannot determine type parameters: ${delegate.getDelegateClass.getName}")
  end register

  // Look up message senders by an array pair of system and message classes.
  override def lookup(keys: Array[AnyRef]): DelegateDescriptor =
    keys match
      case Array(c: Class[?]) => lookup(c).orNull
      case _                  => throw new IllegalArgumentException("Invalid lookup key")

  override def lookupAll: jIterable[DelegateDescriptor] =
    registry.values.asJava

  protected def lookup(c: Class[?]): Option[DelegateDescriptor]

  override def toMap: ju.Map[String, ju.Collection[DelegateDescriptor]] =
    (registry map { case (clazz, delegate) =>
      (clazz.getName, List(delegate).asJavaCollection)
    }).asJava
end TypeParameterRegistry

// A registry that keys classes by the first type parameter of their generic
// component interface. Lookup is by exact class.
class ExactTypeParameterRegistry extends TypeParameterRegistry:
  // Searches for the most specific registered sender for this combination of
  // system type and message type. Matching by system type is more important
  // than matching by message type.
  override def lookup(c: Class[?]): Option[DelegateDescriptor] =
    registry.get(c)

// A registry that keys classes by the first type parameter of their generic
// component interface. Lookup traverses the class hierarchy to find first match.
class InheritedTypeParameterRegistry extends TypeParameterRegistry:
  import TypeParameterRegistry.*

  // Searches for the most specific registered sender for this combination of
  // system type and message type. Matching by system type is more important
  // than matching by message type.
  override def lookup(c: Class[?]): Option[DelegateDescriptor] =
    val allDelegates: LazyList[Option[DelegateDescriptor]] =
      for t <- superTypes(c)
      yield registry.get(t)
    allDelegates.find(_.isDefined).flatten
end InheritedTypeParameterRegistry
