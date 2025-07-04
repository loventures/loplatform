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

import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.{Formatter, LogRecord}

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils

object StandardFormatter extends Formatter:
  private val RFC_3339_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  private val LINE_SEPARATOR       = System.getProperty("line.separator")
  private val RECORD_BEGIN_MARKER  = "[#|"
  private val RECORD_END_MARKER    = "|#]" + LINE_SEPARATOR
  private val FIELD_SEPARATOR      = '|'
  private val NVPAIR_SEPARATOR     = ';'
  private val NV_SEPARATOR         = '='

  private val dateFormatter = new SimpleDateFormat(RFC_3339_DATE_FORMAT)
  private val date          = new Date

  override def format(record: LogRecord): String =
    val sb = new StringBuilder(RECORD_BEGIN_MARKER)
    date.setTime(record.getMillis)
    sb.append(dateFormatter.format(date))
    sb.append(FIELD_SEPARATOR)
    sb.append(record.getLevel).append(FIELD_SEPARATOR)
    sb.append(record.getLongThreadID).append(NVPAIR_SEPARATOR)
    sb.append(Thread.currentThread.getName).append(FIELD_SEPARATOR)
    sb.append(StringUtils.removeStart(record.getLoggerName, "com.learningobjects.")).append(FIELD_SEPARATOR)
    sb.append(formatMessage(record))
    Option(record.getThrown) foreach { ex =>
      ExceptionUtils.getRootCauseStackTrace(record.getThrown) foreach { str =>
        sb.append(LINE_SEPARATOR).append(str)
      }
    }
    sb.append(RECORD_END_MARKER)
    sb.toString
  end format
end StandardFormatter
