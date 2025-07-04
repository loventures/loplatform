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

package com.learningobjects.cpxp.scala.util

import org.apache.http.client.utils.URIBuilder
import scalaz.syntax.std.option.*

/** DSL for building URIs. [ "/foo" ? ("bar" -> "baz") & ("baz" -> "barman") ]
  */
object URIBuilderOps:

  implicit class StringOps(val self: String) extends AnyVal:
    def ?[A: ToString](kv: (String, A)): URIBuilder          = new URIBuilder(self) & kv
    def ?[A: ToString](kvo: Option[(String, A)]): URIBuilder = new URIBuilder(self) & kvo

  implicit class UBOps(val self: URIBuilder) extends AnyVal:
    def &[A: ToString](kv: (String, A)): URIBuilder          = self.addParameter(kv._1, ToString[A].apply(kv._2))
    def &[A: ToString](kvo: Option[(String, A)]): URIBuilder = kvo.cata(kv => self & kv, self)

  import language.implicitConversions

  implicit def stringify(ub: URIBuilder): String = ub.toString
end URIBuilderOps

trait ToString[A] extends (A => String)

object ToString:
  implicit def apply[A: ToString]: ToString[A]  = implicitly
  implicit val StringToString: ToString[String] = s => s
