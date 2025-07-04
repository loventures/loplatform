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
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.enrollment.EnrollmentFacade
import doobie.*
import doobie.implicits.*
import kantan.csv.HeaderEncoder
import loi.cp.content.CourseContent
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.course.{CourseComponent, CourseSection}
import loi.cp.lwgrade.Grade.Graded
import loi.cp.lwgrade.{GradeService, StudentGradebook}
import loi.cp.reference.EdgePath
import loi.cp.user.UserComponent
import loi.db.Redshift
import loi.doobie.log.*
import scaloi.json.ArgoExtras

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant, ZoneId}
import java.util.Optional
import java.util.concurrent.TimeUnit
import scala.compat.java8.OptionConverters.*

@Service
class CourseCompletionService(implicit
  gradeService: GradeService,
  componentService: ComponentService,
  xa: => Transactor[IO]
):
  import CourseCompletionService.*

  def generateCompletionReport(
    sadLightweightCourse: LightweightCourse,
    section: CourseSection,
    enrollment: EnrollmentFacade,
    withRedshift: Boolean
  ): CourseCompletionReport =

    // Load the relevant data
    val user          = enrollment.getUser.getId.component[UserComponent]
    val grades        = gradeService.getGrades(user.userId, section, section.contents)
    val lastLoginTime =
      if withRedshift then getLastLoginTimeRS(section.id, user.getId) else getLastLoginTimePG(section.id, user.getId)
    val gradedContent = section.contents.tree.flatCollect {
      case content if content.gradingPolicy.exists(_.isForCredit) => content
    }

    val sectionInfo  = SectionInfo(sadLightweightCourse, section.asset.data.title)
    val activityInfo = ActivityInfo(lastLoginTime)
    val gradeInfo    = GradeInfo(grades, gradedContent)

    CourseCompletionReport(sectionInfo, user, gradeInfo, activityInfo)
  end generateCompletionReport

  private def getLastLoginTimePG(sectionId: Long, userId: Long) =
    sql"""
        select time
        from analyticfinder
        where datajson ->> 'eventType' = 'CourseEntryEvent'
        and datajson #>> '{user, id}' = ${userId.toString}
        and datajson #>> '{course, section, id}' = ${sectionId.toString}
        and time > (current_date - interval '7 day')
        order by time desc
        limit 1
       """
      .query[Option[Timestamp]]
      .option
      .map(_.flatten)
      .transact(xa)
      .unsafeRunSync()

  private def getLastLoginTimeRS(sectionId: Long, userId: Long) =
    sql"""
        SELECT
           max(sectionentry.time)
        FROM
          sectionentry
        WHERE
          sectionid = $sectionId AND
          userid = $userId
    """
      .query[Option[Timestamp]]
      .option
      .map(_.flatten)
      .transact(redshift)
      .unsafeRunSync()

  private def redshift = Redshift.buildTransactor("loan")
end CourseCompletionService

object CourseCompletionService:
  private implicit val log: org.log4s.Logger = org.log4s.getLogger

case class GradeInfo(
  rawGrades: StudentGradebook,
  gradedContent: List[CourseContent]
):
  import CourseCompletionReport.*

  lazy val gradesByEdge: Map[EdgePath, Graded] = rawGrades.grades.collect { case (e, g: Graded) =>
    (e, g)
  }

  lazy val contentByEdge: Map[EdgePath, CourseContent] = gradedContent.map(c => c.edgePath -> c).toMap

  lazy val grades: Iterable[Graded] = gradesByEdge.values

  lazy val isComplete: Boolean = gradedContent.forall(c =>
    gradesByEdge
      .collect({ case (e: EdgePath, g: Graded) =>
        (e, g)
      })
      .exists(_._1 == c.edgePath)
  )

  lazy val mostRecentGrade: Option[(EdgePath, Graded)] =
    if grades.isEmpty then None
    else Some(gradesByEdge.toList.maxBy(_._2.date))

  lazy val percentageOfGradedItemsCompleted: Double =
    if gradedContent.isEmpty then 0d
    else grades.size.doubleValue() / gradedContent.size.doubleValue()

  lazy val (pointsEarned, pointsPossible) = gradesByEdge.values
    .foldLeft((0d, 0d))({ case ((pointsEarnedTemp, pointsPossibleTemp), g) =>
      (pointsEarnedTemp + g.grade, pointsPossibleTemp + g.max)
    })

  lazy val pointsInCourse: Long = gradedContent.flatMap(_.gradingPolicy).map(_.pointsPossible.longValue()).sum

  lazy val projectedGrade: Double = pointsEarned / pointsPossible

  lazy val lastCompletedItem: Option[(CourseContent, Graded)] = mostRecentGrade.map(g =>
    val mostRecentContent = contentByEdge(g._1)
    (mostRecentContent, g._2)
  )

  lazy val completionDate: Option[String] =
    if isComplete then mostRecentGrade.map(g => dateFormatter.format(g._2.date))
    else None
