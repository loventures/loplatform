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

package loi.cp.course.lightweight
import de.tomcat.juli.LogMeta
import loi.cp.context.ContextId
import loi.cp.reference.EdgePath

/** Utilities for logging common lightweight course meta information using [LogMeta].
  */
object LightweightLogMeta:
  def context(contextId: ContextId): Unit     = context(contextId.value)
  def context(contextId: Long): Unit          = LogMeta.put("context", contextId)
  def edgePath(edgePathValue: EdgePath): Unit = LogMeta.put("edgePath", edgePathValue.toString)
