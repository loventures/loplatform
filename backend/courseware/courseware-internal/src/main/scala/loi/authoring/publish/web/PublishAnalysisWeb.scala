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

package loi.authoring.publish.web

import loi.cp.offering.PublishAnalysis
import loi.cp.offering.PublishAnalysis.{CreateLineItem, DeleteLineItem, UpdateLineItem}

import java.util.UUID
import scala.collection.mutable

case class PublishAnalysisWeb(
  numStaleSections: Int,
  creates: List[PublishAnalysis.CreateLineItem],
  updates: List[PublishAnalysis.UpdateLineItem],
  deletes: List[PublishAnalysis.DeleteLineItem],
)

object PublishAnalysisWeb:
  def from(analysis: PublishAnalysis): PublishAnalysisWeb =

    val accCreates = mutable.Map.empty[UUID, CreateLineItem]
    val accUpdates = mutable.Map.empty[UUID, UpdateLineItem]
    val accDeletes = mutable.Map.empty[UUID, DeleteLineItem]

    for
      section <- analysis.lisResultChanges
      create  <- section.creates
    do accCreates.put(create.name, create)

    for
      section <- analysis.lisResultChanges
      update  <- section.updates
    do accUpdates.put(update.name, update)

    for
      section <- analysis.lisResultChanges
      delete  <- section.deletes
    do accDeletes.put(delete.name, delete)

    PublishAnalysisWeb(
      analysis.numStaleSections,
      accCreates.values.toList.sortBy(_.title),
      accUpdates.values.toList.sortBy(_.title),
      accDeletes.values.toList.sortBy(_.title),
    )
  end from
end PublishAnalysisWeb
