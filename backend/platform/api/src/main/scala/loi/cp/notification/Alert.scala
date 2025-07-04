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

package loi.cp.notification

import java.util.Date

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.de.web.{DeletableEntity, Queryable, QueryableId}

@Schema("alert")
trait Alert extends ComponentInterface with QueryableId with DeletableEntity:
  import Alert.*

  /** The number of notifications aggregated in this alert. */
  @Queryable
  @JsonProperty(CountProperty)
  def getCount: Long

  /** The time of the last notification aggregated in this alert. */
  @Queryable
  @JsonProperty(TimeProperty)
  def getTime: Date

  /** The aggregation key for the notifications in this alert. */
  @Queryable
  @JsonProperty(AggregationKeyProperty)
  def getAggregationKey: String

  /** The notification. */
  @JsonProperty
  def getNotification: Notification

  /** The context in which this alert occurred. */
  @Queryable
  @JsonProperty(ContextIdProperty)
  def getContextId: Option[Long]

  @Queryable
  @JsonProperty(ViewedProperty)
  def isViewed: Boolean

  @RequestMapping(path = "view", method = Method.POST)
  def view(): Unit
end Alert

/** The alert component companion.
  */
object Alert:

  /** The count property. */
  final val CountProperty = "count"

  /** The time property. */
  final val TimeProperty = "time"

  /** The aggregation key property. */
  final val AggregationKeyProperty = "aggregationKey"

  /** The context id property. */
  final val ContextIdProperty = "context_id"

  /** The viewed on property. */
  final val ViewedProperty = "viewed"
end Alert
