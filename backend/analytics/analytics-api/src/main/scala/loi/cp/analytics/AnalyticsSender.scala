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

import loi.cp.analytics.bus.AnalyticBusConfiguration

import java.util.Date

/** Describes how an analytics system can send a sequence of events. */
trait AnalyticsSender:

  /** Send a sequence of analytics to the target of the associated system. */
  def sendAnalytics(
    events: Seq[Analytic],
    busConfig: AnalyticBusConfiguration,
    lastMaterializedViewRefreshDate: Option[Date]
  ): DeliveryResult

sealed trait DeliveryResult:
  def isSuccess: Boolean = this.isInstanceOf[DeliverySuccess]
sealed trait DeliveryFailure extends DeliveryResult

object DeliveryResult:
  def permanentFailure(th: Throwable): DeliveryFailure                      = PermanentFailure(th)
  def transientFailure(th: Throwable): DeliveryFailure                      = TransientFailure(th)
  def success(didRefreshMaterializedViews: Boolean = false): DeliveryResult = DeliverySuccess(
    didRefreshMaterializedViews
  )

case class DeliverySuccess(
  didRefreshMaterializedViews: Boolean = false
) extends DeliveryResult

case class TransientFailure(th: Throwable) extends DeliveryFailure
case class PermanentFailure(th: Throwable) extends DeliveryFailure
