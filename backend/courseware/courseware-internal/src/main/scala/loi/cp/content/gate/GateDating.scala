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

package loi.cp.content
package gate

import java.time.Instant

import argonaut.CodecJson

/** Captures when a gate date event last executed (either course-wide or per-user). */
case class GateDating(lastScheduled: Option[Instant] = None)

object GateDating:
  final val empty = GateDating()

  import scaloi.json.ArgoExtras.*
  implicit val codec: CodecJson[GateDating] = CodecJson.derive[GateDating]
