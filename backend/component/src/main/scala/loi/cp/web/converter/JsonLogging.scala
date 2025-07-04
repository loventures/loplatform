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

package loi.cp.web.converter

import java.io.InputStream
import java.nio.charset.StandardCharsets

import de.tomcat.juli.LogMeta
import org.apache.commons.io.IOUtils

import scala.util.control.NonFatal

/** Utility for safely logging bowdlerized json. The API is somewhat baroque because it is bridging mismatched Java and
  * Scala.
  */
private[converter] object JsonLogging:
  import argonaut.*
  import Argonaut.*

  /** Run a function with the JSON parsed from the supplied input stream stored in the diagnostic context of the logging
    * framework.
    */
  def withLogMetaJson[A](in: InputStream)(f: => A): Unit =
    try withLogMetaJson(IOUtils.toString(in, StandardCharsets.UTF_8))(f)
    catch
      case NonFatal(e) =>
        logger.warn(e)(s"Error parsing JSON")

  /** Run a function with the JSON parsed from the supplied string stored in the diagnostic context of the logging
    * framework.
    */
  def withLogMetaJson[A](value: String)(f: => A): Unit =
    value.parseWith(
      { json =>
        LogMeta.let("json" -> Bowdlerize(json))(f)
      },
      { error =>
        logger warn s"Error parsing JSON: $error"
      }
    )

  /** Java API. */
  def withLogMetaJson[A](in: InputStream, f: Runnable): Unit = withLogMetaJson(in)(f.run())

  private final val logger = org.log4s.getLogger
end JsonLogging
