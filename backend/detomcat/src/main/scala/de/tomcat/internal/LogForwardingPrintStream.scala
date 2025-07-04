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

package de.tomcat.internal

import java.io.PrintStream

/** A PrintStream which forwards lines to a given logging side effect. All other printstream operations get forwarded to
  * the undelying stream.
  */
class LogForwardingPrintStream(log: String => Unit, original: PrintStream) extends PrintStream(original):
  override def println(): Unit = log("")

  override def println(x: Boolean): Unit = log(x.toString)

  override def println(x: Char): Unit = log(x.toString)

  override def println(x: Int): Unit = log(x.toString)

  override def println(x: Long): Unit = log(x.toString)

  override def println(x: Float): Unit = log(x.toString)

  override def println(x: Double): Unit = log(x.toString)

  override def println(x: Array[Char]): Unit = log(x.mkString)

  override def println(x: String): Unit = log(Option(x).getOrElse("null"))

  override def println(x: scala.Any): Unit = log(Option(x).fold("null")(_.toString))
end LogForwardingPrintStream
