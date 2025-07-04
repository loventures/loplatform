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

package de.tomcat

import java.util.logging.*

import de.tomcat.internal.LogForwardingPrintStream
import de.tomcat.juli.{AnsiStripHandler, ConsoleFormatter, FutureHandler}

import scala.concurrent.ExecutionContext

/** This object captures stdOut and stdErr, redirecting it's output to a dedicated logger.
  */
object Console:
  lazy val logger =
    val newLogger = Logger.getLogger(Console.getClass.getName)
    // Fetch the de.tomcat.Console logger
    newLogger.setLevel(Level.ALL)
    newLogger.getHandlers.foreach(h => newLogger.removeHandler(h)) // Remove all existing Handlers
    newLogger.setUseParentHandlers(false)
    newLogger

  // We're going to redirect system.out and system.err to our Console Logger
  lazy val systemOut = System.out
  lazy val systemErr = System.err

  // Configure a new Console Handler with the original StdOut
  lazy val consoleHandler =
    val ch = new ConsoleHandler():
      setOutputStream(systemOut)
    ch.setFormatter(ConsoleFormatter)
    ch.setLevel(Level.ALL)
    ch

  lazy val outPrintStream   = new LogForwardingPrintStream(msg => logger.info(msg), systemOut)
  lazy val errorPrintStream = new LogForwardingPrintStream(msg => logger.severe(msg), systemErr)

  /** Initialize the Console Logger.
    * @param color
    *   enable/disable color.
    */
  def init(color: Boolean)(implicit ex: ExecutionContext): Unit =

    val colorHandler = if color then consoleHandler else new AnsiStripHandler(consoleHandler)
    val asyncHandler = new FutureHandler(colorHandler)(using FutureHandler.logExecutionContext)
    logger.addHandler(asyncHandler)

    systemOut
    systemErr

    // Replace StdOut and Err with our Logger.
    System.setOut(outPrintStream)
    System.setErr(errorPrintStream)
  end init
end Console
