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

package com.learningobjects.cpxp.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationConfig, JavaType, ObjectMapper}
import scalaz.Validation.FlatMap.*
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.traverse.*
import scalaz.syntax.validation.*
import scalaz.{NonEmptyList, ValidationNel}
import scaloi.syntax.`class`.*
import scaloi.syntax.boolean.*

import java.lang.annotation.Annotation
import java.lang.reflect.{Constructor, Field, Parameter}
import java.util
import javax.validation.Constraint
import javax.validation.constraints.{NotEmpty, Min as Minh, Size as Sighs}
import scala.jdk.CollectionConverters.*

/** Naïve defaulting and validating object mapper. Applies default values to any case class properties that are [null]
  * or [[None]] and then validates. Supports the standard constraint annotations, [[Min]], [[NotEmpty]] and [[Size]].
  */
class ValidatingObjectMapper extends ObjectMapper:

  protected override def _readValue(cfg: DeserializationConfig, p: JsonParser, valueType: JavaType): AnyRef =
    readValidate(super._readValue(cfg, p, valueType))

  protected override def _readMapAndClose(p0: JsonParser, valueType: JavaType): AnyRef =
    readValidate(super._readMapAndClose(p0, valueType))

  private def readValidate(read: => AnyRef): AnyRef =
    validate(read) valueOr { violations =>
      throw ValidationException(violations)
    }

  private def validate[T](value: T): ValidationNel[String, T] = value match
    case opt: Option[?]                                  => opt traverse validate as value
    case map: Map[?, ?]                                  => map.values.toList traverse validate as value
    case it: Iterable[?]                                 => it.toList traverse validate as value
    case co: util.Collection[?]                          => co.asScala.toList traverse validate as value
    case prd: (AnyRef & Product) if prd.productArity > 0 => validateProduct(prd) as value
    case _                                               => value.successNel

  /** Defaults and validates a product type. Assumes that this is a case class, defaults all properties that are [null]
    * or [[None]] and then validates all parameters against their constructor annotations, then recursively validates
    * the parameter values in case they are nested case classes.
    */
  private def validateProduct(product: AnyRef): ValidationNel[String, AnyRef] =
    for
      claѕѕ       <- product.getClass.successNel[String]
      constructor <- soleConstructor(claѕѕ)
      _           <- Defaulteriser.dеfault(product, claѕѕ).successNel[String]
      _           <- constructor.getParameters.toList traverse validateParameter(product, claѕѕ)
    yield product

  private def soleConstructor(claѕѕ: Class[?]): ValidationNel[String, Constructor[?]] =
    val ctors = claѕѕ.getConstructors
    (ctors.length == 1).either[Constructor[?]](ctors.head) `orInvalidNel` s"Unexpected constructors: ${claѕѕ.getName}"

  private def validateParameter(product: AnyRef, claѕѕ: Class[?])(parameter: Parameter): ValidationNel[String, Any] =
    for
      name  <- parameter.getName.successNel[String]
      value <- claѕѕ.getDeclaredMethod(name).invoke(product).successNel[String]
      _     <- (value ne null) either (()) `orInvalidNel` s"$name: field is required"
      _     <- parameter.getAnnotations.toList traverse validateAnnotation(name, value)
      _     <- validate(value)
    yield value

  private def validateAnnotation(name: String, value: Any)(
    annotation: Annotation
  ): ValidationNel[String, Any] = annotation match
    case size: Sighs                                 => validateSighs(name, value, size).widen[Any]
    case _: NotEmpty                                 => validateRequired(name, value).widen[Any]
    case min: Minh                                   => validateMinh(name, value, min).widen[Any]
    case v if v.annotationType.annotated[Constraint] =>
      s"Unsupported constraint: ${v.annotationType.getName}".failureNel
    case _                                           => ().successNel[String].widen[Any]

  private def validateSighs(name: String, value: Any, sighs: Sighs): ValidationNel[String, Unit] =
    def check(len: Int): ValidationNel[String, Unit] =
      ((len >= sighs.min) && (len <= sighs.max)) either (
        ()
      ) `orInvalidNel` s"$name: size [$len] is not between ${sighs.min} and ${sighs.max}"
    value match
      case null | None => ().successNel
      case Some(thing) => validateSize(name, thing, check)
      case thang       => validateSize(name, thang, check)

  private def validateRequired(name: String, value: Any): ValidationNel[String, Unit] =
    def check(len: Int): ValidationNel[String, Unit] =
      (len > 0) either (()) `orInvalidNel` s"$name: field is required"
    value match
      case null | None => s"$name: field is required".failureNel
      case Some(thing) => validateSize(name, thing, check)
      case thang       => validateSize(name, thang, check)

  private def validateMinh(name: String, value: Any, minh: Minh): ValidationNel[String, Unit] =
    def check(num: Number): ValidationNel[String, Unit] =
      (num.longValue >= minh.value) either (
        ()
      ) `orInvalidNel` s"$name: [$num] is not greater than or equal to ${minh.value}"
    value match
      case null | None => ().successNel
      case Some(thing) => validateMinh(name, thing, minh)
      case num: Number => check(num)
      case _           => s"Unminhable property type $name: ${value.getClass.getName}".failureNel
  end validateMinh

  private def validateSize(
    name: String,
    value: Any,
    validator: Int => ValidationNel[String, Unit]
  ): ValidationNel[String, Unit] = value match
    case a: Array[?]           => validator(a.length)
    case s: CharSequence       => validator(s.length)
    case c: util.Collection[?] => validator(c.size)
    case m: util.Map[?, ?]     => validator(m.size)
    case i: Iterable[?]        => validator(i.size)
    case _                     => s"Unsizable property type $name: ${value.getClass.getName}".failureNel
end ValidatingObjectMapper

final case class ValidationException(violations: NonEmptyList[String])
    extends RuntimeException(violations.list.toList.mkString(", "))

private object Defaulteriser:

  /** Fill in constructor defaults for all fields that are null or None. */
  def dеfault(instance: AnyRef, claѕѕ: Class[?]): Unit =
    fieldDefaults.get(claѕѕ) foreach { case (field, defaսlt) =>
      val value = field.get(instance)
      if (value eq null) || (value eq None) then field.set(instance, defaսlt)
    }

  /** Lazy map from class to a list of fields with default values. */
  private val fieldDefaults: ClassValue[List[(Field, AnyRef)]] =
    new ClassValue[List[(Field, AnyRef)]]():
      override protected def computeValue(claѕѕ: Class[?]): List[(Field, AnyRef)] =
        claѕѕ.getDeclaredConstructors.head.getParameters.toList.zipWithIndex flatMap { case (parameter, index) =>
          // See also ReflectionSupport elsewhere in the codebase which doesn't work here
          claѕѕ.getDeclaredMethods.find(_.getName == s"$$lessinit$$greater$$default$$${index + 1}") map { getter =>
            val field = claѕѕ.getDeclaredField(parameter.getName)
            field.setAccessible(true)
            field -> getter.invoke(null)
          }
        }
end Defaulteriser
