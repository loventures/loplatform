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

package com.learningobjects.cpxp.util.lang

import java.lang as jl

import enumeratum.EnumEntry
import scalaz.{Memo, \/}

import scala.util.Try

/** Utility methods to handle enumerata and jl.Enums as untyped values. Obviously, if you know what you have, you don't
  * need this. See `EnumLikeTest` for usage examples.
  */
object EnumLike:

  /** Whether `cls` names the value type of an enumeration */
  def isEnumType(cls: Class[?]): Boolean =
    JavaEnum.isAssignableFrom(cls) || isEnumeratumType(cls)

  def isEnumeratumType(cls: Class[?]): Boolean =
    Enumeratum.isAssignableFrom(cls) || getTraitEnum(cls).isDefined

  /** Whether `e` is an enumeration value */
  def isEnumValue(e: Any): Boolean =
    JavaEnum.isAssignableFrom(e.getClass) || Enumeratum.isAssignableFrom(e.getClass)

  /** Returns the name of the enumeration value `a` */
  def toName(e: Any): String = e match
    case e: jl.Enum[?]           => e.name
    case e: enumeratum.EnumEntry => e.entryName
    case _                       => throw notHandled

  /** Returns the index of the enumeration value `e` */
  def toIndex(e: Any): Int = e match
    case e: jl.Enum[?]           => e.ordinal
    case e: enumeratum.EnumEntry =>
      getValueEnum(e.getClass)
        .map(_.asInstanceOf[enumeratum.Enum[e.type]].indexOf(e))
        .getOrElse(throw noCompanion(e.getClass))
    case _                       => throw notHandled

  /** Returns the value of the enumeration class `cls` with the name `name` */
  def fromName(name: String, cls: Class[?]): Option[Any] = cls match
    case cls if cls.getSuperclass == classOf[jl.Enum[?]] =>
      \/.attempt(
        cls.getMethod("valueOf", classOf[String]).invoke(null, name)
      )(identity).toOption
    case TraitEnum(te)                                   => te.withNameOption(name)
    case _                                               => throw notHandled

  /** Returns the value of the enumeration class `cls` with the index `ix` */
  def fromIndex(ix: Int, cls: Class[?]): Option[Any] = cls match
    case cls if cls.getSuperclass == classOf[jl.Enum[?]] =>
      \/.attempt(
        cls
          .getMethod("values")
          .invoke(null)
          .asInstanceOf[Array[jl.Enum[?]]](ix)
      )(identity).toOption
    case TraitEnum(te)                                   => te.values.lift(ix)
    case _                                               => throw notHandled

  def getEnumeratumEnum(cls: Class[?]): Option[enumeratum.Enum[? <: EnumEntry]] =
    getTraitEnum(cls).orElse(getValueEnum(cls))

  /** Returns the enumeratum `Enum[_]` object (containing the values) for the specified enumeration value type */
  def getTraitEnum(cls: Class[?]): Option[enumeratum.Enum[? <: EnumEntry]] =
    traitEnum(cls)

  private val traitEnum = Memo.immutableHashMapMemo: (cls: Class[?]) =>
    Try {
      val moduleClass = Class.forName(cls.getName + "$")
      val module      = moduleClass.getField("MODULE$").get(null)
      module.asInstanceOf[enumeratum.Enum[? <: EnumEntry]]
    }.toOption

  /** Utility extractor for the `Enum[_]` object for a class. */
  object TraitEnum:
    def unapply(cls: Class[?]): Option[enumeratum.Enum[? <: EnumEntry]] =
      getTraitEnum(cls)

  /** Returns the enumeratum `Enum[_]` object (containing the values) for the specified enumeration value class. This
    * exists because `getTraitEnum(e.getClass)` won't work.
    */
  def getValueEnum(cls: Class[?]): Option[enumeratum.Enum[? <: EnumEntry]] =
    traitEnum(cls.getEnclosingClass)

  private def notHandled                 =
    new UnsupportedOperationException("only handling Java enums and Enumerata")
  private def noCompanion(cls: Class[?]) =
    new UnsupportedOperationException(s"can't find companion for ${cls.getName}")

  private val JavaEnum   = classOf[jl.Enum[?]]
  private val Enumeratum = classOf[enumeratum.EnumEntry]
end EnumLike
