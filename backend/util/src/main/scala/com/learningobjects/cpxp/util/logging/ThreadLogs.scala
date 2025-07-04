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

package com.learningobjects.cpxp.util.logging

import java.io.{PrintWriter, StringWriter}
import java.util.logging.Formatter

import cats.effect.IO

object ThreadLogs:
  private final val OneMeellionCharacters = 1048576

  /** Run a function while capturing logs, then return the function result and logs.
    * @param f
    *   the function to be monitored
    * @param fmt
    *   the log formatter
    * @tparam A
    *   the result type
    * @return
    *   a tuple of the function result and the logs
    */
  def logged[A](f: => A)(implicit fmt: Formatter = new MinimalFormatter(getClass, false)): (A, String) =
    val sw       = new BadlyBoundedStringWriter(OneMeellionCharacters)
    val listener = new ThreadLogWriter(new PrintWriter(sw), fmt)
    val a        = LogCapture.captureLogs(listener)(f)
    (a, sw.toString)

  /** Run a function while capturing logs, then pass the function result and logs to a side-effecting function, then
    * return the function result.
    * @param f
    *   the function to be monitored
    * @param g
    *   the side-effecting function that process the function result and logs
    * @param fmt
    *   the log formatter
    * @tparam A
    *   the result type
    * @return
    *   the function result
    */
  def tapLogs[A](
    f: => A
  )(g: (A, String) => Unit)(implicit fmt: Formatter = new MinimalFormatter(getClass, false)): A =
    val (result, logs) = logged(f)
    g(result, logs)
    result

  /** Create a [[IO]] that will record log messages in the captured logs of the calling thread.
    * @param f
    *   the function to run
    * @tparam A
    *   the result type
    * @return
    *   the resulting task
    */
  def loggedIO[A](f: => A): IO[A] =
    val callingThread = Thread.currentThread
    IO {
      ThreadLogWriter.effectiveThread.withValue(callingThread)(f)
    }
end ThreadLogs

class BadlyBoundedStringWriter(limit: Int) extends StringWriter:
  private var full = false

  override def write(c: Int): Unit = bounded(super.write(c))

  override def write(c: Array[Char]): Unit = bounded(super.write(c))

  override def write(c: Array[Char], o: Int, l: Int): Unit = bounded(super.write(c, o, l))

  override def write(s: String): Unit = bounded(super.write(s))

  override def write(s: String, o: Int, l: Int): Unit = bounded(super.write(s, o, l))

  private def bounded(f: => Unit): Unit =
    if !full then
      f
      if getBuffer.length() >= limit then
        getBuffer.append("OOM")
        full = true
end BadlyBoundedStringWriter
