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

import java.time.format.{DateTimeFormatter, FormatStyle}
import java.time.{Instant, ZoneId}
import java.util.Locale
import java.util.logging.{Formatter, Level, LogRecord}

import fansi.Color

import org.apache.commons.lang3.exception.ExceptionUtils

/** This log formatter writes
  */
object ConsoleFormatter extends Formatter:
  override def format(record: LogRecord): String =
    val message     = Option(formatMessage(record)).getOrElse("<null>")
    val dtFormatter = DateTimeFormatter
      .ofLocalizedTime(FormatStyle.MEDIUM)
      .withLocale(Locale.UK) // For 24H Time format
      .withZone(ZoneId.systemDefault())
    val time       = dtFormatter.format(Instant.ofEpochMilli(record.getMillis))
    val threadName = threadNameById(record.getLongThreadID)
    val color      = record.getLevel match
      case Level.ALL     => Color.DarkGray
      case Level.CONFIG  => Color.DarkGray
      case Level.FINE    => Color.DarkGray
      case Level.FINER   => Color.DarkGray
      case Level.FINEST  => Color.DarkGray
      case Level.INFO    => Color.White
      case Level.OFF     => Color.DarkGray
      case Level.SEVERE  => Color.Red
      case Level.WARNING => Color.Yellow

    def render(s: String) = s"\r[${color("detomcat")}|$time|$threadName] $s\n"

    def stackTraceLines(th: Option[Throwable]): Option[String] =
      th.map(t =>
        ExceptionUtils.getRootCauseMessage(t) + "\n" + ExceptionUtils
          .getRootCauseStackTrace(t)
          .mkString
      )

    val lines        = message.split("\\R+")
    val messageLines =
      if lines.isEmpty then render(message)
      else lines.map(render).mkString
    messageLines + stackTraceLines(Option(record.getThrown)).getOrElse("")
  end format
end ConsoleFormatter
