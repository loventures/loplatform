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

import java.util.logging.*

import scala.util.Try

/** Captures all log messages emitted by the system for the duration of an operation.
  */
object LogCapture:

  /** The root logger. */
  private final val rootLogger = Logger.getLogger("")

  /** Java API: Execute a runnable operation, capturing all system logs to a specified handler for the duration of that
    * operation. Note that this will include all logs from any thread.
    *
    * @param handler
    *   the handler to accept log messages
    * @param runnable
    *   the operation to perform
    */
  def captureLogs(handler: Handler, runnable: Runnable): Unit =
    captureLogs(handler)(runnable.run())

  /** Execute a function, capturing all system logs to a specified handler for the duration of that function. Note that
    * this will include all logs from any thread.
    *
    * @param handler
    *   the handler to accept log messages
    * @param f
    *   the function to perform
    */
  def captureLogs[T](handler: Handler)(f: => T): T =
    val safe = new SafeHandler(handler)
    rootLogger.addHandler(safe)
    try f
    finally
      rootLogger.removeHandler(safe)
      safe.close()
end LogCapture

/** A log handler that wraps an existing log handler and endeavours to internalize any failures that occur in that
  * handler.
  *
  * @param handler
  *   the log handler to wrap
  */
class SafeHandler(handler: Handler) extends Handler:

  /** Whether the handler has failed. */
  private var failed = false

  /** Flush the underlying handler if this has not failed. */
  override def flush(): Unit = if !failed then handler.flush()

  /** Close the underlying handler if this has not failed. */
  override def close(): Unit = if !failed then handler.close()

  /** Publish a log record to the wrapped handler if this has not failed. If the publish operation fails, enter failed
    * state and close the handler.
    */
  override def publish(record: LogRecord): Unit =
    if !failed then
      Try {
        handler.publish(record)
      } getOrElse {
        failed = true
        handler.close()
      }
end SafeHandler
