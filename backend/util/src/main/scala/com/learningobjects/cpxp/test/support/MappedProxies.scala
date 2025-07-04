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

package com.learningobjects.cpxp.test.support

import java.lang.reflect.{Method, InvocationHandler, Proxy}
import java.util.Map as JMap
import scala.jdk.CollectionConverters.*
import scala.collection.mutable

import scala.reflect.ClassTag

/** This is a way of implementing any interface as an untyped bag for use in making very simple mocks that aren't worth
  * bothering EasyMock about, independently of
  *
  * Typical usage from Scala: def createTestUser(id: lang.Long) = makeProxy[UserFacade]("Id" -> id)
  *
  * Typical usage from Java: Map<String, Object> map = new HashMap<>(); map.put("Id", 1L); return
  * MappedProxies.makeProxy(UserFacade.class, map);
  */
trait MappedProxies:

  def makeProxy[T](mappings: (String, AnyRef)*)(implicit tt: ClassTag[T]): T =
    makeProxy(Map(mappings*))(using tt)

  def makeProxy[T](backingMap: Map[String, AnyRef])(implicit tt: ClassTag[T]): T =
    makeProxy(tt.runtimeClass.asInstanceOf[Class[T]], backingMap)

  def makeProxy[T](targetClass: Class[T], backingMap: Map[String, AnyRef]): T =
    makeProxy(targetClass, new ImmutableMappedInvocationHandler(backingMap, targetClass))

  def makeMutableProxy[T](mappings: (String, AnyRef)*)(implicit tt: ClassTag[T]): (T, mutable.Map[String, AnyRef]) =
    makeMutableProxy(mutable.Map(mappings*))(using tt)

  def makeMutableProxy[T](backingMap: mutable.Map[String, AnyRef])(implicit
    tt: ClassTag[T]
  ): (T, mutable.Map[String, AnyRef]) =
    (makeMutableProxy(tt.runtimeClass.asInstanceOf[Class[T]], backingMap), backingMap)

  def makeMutableProxy[T](targetClass: Class[T], backingMap: mutable.Map[String, AnyRef]): T =
    makeProxy(targetClass, new MutableMappedInvocationHandler(backingMap, targetClass))

  private def makeProxy[T](targetClass: Class[T], invocationHandler: InvocationHandler): T =
    Proxy
      .newProxyInstance(targetClass.getClassLoader, Array[Class[?]](targetClass), invocationHandler)
      .asInstanceOf[T]

  private trait MappedInvocationHandler:
    protected def getFromBackingMap(key: String): AnyRef
    private def getPrefixed(methodName: String, prefix: String): Option[AnyRef] =
      if methodName.startsWith(prefix) then Some(getFromBackingMap(methodName.stripPrefix(prefix)))
      else None

    private def isProxyEqual(other: AnyRef) =
      (other != null) && other.isInstanceOf[Proxy] && equals(Proxy.getInvocationHandler(other))

    /** Handles hashCode() and equals(): the proxies are equal if their backing invocationhandlers are equal.
      * @param methodName
      * @param args
      * @return
      */
    protected def handleBuiltIn(methodName: String, args: Array[AnyRef]): Option[AnyRef] =
      if methodName.equals("hashCode") then (Some(hashCode(): java.lang.Integer))
      else if methodName.equals("equals") then Some(isProxyEqual(args(0)): java.lang.Boolean)
      else if methodName.equals("toString") then Some(toString)
      else None

    /** Handles getters prefixed with get, is, and find; then falls back to assuming the method name itself is a key in
      * the backing hash, so this method will always return something or throw.
      * @param methodName
      *   the name of the method invoked
      * @return
      *   a value from the map, via the deprefixed or whole method name
      */
    protected def handleGet(methodName: String): AnyRef =
      getPrefixed(methodName, "get")
        .orElse(getPrefixed(methodName, "is"))
        .orElse(getPrefixed(methodName, "find"))
        .getOrElse(getFromBackingMap(methodName))
  end MappedInvocationHandler

  private class ImmutableMappedInvocationHandler[T](val backingMap: Map[String, AnyRef], val targetClass: Class[T])
      extends InvocationHandler
      with MappedInvocationHandler:
    override def invoke(proxy: scala.Any, method: Method, args: Array[AnyRef]): AnyRef =
      handleBuiltIn(method.getName, args).getOrElse(handleGet(method.getName))

    protected override def getFromBackingMap(key: String): AnyRef =
      backingMap(key)

    override def toString: String =
      "Proxied " + targetClass.getName + " backed by " + backingMap
  end ImmutableMappedInvocationHandler

  private class MutableMappedInvocationHandler[T](
    val backingMap: mutable.Map[String, AnyRef],
    val targetClass: Class[T]
  ) extends InvocationHandler
      with MappedInvocationHandler:

    protected def handleSet(methodName: String, args: Array[AnyRef]) =
      if methodName.startsWith("set") then
        backingMap(methodName.stripPrefix("set")) = args(0)
        Some(null)
      else None

    override def invoke(proxy: scala.Any, method: Method, args: Array[AnyRef]): AnyRef =
      handleBuiltIn(method.getName, args)
        .orElse(handleSet(method.getName, args))
        .getOrElse(handleGet(method.getName))

    protected override def getFromBackingMap(key: String): AnyRef =
      backingMap(key)
    override def toString: String                                 =
      "Proxied " + targetClass.getName + " backed by " + backingMap
  end MutableMappedInvocationHandler
end MappedProxies

object MappedProxies extends MappedProxies:
  def makeProxy[T](targetClass: Class[T], backingMap: JMap[String, AnyRef]): T =
    makeProxy(targetClass, backingMap.asScala.toMap)
