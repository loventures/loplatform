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

package loi.cp.analytics

import java.util.UUID
import java.lang as jl

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import loi.cp.analytics.event.Event

@Component
class AnalyticImpl(
  val componentInstance: ComponentInstance,
  self: AnalyticFacade,
) extends AnalyticComponent
    with ComponentImplementation:

  override def getId: jl.Long = self.getId

  override def guid: UUID = self.getGuid

  override def time: String = Analytic.DateFormat.format(self.getTime)

  /** IF the event data is JSON, pretty-prints the tree instead of spitting out the escaped value; otherwise just value
    */
  override def eventData: Event = self.getDataJson
end AnalyticImpl
