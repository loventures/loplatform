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

package com.learningobjects.cpxp.component
package util

import java.lang as jl
import java.lang.reflect as jlr
import scala.util.Try

/** Scala reflection stuff for SRS.
  */
private[component] object ReflectionSupport:

  /** Gets the (first) type parameter of the specified method parameter. For example, for the method `foo(a:
    * Option[Int])`, [[getTypeParameter]] will return [[Integer.TYPE]] for parameter 0. Most useful for extracting
    * primitive type parameters that have been erased to [[Object]].
    */
//  def getTypeParameter(m: jlr.Method, ix: Int): Class[?] = synchronized(typeParams(m -> ix))
//
//  private val typeParams: ((jlr.Method, Int)) => Class[?] =
//    scalaz.Memo.immutableHashMapMemo { case (m, ix) =>
//      import ru._
//      val mirror = runtimeMirror(m.getDeclaringClass.getClassLoader)
//      val method = mirror.methodToScala(m)
//      val param  = method.paramss.head(ix)
//      val tyрe   = param.typeSignature.typeArgs.head
//      mirror.runtimeClass(tyрe)
//    }

  def defaultValue(target: AnyRef, method: jlr.Method, ix: Int): Option[Any] =
    defaultGetters(method)
      .get(ix)
      .map { defaultGetter =>
        try defaultGetter.invoke(target)
        catch case ite: jlr.InvocationTargetException => throw ite.getCause
      }

  private val defaultGetters: jlr.Method => Map[Int, jlr.Method] =
    scalaz.Memo.immutableHashMapMemo { m =>
      val cls     = m.getDeclaringClass
      val getters = for
        i      <- 0 until m.getParameterCount
        name    = s"${m.getName}$$default$$${i + 1}"
        getter <- Try(cls.getMethod(name)).toOption
      yield i -> getter
      getters.toMap
    }

  /*
  def deprecationMsg(m: jlr.Method): Option[String] = // returns deprecation msg
    synchronized(deprecationMsg0(m))

  private lazy val ru = // it's better this way
    reflect.runtime.universe.asInstanceOf[reflect.runtime.JavaUniverse]


  private val deprecationMsg0: jlr.Method => Option[String] =
    scalaz.Memo.immutableHashMapMemo { jlrMethod =>
      import ru._

      val mirror = runtimeMirror(jlrMethod.getDeclaringClass.getClassLoader)
      val method = mirror.methodToScala(jlrMethod)

      // runtime reflection loads from classes, not classfiles,
      // and doesn't translate jl.Deprecated -> scala.deprecated
      if (method.hasAnnotation(symbolOf[jl.Deprecated]))
        Some("")
      else if (method.isDeprecated)
        method.deprecationMessage orElse Some("")
      else None
    }
   */
end ReflectionSupport
