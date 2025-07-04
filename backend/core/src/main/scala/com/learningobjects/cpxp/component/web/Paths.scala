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

package com.learningobjects.cpxp.component.web

import com.learningobjects.de.web.util.UriTemplate
import scalaz.syntax.std.boolean.*

import scala.jdk.CollectionConverters.*

/** Utils for extracting variables from Url templates
  */
object Paths:

  /* Real cheap UriPattern wrapper */
  final case class ReqPattern(pattern: String):
    val templ                                                   = new UriTemplate(pattern) // Stolen from SRS
    def unapplySeq(uri: String): Option[Seq[String]]            =
      templ.matches(uri).option(templ.getVariableNames.asScala.toSeq.map(templ.`match`(uri).asScala))
    def apply(vals: Any*)(implicit hsp: HostSchemePort): String =
      hsp.toString ++ templ.createURI(vals map (_.toString)*)

  case class HostSchemePort(hsp: String):
    override def toString = hsp
end Paths
