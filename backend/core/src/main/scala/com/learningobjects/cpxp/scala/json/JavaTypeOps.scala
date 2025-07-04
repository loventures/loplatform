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

package com.learningobjects.cpxp.scala.json

import com.fasterxml.jackson.databind.JavaType
import com.learningobjects.cpxp.util.lang.EnumLike
import enumeratum.{Enum, EnumEntry}

import scala.language.implicitConversions

final class JavaTypeOps(val jt: JavaType) extends AnyVal:

  def toEnumeratumEnum: Option[Enum[? <: EnumEntry]] = EnumLike.getEnumeratumEnum(jt.getRawClass)

  def isEnumeratumEnum: Boolean = EnumLike.isEnumeratumType(jt.getRawClass)

object JavaTypeOps extends ToJavaTypeOps

trait ToJavaTypeOps:

  implicit def toJavaTypeOps(jt: JavaType): JavaTypeOps = new JavaTypeOps(jt)
