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

package loi.cp.mastery.notification

import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import loi.authoring.asset.Asset
import loi.authoring.node.AssetNodeService
import loi.cp.notification.{NotificationFacade, NotificationImplementation}

import java.util.Date

/** The competency mastery notification implementation. */
@Component
class CompetencyMasteryNotificationImpl(val componentInstance: ComponentInstance, val self: NotificationFacade)(
  assetService: AssetNodeService,
) extends CompetencyMasteryNotification
    with NotificationImplementation
    with ComponentImplementation:
  import CompetencyMasteryNotificationImpl.*

  @PostCreate
  def init(ev: Init): Unit =
    self.setTime(Date `from` ev.when)
    self.setContext(Some(ev.course.id))
    self.setData(
      CompetencyMasteryNotificationData(ev.user.id, ev.competency.id, ev.course.commitId, ev.course.branch.id)
    )

  override def audience: List[Long] = data.studentId :: Nil

  override val aggregationKey: Option[String] = Some(AggregationKey)

  // I could provide branch and commit and not guess,
  override def getCompetency: Asset[?] =
    assetService.loadRawByGuessing(data.competencyId).get

  private def data = self.getData[CompetencyMasteryNotificationData]
end CompetencyMasteryNotificationImpl

object CompetencyMasteryNotificationImpl:
  /* All such alerts are aggregated together, i.e. "you have mastered $n new competencies" */
  final val AggregationKey = "competencyMastery"

// commitId and branchId exist for legacy data and are unused
private final case class CompetencyMasteryNotificationData(
  studentId: Long,
  competencyId: Long,
  commitId: Long,
  branchId: Long,
)
