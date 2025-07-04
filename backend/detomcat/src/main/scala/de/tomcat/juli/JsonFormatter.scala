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

import java.time.Instant
import java.util.logging.{Formatter, LogRecord}

import org.apache.commons.lang3.exception.ExceptionUtils

/** Log messages in with Json. This logger will print single line log messages in a compact format. Mutliline messages
  * and exceptions will be pretty printed however, if so configured.
  */
class JsonFormatter(prettyPrint: Boolean) extends Formatter:
  import argonaut.*
  import Argonaut.*

  implicit val encodeLogRecord: EncodeJson[LogRecord] =
    EncodeJson[LogRecord]({ record =>
      val message = Option(formatMessage(record)).getOrElse("null")
      val lines   = message.split("\\R+")
      Json(
        "level"  := record.getLevel.toString,
        "time"   := Instant.ofEpochMilli(record.getMillis).toString,
        "thread" := threadNameById(record.getLongThreadID),
        "logger" := Option(record.getLoggerName).map(trimLoggerName),
        if lines.isEmpty || lines.length == 1 then "message" := message else "message" := lines.toList
      ).deepmerge(LogMeta.get) // should just be a fold
    })

  implicit val throwableEncode: EncodeJson[Throwable] =
    EncodeJson[Throwable]({ orig =>
      val th         = Option(ExceptionUtils.getRootCause(orig)).getOrElse(orig)
      val message    = Option(th.getMessage).getOrElse("null")
      val className  = th.getClass.getName
      val stackTrace = ExceptionUtils.getRootCauseStackTrace(th).map(_.replace("\t", "  "))
      val attrs      = Seq("message" := message, "class" := className, "stackTrace" := stackTrace.toVector)
      Json.obj(attrs*)
    })

//  val stackTraceElementEncode: EncodeJson[StackTraceElement] = EncodeJson[StackTraceElement]({ ste =>
//    val className  = ste.getClassName
//    val fileName   = ste.getFileName
//    val methodName = ste.getMethodName
//    val lineNumber = ste.getLineNumber
//    Json.obj("class" := className, "file" := fileName, "method" := methodName, "line" := lineNumber)
//  })
//
//  implicit val stackTraceElementStringEncode: EncodeJson[StackTraceElement] =
//    EncodeJson[StackTraceElement](ste => jString(ste.toString))

  override def format(record: LogRecord): String =
    val msgJson   = record.asJson
    val thString  = Option(record.getThrown).map(_.asJson)
    val msgAndTh  = thString.map(th => msgJson.withObject(_ :+ ("exception" := th))).getOrElse(msgJson)
    val multiline = msgJson.field("message").flatMap(_.array).isDefined
    val msgString = msgAndTh.pretty(
      if multiline && prettyPrint then prettySpace
      else prettyNoSpace
    )
    msgString + "\n"
  end format

  private val prettySpace   = PrettyParams.spaces2.copy(preserveOrder = true)
  private val prettyNoSpace = PrettyParams.nospace.copy(preserveOrder = true)
end JsonFormatter
