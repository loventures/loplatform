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

package loi.authoring.index

import loi.authoring.index.instances.*
import loi.authoring.syntax.index.*

import scala.language.implicitConversions

/** Typeclass describing a type [[A]] that can be deconstructed into a [[List]] of plaintext or HTML [[String]]s.
  */
trait Strings[A]:
  def strings(a: A): List[String]

  def htmls(a: A): List[String]

object Strings extends ContentPartStringsInstances with QuestionContentStringsInstances with BlobRefStringsInstances:

  /** Summon the implicit [[Strings]] of the type [[A]]. */
  def apply[A: Strings]: Strings[A] = implicitly

  def plaintext[A](fas: A => List[String]): Strings[A] = new Strings[A]:
    override def strings(a: A): List[String] = fas(a)

    override def htmls(a: A): List[String] = Nil

  /** [[Strings]] evidence for a single plaintext [[String]]. */
  implicit def stringStrings: Strings[String] = new Strings[String]:
    override def strings(a: String): List[String] = if a.nonEmpty then a :: Nil else Nil

    override def htmls(a: String): List[String] = Nil

  /** [[Strings]] evidence for an [[Option]] of a type [[A]] with [[Strings]] evidence. */
  implicit def optStrings[A: Strings]: Strings[Option[A]] = new Strings[Option[A]]:
    override def strings(fa: Option[A]): List[String] = fa.toList.flatMap(_.strings)

    override def htmls(fa: Option[A]): List[String] = fa.toList.flatMap(_.htmls)

  /** [[Strings]] evidence for a [[Seq]] of a type [[A]] with [[Strings]] evidence. */
  implicit def seqStrings[A: Strings]: Strings[Seq[A]] = new Strings[Seq[A]]:
    override def strings(fa: Seq[A]): List[String] = fa.toList.flatMap(_.strings)

    override def htmls(fa: Seq[A]): List[String] = fa.toList.flatMap(_.htmls)
end Strings

final class StringsOps[A](private val self: A) extends AnyVal:

  /** Extract a [[List]] of [[String]] from [[self]]. */
  def strings(implicit Strings: Strings[A]): List[String] = Strings.strings(self)

  /** Extract a [[List]] of HTML [[String]] from [[self]]. */
  def htmls(implicit Strings: Strings[A]): List[String] = Strings.htmls(self)

trait StringsSyntax:

  /** Convert a type [[A]] with [[Strings]] evidence into a [[String]] by joining its strings with a space. */
  def stringify[A: Strings](a: A): String = a.strings.distinct.mkString(" ")

  /** Convert a type [[A]] with [[Strings]] evidence into an [[Option]] of a [[String]]. */
  def stringifyOpt[A: Strings](a: A): Option[String] = Some(stringify(a)).filter(_.nonEmpty)

  implicit def toStringsOps[A: Strings](a: A): StringsOps[A] = new StringsOps(a)
