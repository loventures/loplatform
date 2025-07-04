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

package loi.cp.progress

import java.time.Instant
import java.util as ju
import loi.cp.progress.report.Progress
import loi.cp.reference.EdgePath
import scalaz.syntax.std.map.*

import scala.jdk.CollectionConverters.*

final case class ProgressMap(
  map: ju.Map[EdgePath, Progress], // serialization horror
  lastModified: Option[Instant],
):
  def get(edgePath: EdgePath): Option[Progress] = Option(map.get(edgePath))

  def isComplete(edgePath: EdgePath): Boolean = get(edgePath).exists(_.isComplete)

  def toWebProgress(userId: Long): report.ProgressReport =
    val serializableProgressMap = map.asScala.toMap.mapKeys(ProgressPath.fromEdgePath).asJava
    report.ProgressReport(userId, lastModified, serializableProgressMap)
end ProgressMap

object ProgressMap:
  def empty = ProgressMap(new ju.HashMap, None)
