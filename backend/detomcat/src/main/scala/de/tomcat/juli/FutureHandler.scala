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

import java.util.concurrent.Executors
import java.util.logging.{Handler, LogRecord}

import scala.concurrent.{ExecutionContext, Future}

/** A Log handler that publishes the LogRecord asynchronously, returning to the callee immediately.
  */
final class FutureHandler(other: Handler)(implicit ec: ExecutionContext) extends Handler:
  override def flush(): Unit =
    Future { other.flush() }
    ()

  override def publish(record: LogRecord): Unit =
    Future { other.publish(record) }
    ()

  override def close(): Unit =
    Future { other.close() }
    ()
end FutureHandler
object FutureHandler:
  val logExecutionContext: ExecutionContext =
    val loggerThread = Executors.newSingleThreadExecutor({ (r: Runnable) =>
      val t = new Thread(r)
      t.setDaemon(true)
      t.setName(FutureHandler.getClass.getName)
      t.setPriority(Thread.MIN_PRIORITY)
      t
    })
    ExecutionContext.fromExecutorService(loggerThread)
end FutureHandler
