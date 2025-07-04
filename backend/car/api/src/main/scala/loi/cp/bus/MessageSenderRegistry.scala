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

package loi.cp.bus

import com.google.common.reflect.TypeToken
import com.learningobjects.cpxp.component.internal.BaseDelegateDescriptor
import com.learningobjects.cpxp.component.registry.{Registry, TypeParameterRegistry}

import java.lang.Iterable as jIterable
import java.lang.reflect.ParameterizedType
import java.util
import java.util.Collections
import scala.collection.mutable.HashMap
import scala.jdk.CollectionConverters.*

class MessageSenderRegistry extends Registry[MessageSenderBinding, BaseDelegateDescriptor]:
  import TypeParameterRegistry.*

  private val registry =
    HashMap[(Class[?], Class[?]), BaseDelegateDescriptor]()

  // When registering a MessageSenderComponent implementation, bind it by its
  // type parameters.
  override def register(binding: MessageSenderBinding, delegate: BaseDelegateDescriptor): Unit =
    delegate.getDelegateClass.getGenericInterfaces
      .filter(MessageSenderComponentType.isSupertypeOf(_)) match
      case Array(t: ParameterizedType) =>
        registry.put((typeParamClass(t, 0), typeParamClass(t, 1)), delegate)
        ()
      case _                           =>
        throw new IllegalArgumentException(s"Cannot determine type parameters: ${delegate.getDelegateClass.getName}")

  private val MessageSenderComponentType =
    TypeToken.of(classOf[MessageSender[?, ?]])

  // Look up message senders by an array pair of system and message classes.
  override def lookup(keys: Array[AnyRef]): BaseDelegateDescriptor =
    keys match
      case Array(s: Class[?], m: Class[?]) => lookup(s, m).orNull
      case _                               => throw new IllegalArgumentException("Invalid lookup key")

  override def lookupAll: jIterable[BaseDelegateDescriptor] =
    registry.values.asJava

  // Searches for the most specific registered sender for this combination of
  // system type and message type. Matching by system type is more important
  // than matching by message type.
  private def lookup(s: Class[?], m: Class[?]): Option[BaseDelegateDescriptor] =
    val allDelegates: LazyList[Option[BaseDelegateDescriptor]] = for
      systemType  <- superInterfaces(s)
      messageType <- superTypes(m)
    yield registry.get((systemType, messageType))
    allDelegates.find(_.isDefined).flatten

  override def toMap: util.Map[String, util.Collection[BaseDelegateDescriptor]] =
    registry
      .map({ case ((c1, c2), delegate) =>
        (
          (c1.getName, c2.getName).toString(),
          Collections
            .singleton(delegate)
            .asInstanceOf[util.Collection[BaseDelegateDescriptor]]
        )
      })
      .asJava
end MessageSenderRegistry
