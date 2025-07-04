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

import java.io.*
import java.nio.charset.Charset
import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}
import java.util.logging.*

import scaloi.syntax.readWriteLock.*
import sun.misc.{Signal, SignalHandler}

import scala.util.control.NonFatal

/** Derived from: org.apache.juli.FileHandler License: Apache Licence, Version 2.0
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * The default file handler opens its stream upon construction and so does not recognize character encoding settings
  * and such.
  *
  * This is java code converted by intellij, no credit is taken for scalitude.
  */
final class SignalReloadingFileHandler(
  directory: File,
  prefix: String,
  suffix: String,
  encoding: Charset,
  formatter: Formatter
) extends Handler
    with SignalHandler:
  import SignalReloadingFileHandler.*

  /** The PrintWriter to which we are currently logging, if any.
    */
  private var writer: PrintWriter = scala.compiletime.uninitialized

  /** Lock used to control access to the writer.
    */
  private val writerLock: ReadWriteLock = new ReentrantReadWriteLock

  setEncoding(encoding.name)
  setFormatter(formatter)
  open()

  /** Format and publish a <tt>LogRecord</tt>.
    *
    * @param record
    *   description of the log event
    */
  override def publish(record: LogRecord): Unit =
    if isLoggable(record) then
      writerLock reading {
        if writer ne null then
          try
            val formatted =
              try getFormatter.format(record)
              catch
                case NonFatal(e) =>
                  try
                    val failure = new LogRecord(Level.WARNING, s"Error formatting record: ${record.getMessage}")
                    failure.setThrown(e)
                    getFormatter.format(failure)
                  catch
                    case NonFatal(e) =>
                      s"Double format error: $e"
            writer.write(formatted)
            writer.flush()
          catch
            case NonFatal(e) =>
              writer = null // assume there is no hope and abandon file
              e.printStackTrace()
      }

  /** Flush the writer.
    */
  override def flush(): Unit =
    writerLock reading {
      if writer ne null then writer.flush()
    }

  /** Close the currently open log file (if any).
    */
  override def close(): Unit =
    writerLock writing {
      if writer ne null then
        writer.write(getFormatter.getTail(this))
        writer.flush()
        writer.close()
        writer = null
    }

  override def handle(signal: Signal): Unit =
    publish(new LogRecord(Level.INFO, s"Received $signal"))
    if signal == HUP then
      writerLock writing {
        close()
        open()
      }

  protected def open(): Unit =
    writerLock writing {
      val fos = new FileOutputStream(logFile, true)
      val pw  = new PrintWriter(new OutputStreamWriter(fos, getEncoding), false)
      pw.write(getFormatter.getHead(this))
      writer = pw
    }

  def logFile: File = new File(directory.getAbsoluteFile, prefix + suffix)
end SignalReloadingFileHandler

object SignalReloadingFileHandler:
  final val HUP = new Signal("HUP")
