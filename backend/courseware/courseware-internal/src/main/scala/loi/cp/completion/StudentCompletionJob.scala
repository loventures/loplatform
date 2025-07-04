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

package loi.cp.completion

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import java.time.Instant

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.{Component, Schema}
import com.learningobjects.cpxp.component.{ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.PKComponentOps
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.{UserConstants, UserFacade, UserFolderFacade}
import com.learningobjects.cpxp.web.ExportFile
import doobie.*
import doobie.implicits.*
import kantan.csv.rfc
import kantan.csv.ops.*
import loi.cp.course.{CourseEnrollmentService, CourseSectionService}
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.job.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.query.{Comparison, Function}
import com.learningobjects.cpxp.service.script.ComponentFacade
import com.learningobjects.cpxp.util.ManagedUtils

import scala.jdk.CollectionConverters.*

@Schema("studentCompletionJob")
trait StudentCompletionJob extends EmailJob[StudentCompletionJob]:
  @JsonProperty
  def getReturnOnlyCompletedStudents: Boolean
  def setReturnOnlyCompletedStudents(returnOnlyCompletedStudents: Boolean): Unit

  @JsonProperty
  def getStudentEmailAddresses: String
  def setStudentEmailAddresses(studentEmailAddresses: String): Unit

@Component
final class StudentCompletionJobImpl(implicit
  val self: EmailJobFacade,
  val es: EmailService,
  val fs: FacadeService,
  val componentInstance: ComponentInstance,
  xa: Transactor[IO],
  domainDTO: DomainDTO,
  courseSectionService: CourseSectionService,
  componentService: ComponentService,
  courseCompletionService: CourseCompletionService,
  courseEnrollmentService: CourseEnrollmentService,
  enrollmentWebService: EnrollmentWebService
) extends AbstractEmailJob[StudentCompletionJob]
    with StudentCompletionJob:

  val logger = org.log4s.getLogger

  override def update(job: StudentCompletionJob): StudentCompletionJob =
    setReturnOnlyCompletedStudents(job.getReturnOnlyCompletedStudents)
    setStudentEmailAddresses(job.getStudentEmailAddresses)
    super.update(job)

  override def getReturnOnlyCompletedStudents: Boolean                                    =
    self.getAttribute("returnOnlyCompletedStudents", classOf[Boolean])
  override def setReturnOnlyCompletedStudents(returnOnlyCompletedStudents: Boolean): Unit =
    self.setAttribute("returnOnlyCompletedStudents", returnOnlyCompletedStudents)

  override def getStudentEmailAddresses: String                              = self.getAttribute("studentEmailAddresses", classOf[String])
  override def setStudentEmailAddresses(studentEmailAddresses: String): Unit =
    self.setAttribute("studentEmailAddresses", studentEmailAddresses)

  /** Generate the report to be emailed out. */
  override protected def generateReport(): GeneratedReport =

    val emailAddresses = getStudentEmailAddresses.split(",").toList

    val reports =
      if emailAddresses.isEmpty then Seq.empty
      else generateCompletionDataForEnrollments(emailAddresses)

    import CourseCompletionReport.*
    val reportDate = dateFormatter.format(Instant.now())
    val out        = new ExportFile(s"completion_report_$reportDate.csv", MediaType.CSV_UTF_8)

    out.file.writeCsv(reports, rfc.withHeader)

    GeneratedReport(
      s"Student Completions Report for $reportDate",
      "Attached is the completion report",
      html = false,
      Seq(out.toUploadInfo)
    )
  end generateReport

  private def generateCompletionDataForEnrollments(emailAddresses: Seq[String]) =
    val userFolder             = UserConstants.ID_FOLDER_USERS.facade[UserFolderFacade]
    val users                  = userFolder
      .queryUsers()
      .addCondition(
        UserConstants.DATA_TYPE_EMAIL_ADDRESS,
        Comparison.in,
        emailAddresses.map(_.toLowerCase()),
        Function.LOWER
      )
      .getFacades[UserFacade]
    val allEnrollmentsAskedFor =
      users.flatMap(u => enrollmentWebService.getUserEnrollments(u.getId, EnrollmentType.ACTIVE_ONLY).asScala)

    allEnrollmentsAskedFor
      .filter(e =>
        e.getGroupId != null // facade framework views groupId as null if group is deleted
          && e.getGroupId.facade[ComponentFacade].getIdentifier == LightweightCourse.Identifier
      )
      .groupBy(_.getGroupId)
      .flatMap { case (groupId, enrollments) =>
        courseSectionService
          .getCourseSection(groupId)
          .map(section =>
            val sadLightweightCourse = section.id.component[LightweightCourse]
            enrollments.map(e =>
              committed(
                courseCompletionService.generateCompletionReport(
                  sadLightweightCourse,
                  section,
                  e,
                  withRedshift = !getReturnOnlyCompletedStudents
                )
              )
            )
          )
          .getOrElse(Seq.empty)
      }
  end generateCompletionDataForEnrollments

  private def generateCompletedStudentDailyReport() =
    val coursesWithRecentEntries =
      sql"""
           select distinct cast(datajson #>> '{course, section, id}' as bigint) as section_id
           from analyticfinder
           where datajson ->> 'eventType' = 'CourseEntryEvent'
           and time > (current_date - interval '1 day');
         """
        .query[Long]
        .to[List]
        .transact(xa)
        .unsafeRunSync()

    coursesWithRecentEntries
      .flatMap(sectionId =>
        courseSectionService
          .getCourseSection(sectionId)
          .map(section =>
            val sadLightweightCourse = section.id.component[LightweightCourse]
            val enrollments          = enrollmentWebService
              .getGroupEnrollments(section.id, EnrollmentType.ACTIVE_ONLY)
              .asScala
              .filter(_.getRole.getRoleId == "student")
            enrollments.map(u =>
              committed(courseCompletionService.generateCompletionReport(sadLightweightCourse, section, u, false))
            )
          )
          .getOrElse(Seq.empty)
      )
      .filter(_.complete)
  end generateCompletedStudentDailyReport

  /** Commit in the inner loop to avoid holding a very long transaction that keeps locks on interesting rows. */
  def committed[A](a: => A): A =
    ManagedUtils.commit()
    a
end StudentCompletionJobImpl
