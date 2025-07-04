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

package de

import cats.Functor
import de.common.Klient
import scalaz.Functor as Zunctor
import scaloi.syntax.FauxnadOps

import scala.language.implicitConversions
import scala.xml.Elem

package object changelog:

  implicit class ElemOps(val self: Elem) extends AnyVal:

    /** Get the value of an attribute from this element if the element is of the right type. */
    def elemAttr(label: String, attr: String): Option[String] =
      Option(self).filter(_.label == label).flatMap(_.attribute(attr)).map(_.mkString)

    /** Get the value of an attribute from this element if the element is of the right type and attribute starts with a
      * given prefix.
      */
    def elemAttrStartsWith(label: String, attr: String, prefix: String): Option[String] =
      elemAttr(label, attr).filter(_.startsWith(prefix))
  end ElemOps

  implicit def klientFauxnad[F[_], A](klient: Klient[F, A]): FauxnadOps[Klient[F, *], A] =
    new FauxnadOps[Klient[F, *], A](klient)

  implicit def klientZunctor[F[_]: Functor]: Zunctor[Klient[F, *]] = new Zunctor[Klient[F, *]]:
    override def map[A, B](fa: Klient[F, A])(f: A => B): Klient[F, B] = fa.map(f)
end changelog
