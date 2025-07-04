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

package loi.cp

import java.io.PrintWriter

/** SSE package.
  */
package object sse:

  /** SSE enhancements on print writers.
    *
    * @param self
    *   the underlying print writer
    */
  implicit class SseWriter(val self: PrintWriter) extends AnyVal:

    /** Stream an SSE event to this print writer.
      *
      * @param event
      *   the SSE event
      */
    def stream(event: SseEvent): Unit =
      event.id foreach { id =>
        self.println(s"id: $id")
      }
      event.`type` foreach { ev =>
        self.println(s"event: $ev")
      }
      event.data.toSeq flatMap { _.split("\\r?\\n") } foreach { line =>
        self.println(s"data: $line")
      }
      self.println()
      if event.id.contains(SseEvent.CloseId) then self.close()
      else self.flush()
    end stream
  end SseWriter
end sse
