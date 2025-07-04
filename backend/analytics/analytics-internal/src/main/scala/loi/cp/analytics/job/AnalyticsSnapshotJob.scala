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

package loi.cp.analytics.job

import com.learningobjects.cpxp.component.ComponentInstance
import com.learningobjects.cpxp.component.annotation.{Component, Schema}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.analytics.AnalyticsService
import loi.cp.analytics.event.InstructorSnapshotDayCreatEvent1
import loi.cp.job.{AbstractJob, Job, JobFacade, Run}
import org.log4s.Logger
import scaloi.misc.TimeSource

import java.time.temporal.ChronoUnit
import java.util.{Date, UUID}

@Schema("analyticsSnapshotJob")
trait AnalyticsSnapshotJob extends Job[AnalyticsSnapshotJob]

@Component
class AnalyticsSnapshotJobImpl(
  val componentInstance: ComponentInstance,
  val self: JobFacade,
  val fs: FacadeService,
  analyticsService: AnalyticsService,
  domainDto: => DomainDTO,
  timeSource: TimeSource
) extends AbstractJob[AnalyticsSnapshotJob]
    with AnalyticsSnapshotJob:

  override protected val logger: Logger = org.log4s.getLogger

  override protected def execute(run: Run): Unit =
    analyticsService
      .emitEvent(
        InstructorSnapshotDayCreatEvent1(
          id = UUID.randomUUID(),
          source = domainDto.hostName,
          time = timeSource.date,
          snapshotDay = Date.from(timeSource.instant.minus(1, ChronoUnit.DAYS))
        )
      )

    run.succeeded("Success")
  end execute
end AnalyticsSnapshotJobImpl
