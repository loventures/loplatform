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

import scalaz.Isomorphism.*
import scalaz.syntax.either.*
import scalaz.syntax.validation.*
import scalaz.*
import scaloi.\|/

/** An ADT used to represent a value that can be one of:
  *   1. Present, which means included in the data model, and with a non-null value 2. Null, which means included in the
  *      data model, but a value of 'null' was specified 3. Absent, which means this field was completely left out of
  *      the data model.
  *
  * This ADT useful if you need to make a distinction in a Csv row of a missing or included column, and also for JSON
  * structures where fields can be missing or included.
  *
  * To use with Jackson, comprise your case class like:
  *
  * {{{
  * case class MyJsonObj {
  *   @BeanProperty
  *   var myField: OptionalField[String] = Absent
  * }
  * }}}
  */
sealed trait OptionalField[T]:

  /** Whether or not the value is present and non-null.
    */
  final def isDefined: Boolean = this.isInstanceOf[Present[?]]

  /** Whether or not the value is present or null.
    */
  final def nonAbsent: Boolean = this ne Absent()

  /** Get the value. Follows same contract as Option#get
    */
  final def get: T = this.toOption.get

  /** Get the value or a default.
    */
  final def getOrElse[S >: T](default: => S): S = this match
    case Present(t) => t
    case _          => default

  /** Convert this OptionalField to an [[scala.Option]]
    *
    * @return
    *   Some(a) if this value is Present, None otherwise
    */
  final def toOption: Option[T] = toOption(None)

  final def toOption(ifAbsent: => Option[T]): Option[T] = this match
    case Present(a) => Some(a)
    case Null()     => None
    case Absent()   => ifAbsent

  /** Map support for OptionalField
    */
  final def map[U](f: T => U): OptionalField[U] =
    flatMap(a => Present(f(a)))

  /** Foreach support for OptionalField
    */
  final def foreach[U](f: T => U): Unit = this match
    case Present(a) => f(a)
    case _          => ()

  /** If this value is [[Present]] then invoke `f` with the value; else if this value is [[Null]] then invoke `f` with
    * `null`.
    */
  final def applyTo[U, V >: T](f: V => U)(implicit ev: scala.Null <:< V): Unit = this match
    case Present(a) => f(a)
    case Null()     => f(ev(null))
    case Absent()   => ()

  /** If this value is [[Present]] then invoke `f` with [[Some]] (or an isomorphism) of the value; else if this value is
    * [[Null]] then invoke `f` with [[None]] (or an isomorphism).
    */
  final def coapplyTo[F[_], V >: T, U](f: F[V] => U)(implicit ev: F <~> Option): Unit = this match
    case Present(a) => f(ev.from(Some(a)))
    case Null()     => f(ev.from(None))
    case Absent()   => ()

  /** flatMap support for OptionalField
    */
  final def flatMap[U](fn: T => OptionalField[U]): OptionalField[U] = this match
    case Present(a) => fn(a)
    case Null()     => Null()
    case Absent()   => Absent()

  /** Returns this value, if non absent, or else the supplied option.
    */
  final def orElse[U >: T](option: => Option[U]): Option[U] = this match
    case Present(a) => Some(a)
    case Null()     => None
    case Absent()   => option

  final def |:[U >: T](option: => Option[U]): Option[U] = orElse(option)

  /** Returns this value, if non absent, or else the supplied option.
    */
  final def orElse(optional: => OptionalField[T]): OptionalField[T] =
    if nonAbsent then this else optional

  final def |:(optional: => OptionalField[T]): OptionalField[T] = orElse(optional)

  /** Catamorphism. Null and Absent are treated as not present.
    */
  final def cata[U](ifPresent: T => U, ifNotPresent: => U): U = this match
    case Present(a) => ifPresent(a)
    case _          => ifNotPresent

  /** "cata" + "condOpt", or something like it */
  final def catOpt[U](ifNull: => U)(ifPresent: T => U): Option[U] = PartialFunction.condOpt(this) {
    case Present(t) => ifPresent(t)
    case Null()     => ifNull
  }

  /** Fold. Null and Absent are treated as not present.
    */
  final def fold[U](ifNotPresent: => U)(ifPresent: T => U): U = this match
    case Present(a) => ifPresent(a)
    case _          => ifNotPresent

  /** Convert OptionalField[ValidationNel[L,R]] -> ValidationNel[L,OptionalField[R]] (Analogous to Scalaz
    * Traverse#sequence)
    */
  final def toValidationNel[L, R](implicit ev: T <:< ValidationNel[L, R]): ValidationNel[L, OptionalField[R]] =
    this match
      case Present(r) => r.fold(_.failure, r => OptionalField.present(r).successNel[L])
      case Null()     => Null[R]().successNel
      case Absent()   => Absent[R]().successNel

  /** Put `self` on the left, and `right` on the right, of an Eitherneitherboth, after optionalising each.
    *
    * @param u
    *   the optional field to put on the right
    * @return
    *   an Eitherneitherboth with `self` on the left and `right` on the right
    */
  final def \|/[U](u: OptionalField[U]): T \|/ U = scaloi.\|/(toOption, u.toOption)
end OptionalField

final case class Present[T](t: T) extends OptionalField[T]

sealed abstract case class Null[T] private () extends OptionalField[T]:
  def coerce[S]: OptionalField[S] = this.asInstanceOf[OptionalField[S]]

object Null:
  private val value                = new Null[Nothing] {}
  def apply[T](): OptionalField[T] = value.coerce[T]

sealed abstract case class Absent[T] private () extends OptionalField[T]:
  def coerce[S]: OptionalField[S] = this.asInstanceOf[OptionalField[S]]

object Absent:
  private val value                = new Absent[Nothing] {}
  def apply[T](): OptionalField[T] = value.coerce[T]

object OptionalField:
  def apply[T](t: Option[T]): OptionalField[T] = t.fold[OptionalField[T]](Absent())(Present.apply)

  def present[T](t: T): OptionalField[T] = Present(t)

  implicit def optionalFieldInstance: Traverse[OptionalField] & Optional[OptionalField] =
    new Traverse[OptionalField] with Optional[OptionalField]:
      override def traverseImpl[G[_], A, B](
        fa: OptionalField[A]
      )(f: A => G[B])(implicit G: Applicative[G]): G[OptionalField[B]] = fa match
        case Present(value) => G.map(f(value))(Present.apply)
        case Absent()       => G.point(Absent())
        case Null()         => G.point(Null())

      override def pextract[B, A](fa: OptionalField[A]): OptionalField[B] \/ A = fa match
        case Present(value) => value.right
        case Absent()       => Absent[B]().left
        case Null()         => Null[B]().left

  import argonaut.*

  implicit def decodeOptionalField[T](implicit dt: DecodeJson[T]): DecodeJson[OptionalField[T]] =
    DecodeJson.withReattempt {
      case ACursor(Left(invalid)) =>
        DecodeResult.ok(Absent())
      case ACursor(Right(hc))     =>
        if hc.cursor.focus.isNull then DecodeResult.ok(Null())
        else hc.as(using dt).map(Present.apply)
    }

  // This is suss.. i'm not sure it encodes absence.
  implicit def encodeOptionalField[T](implicit et: EncodeJson[T]): EncodeJson[OptionalField[T]] =
    EncodeJson { _.cata(et.encode, Json.jNull) }
end OptionalField
