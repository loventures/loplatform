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

package loi.cp.job.test

import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.ComponentInstance
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.job.{AbstractEmailJob, EmailJobFacade, GeneratedReport, JobUtils}

/** This is a sample job for integration testing purposes. */
@VisibleForTesting
@Component(name = "Sample Email Job", enabled = false)
class SampleEmailJobImpl(
  val componentInstance: ComponentInstance,
  val self: EmailJobFacade,
  val es: EmailService,
  val fs: FacadeService
) extends AbstractEmailJob[SampleEmailJobComponent]
    with SampleEmailJobComponent:
  override protected def generateReport(): GeneratedReport =
    GeneratedReport(getName, "Sample job", html = false, List(sampleCsv))

  override val logger = org.log4s.getLogger

  private def sampleCsv: UploadInfo =
    JobUtils.csv(s"$getName.csv") { csv =>
      csv writeRow List("Alef", "Bet", "Gimel")
      csv writeRow List(getName, "b", "c")
    }
end SampleEmailJobImpl
