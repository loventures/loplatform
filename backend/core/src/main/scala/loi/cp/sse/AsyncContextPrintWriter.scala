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

package loi.cp.sse

import jakarta.servlet.AsyncContext

import java.io.PrintWriter
import scala.util.Try

/** A print writer wrapper associated with an asynchronous execution context. Completes the asynchronous execution when
  * the writer is closed.
  *
  * This does some additional exception catching to work around NPEs that tomcat throws in some edge cases of expired
  * connections.
  *
  * @param writer
  *   the print writer to wrap
  * @param context
  *   the associated asynchronous execution context
  */
class AsyncContextPrintWriter(writer: PrintWriter, context: AsyncContext) extends PrintWriter(writer):
  private var error: Boolean = false

  /** Print a newline to the underlying print writer.
    */
  override def println(): Unit =
    Try { writer.println() }
    ()

  /** Print to the underlying print writer.
    *
    * @param s
    *   the string to print
    */
  override def println(s: String): Unit =
    Try { writer.println(s) }
    ()

  /** Close the underlying print writer and then close the asynchronous execution context.
    */
  override def close(): Unit =
    error = Try(writer.close()).flatMap(_ => Try(context.complete())).isFailure || error

  /** Check for errors.
    */
  override def checkError(): Boolean =
    Try { error || writer.checkError() } getOrElse true
end AsyncContextPrintWriter

/** Asynchronous context print writer companion.
  */
object AsyncContextPrintWriter:

  /** Create a print writer associated with an asynchronous execution context.
    *
    * @param context
    *   the asynchronous execution context
    * @return
    *   the wrapped print writer
    */
  def apply(context: AsyncContext): AsyncContextPrintWriter =
    new AsyncContextPrintWriter(context.getResponse.getWriter, context)
end AsyncContextPrintWriter
