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

import org.apache.commons.lang3.reflect.TypeUtils
import java.lang.reflect.Type
import scala.reflect.ClassTag

trait JlrType[A]:
  val tpe: Type

object JlrType:

  def apply[A](using A: JlrType[A]): JlrType[A] = A

  // TODO: scala 3: This is incorrect because it misses the important type parameters.
  // TODO: https://github.com/wvlet/airframe/blob/main/airframe-surface/src/main/scala-3/wvlet/airframe/surface/Surface.scala

  given [F[_], A](using F: ClassTag[F[A]], A: ClassTag[A]): JlrType[F[A]] with
    val tpe: Type = TypeUtils.parameterize(F.runtimeClass, A.runtimeClass)

  given [A](using A: ClassTag[A]): JlrType[A] with
    val tpe: Type = A.runtimeClass

  /*
  import org.apache.commons.lang3.reflect.TypeUtils
    import scala.reflect.runtime.{universe => ru}

    implicit def jlrType[A: ru.TypeTag]: JlrType[A] = new JlrType[A] {
      override val tpe: Type = {

        val mirror = ru.typeTag[A].mirror
        val aClass = mirror.runtimeClass(ru.typeOf[A])
        val args   = ru.typeOf[A].typeArgs

        if (args.nonEmpty) {
          // n-order types
          val argClasses = args.map(mirror.runtimeClass)
          TypeUtils.parameterize(aClass, argClasses: _*)
        } else {
          // proper types
          aClass
        }
      }
    }
   */
end JlrType
