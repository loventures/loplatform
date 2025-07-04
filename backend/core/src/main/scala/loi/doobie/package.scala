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

package loi

import _root_.argonaut.Json
import _root_.doobie.*
import _root_.doobie.implicits.*
import _root_.doobie.util.log.{ExecFailure, LogEvent, LogHandler, Parameters, ProcessingFailure, Success}
import cats.effect.IO
import de.tomcat.juli.LogMeta
import org.log4s.Logger

import java.time.ZoneId

package object doobie:

  object log:
    implicit def log4sLogHandler(implicit logger: Logger): LogHandler[IO] = (logEvent: LogEvent) =>
      IO.delay {
        logEvent match

          case Success(s, a, label, e1, e2) =>
            if logger.isTraceEnabled then
              LogMeta.let(
                "sql"          -> Json.jString(s),
                "args"         -> argsJson(a),
                "execMs"       -> Json.jNumber(e1.toMillis),
                "processingMs" -> Json.jNumber(e2.toMillis)
              )(logger.trace("successful query execution"))

          case ProcessingFailure(s, a, label, e1, e2, t) =>
            LogMeta.let(
              "sql"          -> Json.jString(s),
              "args"         -> argsJson(a),
              "execMs"       -> Json.jNumber(e1.toMillis),
              "processingMs" -> Json.jNumber(e2.toMillis)
            )(logger.error(t)(s"failed ResultSet processing: ${t.getMessage}"))

          case ExecFailure(s, a, label, e1, t) =>
            LogMeta.let(
              "sql"    -> Json.jString(s),
              "args"   -> argsJson(a),
              "execMs" -> Json.jNumber(e1.toMillis),
            )(logger.error(t)(s"failed query execution: ${t.getMessage}"))
      }

    private def argsJson(args: Parameters): Json =
      Json.jArrayElements(
        args.allParams.flatMap(_.map(arg => if arg == null then Json.jNull else Json.jString(arg.toString)))*
      )
  end log

  object io:

    def setSearchPath(schema: String): ConnectionIO[Int] =
      (fr"SET search_path TO" ++ Fragment.const(schema)).update.run

    // This is to accommodate local devs running redshift tests.
    //
    // Notes for future me in case:
    //
    // If everyone was in UTC, or if the Redshift driver supported java.time.*, or if the
    // Redshift driver had a config option to override the client time zone to UTC, then
    // we could avoid this. java.sql.Timestamp is what we are stuck with and it is just a
    // wrapper around epoch seconds (always in UTC). The problem is the driver's parsing
    // to Timestamp grabs a local calendar, which causes the driver to assume that any
    // timestamp value from the db is in the client timezone. Note, we are not using
    // timestampz values because that is for if we wanted to store times in multiple
    // time zones, and we do not. The timestamp datatype retains no timezone, it is just
    // a naked, unitless, zoneless, datetime. So if we store 10:43 in a timestamp column
    // the driver reads 10:43 EST if the driver is in Washington, DC in February. Since
    // Timestamp's heart is epoch seconds in UTC the driver accordingly adds 5 hours
    // of seconds to 10:43 EST. So 10:43 (naked) became 15:43 UTC. The solution in our
    // specific case, is to _not_ store the times in UTC. Store them in the client time.
    // For jenkins and the prod app servers this is UTC, so we are fine. For local devs
    // this is fine because the local devs are just running tests that drop/add the
    // schema every x minutes. This way, 10:43 is meant to be EST not UTC, so it is fine
    // that the driver interprets it as 15:43 UTC.
    //
    // Only one problem remains, which this function works around. The Redshift functions
    // getdate() and current_date are critical to several views. They use the session
    // time zone (the connection time zone) and that defaults to UTC. Since we are storing
    // dates in client times, then these functions need to use client time. Thus, we set
    // the time zone with this.
    // https://docs.aws.amazon.com/redshift/latest/dg/r_timezone_config.html
    // hoepfully all the ZoneIds ppl actually use match the pg ones in that documentation
    def setTimeZone(zoneId: ZoneId): ConnectionIO[Int] =
      (fr"SET timezone TO" ++ Fragment.const("'" + zoneId.getId + "'")).update.run
  end io
end doobie
