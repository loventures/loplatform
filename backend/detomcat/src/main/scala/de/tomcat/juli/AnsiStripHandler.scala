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

package de.tomcat.juli

import java.util.logging.{Handler, LogRecord}

/** A JULI Handler that strips ANSI color codes from a log message before forwarding it another handler
  */
final class AnsiStripHandler(other: Handler) extends Handler:
  import AnsiStripHandler.*
  override def flush(): Unit = other.flush()

  override def close(): Unit = other.close()

  override def publish(record: LogRecord): Unit =
    val newRecord = new LogRecord(record.getLevel, Option(record.getMessage).map(_.replaceAll(ansiPattern, "")).orNull)
    newRecord.setInstant(record.getInstant)
    newRecord.setLongThreadID(record.getLongThreadID)
    newRecord.setThrown(record.getThrown)
    record.setParameters(record.getParameters)
    record.setResourceBundle(record.getResourceBundle)
    other.publish(newRecord)
end AnsiStripHandler
object AnsiStripHandler:
  final val ansiPattern = "\\u001B\\[[;\\d]*m"