end GradeInfo

case class ActivityInfo(lastLogin: Option[Timestamp]):
  import CourseCompletionReport.*

  lazy val daysSinceLastLogin: String = lastLogin
    .map(login =>
      val milliDifference = Math.abs(Timestamp.from(Instant.now()).getTime - login.getTime)
      val days            = TimeUnit.DAYS.convert(milliDifference, TimeUnit.MILLISECONDS)
      if days == 0 then "Logged in on the report generation date"
      else days.toString
    )
    .getOrElse("More than seven days")

  lazy val lastLoginFormatted: String = lastLogin
    .map(login => dateFormatter.format(login.toInstant))
    .getOrElse("Last login was over seven days ago")
end ActivityInfo

case class SectionInfo(sadLightweightCourse: CourseComponent, courseName: String):
  import CourseCompletionReport.*

  lazy val courseStartDate: Option[Instant]          = sadLightweightCourse.getStartDate
  lazy val courseStartDateFormatted: Option[String]  = courseStartDate.map(dateFormatter.format)
  lazy val courseEndDate: Option[Instant]            = sadLightweightCourse.getEndDate
  lazy val courseEndDateFormatted: Option[String]    = courseEndDate.map(dateFormatter.format)
  lazy val duration: Option[Double]                  =
    (courseStartDate zip courseEndDate).map { case (start, end) =>
      val durationOfEnrollment  = Duration.between(start, end)
      val timeSpentInEnrollment = Duration.between(start, Instant.now())

      if durationOfEnrollment.isZero then 0d
      else timeSpentInEnrollment.toMillis.toDouble / durationOfEnrollment.toMillis.toDouble
    }
  lazy val externalId: Optional[ComponentIdentifier] = sadLightweightCourse.getExternalId
end SectionInfo

// NOTE: this is missing time in course calculation
case class CourseCompletionReport(
  courseName: String,
  courseStartDate: Option[String],
  courseEndDate: Option[String],
  courseExternalId: Option[String],
  externalId: Option[String],
  firstName: String,
  lastName: String,
  email: String,
  durationOfEnrollment: Option[Double],
  percentProgressAgainstAssignments: Double,
  totalNumberOfAssignments: Long,
  numberOfAssignmentsCompleted: Long,
  overallGrade: Double,
  totalProgress: String,
  lastLoginDate: String,
  daysSinceLastLogin: String,
  lastCompletedAssignment: Option[String],
  completionDateOfLastCompletedAssignment: Option[String],
  gradeOnLastCompletedAssignment: Option[Double],
  onTrack: Boolean,
  complete: Boolean,
  completionTime: Option[String]
)

object CourseCompletionReport:

  def apply(
    sectionInfo: SectionInfo,
    user: UserComponent,
    gradeInfo: GradeInfo,
    activityInfo: ActivityInfo
  ): CourseCompletionReport =
    CourseCompletionReport(
      sectionInfo.courseName,
      sectionInfo.courseStartDateFormatted,
      sectionInfo.courseEndDateFormatted,
      sectionInfo.externalId.asScala,
      user.getExternalId.asScala,
      user.getGivenName,
      user.getFamilyName,
      user.getEmailAddress,
      sectionInfo.duration,
      gradeInfo.percentageOfGradedItemsCompleted,
      gradeInfo.gradedContent.size,
      gradeInfo.grades.size,
      gradeInfo.projectedGrade,
      s"${gradeInfo.pointsEarned}/${gradeInfo.pointsInCourse} pts",
      activityInfo.lastLoginFormatted,
      activityInfo.daysSinceLastLogin,
      gradeInfo.lastCompletedItem.map(_._1.title),
      gradeInfo.lastCompletedItem.map(item => dateFormatter.format(item._2.date)),
      gradeInfo.lastCompletedItem.map(_._2.grade),
      sectionInfo.duration.exists(d => gradeInfo.projectedGrade <= d),
      gradeInfo.isComplete,
      gradeInfo.completionDate
    )

  implicit val ccrHeaderEncoder: HeaderEncoder[CourseCompletionReport] =
    HeaderEncoder.caseEncoder(
      "Course Name",
      "Course Start Date",
      "Course End Date",
      "Course External ID",
      "User External ID",
      "First Name",
      "Last Name",
      "Email",
      "Duration of Enrollment",
      "Required Content Progress",
      "Total Number of Modules",
      "Total Number of Credit Complete Modules",
      "Overall Grade",
      "Total Progress",
      "Last Login",
      "Days Since Last Login",
      "Name of last for-credit assignment submitted by student",
      "Date/Time of last for-credit assignment submission",
      "Grade on this submission",
      "On Track",
      "Complete",
      "Completion Time"
    )(ArgoExtras.unapply)

  val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MM-dd-yyyy hh:mm:ss a")
    .withZone(ZoneId.systemDefault())
end CourseCompletionReport
