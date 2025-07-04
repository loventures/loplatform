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

package loi.cp.lwgrade
package api

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiFilter, ApiQuery, ApiQueryResults, PredicateOperator, given}
import com.learningobjects.cpxp.component.web.ErrorResponse.*
import com.learningobjects.cpxp.component.web.ErrorResponseOps.*
import com.learningobjects.cpxp.component.web.{ArgoBody, ErrorResponse, TextResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.util.I18nMessage
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.UserId.*
import com.learningobjects.cpxp.service.user.{UserDTO, UserFacade, UserId as Uzr}
import kantan.csv.*
import kantan.csv.ops.*
import loi.cp.content.{CourseContentService, CourseWebUtils}
import loi.cp.context.ContextId as Crs
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.course.{CourseConfigurationService, CourseEnrollmentService, CoursePreferences, CourseSectionService}
import loi.cp.lti.lightweight.LtiGradeSyncService
import loi.cp.lti.lightweight.LtiGradeSyncService.*
import loi.cp.lti.storage.UserGradeSyncHistory
import loi.cp.lti.{CourseColumnIntegrations, LtiColumnIntegrationService, LtiOutcomesService}
import loi.cp.lwgrade.Grade.{ExtractedGrade, GradeExtractor}
import loi.cp.lwgrade.SingleGradeHistory.empty
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStorageService
import loi.cp.user.web.UserWebUtils
import loi.cp.user.{UserComponent, *}
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.list.*
import scalaz.syntax.std.option.*
import scalaz.{NonEmptyList, \/}
import scaloi.misc.Monoids.rightBiasMapMonoid
import scaloi.misc.TryInstances.*
import scaloi.syntax.disjunction.*
import scaloi.syntax.map.*
import scaloi.syntax.option.*
import scaloi.syntax.zero.*
import scaloi.syntax.ʈry.*

import java.io.StringWriter
import java.time.Instant
import scala.jdk.CollectionConverters.*
import scala.util.Try

@Component
final class LegacyGradebookWebControllerImpl(
  val componentInstance: ComponentInstance,
  courseWebUtils: CourseWebUtils,
  gradeService: GradeService,
  enrollmentService: CourseEnrollmentService,
  courseContentService: CourseContentService,
  courseSectionService: CourseSectionService,
  courseStorageService: CourseStorageService,
  ltiColumnIntegrationService: LtiColumnIntegrationService,
  ltiOutcomesService: LtiOutcomesService,
  userWebUtils: UserWebUtils,
  impersonationService: ImpersonationService,
  ltiGradeSyncService: LtiGradeSyncService,
  courseConfigurationService: CourseConfigurationService
)(implicit
  cs: ComponentService,
  facadeService: FacadeService,
) extends LegacyGradebookWebController
    with ComponentImplementation:

  import LegacyGradebookWebController.*
  import LegacyGradebookWebControllerImpl.*

  override def getGrades(
    courseId: Long,
    q: ApiQuery,
    syncHistory: Option[Boolean],
  ): ArgoBody[ApiQueryResults[GradeComponentDto]] =
    val course = courseWebUtils
      .loadCourseSection(courseId)
      .valueOr(err => throw new ResourceNotFoundException(err))

    val userFilter   = usersInApiQuery(q)
    val columnFilter = columnsInApiQuery(q)
    val structure    = GradeStructure(course.contents)
    userFilter
      .getOrElse(enrolledStudentIds(course))
      .toNel
      .cata(
        studs =>

          val histories =
            syncHistory.isTrue ?? courseStorageService.get[UserGradeSyncHistory](course.lwc, studs.list.toList)

          val grades =
            gradeService
              .getCourseGradebooks(course.lwc, course.contents, studs)
              .flatMap({ case (uzr, rawGb) =>
                val gradebook = StudentGradebook.applyRollupGradeViewConfig(courseConfigurationService, course, rawGb)
                toGradeComponents(uzr, gradebook, histories.get(uzr.id), columnFilter)
              })
              .toList

          ArgoBody(new ApiQueryResults[GradeComponentDto](grades.asJava, grades.size.toLong, null))
        ,
        ArgoBody(ApiQueryResults.emptyResults().asInstanceOf[ApiQueryResults[GradeComponentDto]])
      )
  end getGrades

  override def getGradeHistory(
    course: Long,
    edgePath: String,
    userId: Long
  ): ErrorResponse \/ ArgoBody[SingleUserGradeHistoryDto] =
    for
      user     <- userId.component_?[UserComponent] \/> notFound(s"User with id: $userId not found")
      lwc      <- course.component_?[LightweightCourse] \/> notFound(s"Course with id: $course not found")
      contents <- courseContentService.getCourseContents(lwc).toOption \/> serverError(
                    s"Error getting contents for course $course"
                  )
    yield
      val path    = EdgePath.parse(edgePath)
      val column  = GradeStructure(contents).findColumnForEdgePath(path)
      val history =
        Try(courseStorageService.get[UserGradeSyncHistory](lwc, user)).toOption
      ArgoBody {
        SingleUserGradeHistoryDto(
          user = user.toDTO,
          column = column,
          history = toSingleGradeHistory(path)(history)
        )
      }

  override def syncExternalGrade(
    course: Long,
    edgePath: String,
    userId: Long
  ): ErrorResponse \/ ArgoBody[SingleUserGradeHistoryDto] =
    for
      user     <- userId.component_?[UserComponent] \/> notFound(s"User with id: $userId not found")
      lwc      <- course.component_?[LightweightCourse] \/> notFound(s"Course with id: $course not found")
      contents <- courseContentService.getCourseContents(lwc) \/>| serverError(
                    s"Error getting contents for course $course"
                  )
      path      = EdgePath.parse(edgePath)
      grades    = gradeService.getGrades(user, lwc, contents)
      grade    <- grades.get(path) \/> notFound(s"Column with path: $path not found")
    yield
      val history = ltiGradeSyncService.syncGrade(lwc, user, path, grade)

      val column = GradeStructure(contents).findColumnForEdgePath(path)

      ArgoBody {
        SingleUserGradeHistoryDto(
          user = user.toDTO,
          column = column,
          history = history
        )
      }

  override def getLastManualSync(courseId: Long): ErrorResponse \/ ArgoBody[LastManualSyncDto] =
    for
      lwc <- courseWebUtils.loadCourseSection(courseId, None).leftMap(_.to404)
      cci <- ltiColumnIntegrationService.get(lwc) \/> ErrorResponse.notFound
    yield ArgoBody {
      LastManualSyncDto(cci.lastManualSync)
    }

  override def syncEntireCourse(courseId: Long): ErrorResponse \/ ArgoBody[LastManualSyncDto] =
    for
      lwc <- courseSectionService.getCourseSection(courseId) \/> notFound(s"Course with id: $courseId not found")
      _   <- ltiGradeSyncService.syncAllColumnsAndGradesForCourse(lwc) leftMap syncErrorToHttpError
    yield
      val now = Some(Instant.now())
      ltiColumnIntegrationService.modify(lwc)(courseColumnIntegrations =>
        courseColumnIntegrations.map(_.copy(lastManualSync = now))
      )
      ArgoBody {
        LastManualSyncDto(now)
      }

  override def getColumnHistory(course: Long, edgePath: String): ErrorResponse \/ ArgoBody[SingleColumnSyncHistoryDto] =
    for
      lwc                 <- courseSectionService.getCourseSection(course) \/> notFound(s"Course with id: $course not found")
      path                 = EdgePath.parse(edgePath)
      (column, histories) <- ltiGradeSyncService.getColumnHistory(lwc, path) leftMap syncErrorToHttpError
    yield ArgoBody {
      SingleColumnSyncHistoryDto(
        column = column,
        history = ColumnSyncHistoryDto.from(histories, path)
      )
    }

  override def syncExternalColumn(
    course: Long,
    edgePath: String
  ): ErrorResponse \/ ArgoBody[SingleColumnSyncHistoryDto] =
    for
      lwc                 <- courseSectionService.getCourseSection(course) \/> notFound(s"Course with id: $course not found")
      path                 = EdgePath.parse(edgePath)
      (column, histories) <- ltiGradeSyncService.syncColumn(lwc, path) leftMap syncErrorToHttpError
    yield ArgoBody {
      SingleColumnSyncHistoryDto(
        column = column,
        history = ColumnSyncHistoryDto.from(histories, path)
      )
    }

  def syncErrorToHttpError(s: SyncError): ErrorResponse = s match
    case Exceptional(message)     => serverError(message)
    case NoCourse(courseId: Long) => notFound(s"Course with id $courseId was not found")
    case NoColumn(ep)             => notFound(s"Column for edgepath: $ep not found")
    case NoSyncHistory(courseId)  => notFound(s"Course with id $courseId has no sync history")
    case ColumnAlreadySynced(ep)  => unacceptable(s"Column with edgepath: $ep is already synced")

  override def syncAllGradesForColumn(
    course: Long,
    edgePath: String
  ): ErrorResponse \/ ArgoBody[Map[Uzr, SingleGradeHistory]] =
    for
      lwc      <- courseSectionService.getCourseSection(course) \/> notFound(s"Course with id: $course wasn't found")
      path      = EdgePath.parse(edgePath)
      histories = ltiGradeSyncService.syncAllGradesForColumn(lwc, path)
    yield ArgoBody(histories)

  override def syncAllColumnsForCourse(course: Long): ErrorResponse \/ Unit =
    for lwc <- courseSectionService.getCourseSection(course) \/> notFound(s"Course with id: $course wasn't found")
    yield ltiOutcomesService.manuallySyncColumns(lwc)

  private def enrolledStudentIds(course: Crs): List[Uzr] =
    enrollmentService.getEnrolledStudentIds(course.id).map(Uzr.apply)

  override def getSettings(course: Long): ArgoBody[GradebookSettingsDto] =
    ArgoBody(GradebookSettingsDto.empty)

  override def getCategories(
    courseId: Long,
  ): ArgoBody[ApiQueryResults[GradebookCategoryComponentDto]] =
    val course     = courseWebUtils
      .loadCourseSection(courseId)
      .valueOr(err => throw new ResourceNotFoundException(err))
    val structure  = GradeStructure(course.contents)
    val categories = toCategories(structure)
    ArgoBody(new ApiQueryResults(categories.asJava, categories.length.toLong, categories.length.toLong))

  override def getColumns(
    course: Long,
  ): Try[ArgoBody[ApiQueryResults[GradebookColumnComponentDto]]] =
    for
      section         <- courseWebUtils.loadCourseSection(course).toTry(err => new ResourceNotFoundException(err))
      columnHistories <- Try(ltiColumnIntegrationService.get(section))
    yield
      val structure = GradeStructure(section.contents)
      val columns   = toColumns(section.courseDueDates, structure, columnHistories)
      ArgoBody(new ApiQueryResults(columns.asJava, columns.length.toLong, columns.length.toLong))

  override def getColumn(
    courseId: Long,
    edgePath: EdgePath
  ): ErrorResponse \/ GradebookColumnComponentDto =

    val section =
      courseWebUtils
        .loadCourseSection(courseId)
        .valueOr(err => throw new ResourceNotFoundException(err))

    val structure       = GradeStructure(section.contents)
    val columnHistories = ltiColumnIntegrationService.get(section)
    val columns         = toColumns(section.courseDueDates, structure, columnHistories)
    columns.find(_.id == edgePath) \/> notFound(s"Column path $edgePath did not exist in course $courseId")
  end getColumn

  override def doExport(courseId: Long, argoConfig: ArgoBody[GradebookExportConfig]): TextResponse =
    val course                = Crs(courseId)
    val lwc                   = courseId.component_![LightweightCourse].get
    val contents              = courseContentService.getCourseContents(lwc).get
    val config                = argoConfig.decode_!.get
    val allEnrolledStudentIds = config.userIds
      .map(`given` =>
        val allEnrolledStudents = enrolledStudentIds(course)
        allEnrolledStudents.filter(e => `given`.contains(e.id))
      )
      .getOrElse(enrolledStudentIds(course))

    val grades = allEnrolledStudentIds.toList.toNel
      .map(gradeService.getCourseGradebooks(course, contents, _))
      .map(
        _.mapValuesEagerly(StudentGradebook.applyRollupGradeViewConfig(courseConfigurationService, contents, lwc, _))
      )
      .getOrElse(Map.empty)

    val gradesWithUsers   = grades.traverseKeys(_.id.facade_![UserFacade].map(UserDTO(_))).get
    val structure         = GradeStructure(contents)
    val writer            = new StringWriter()
    val useProjectedGrade = courseConfigurationService.getGroupConfig(CoursePreferences, lwc).useProjectedGrade
    val extractor         = if useProjectedGrade then Grade.ProjectedGradeExtractor else Grade.RawGradeExtractor
    val csvWriter         =
      writeGrades(writer)(extractor, gradesWithUsers, structure, config, _.i18n(using componentInstance.getComponent))
    val fileName          = s"${lwc.getName}-${Instant.now().toString}.csv"
    TextResponse.csvDownload(fileName = fileName, document = writer.toString)
  end doExport
  override def exportStudentGradebook(
    courseId: Long,
    studentId: Long
  ): ErrorResponse \/ TextResponse =

    for
      section <- courseWebUtils.loadCourseSection(courseId).leftMap(_.to422)
      student <- userWebUtils.loadUser(studentId).leftMap(_.to422)
      _       <- impersonationService.checkImpersonation(section, student).leftMap(_.to422)
    yield
      val studentGradebook  = gradeService
        .getCourseGradebooks(section, section.contents, NonEmptyList.apply(Uzr(student.id)))
        .map { case (userId, gradebook) =>
          student -> StudentGradebook.applyRollupGradeViewConfig(courseConfigurationService, section, gradebook)
        }
      val structure         = GradeStructure(section.contents)
      val writer            = new StringWriter()
      val useProjectedGrade = courseConfigurationService.getGroupConfig(CoursePreferences, section).useProjectedGrade
      val extractor         = if useProjectedGrade then Grade.ProjectedGradeExtractor else Grade.RawGradeExtractor
      writeGrades(writer)(
        extractor,
        studentGradebook,
        structure,
        GradebookExportConfig.empty,
        _.i18n(using componentInstance.getComponent)
      )
      val fileName          = s"${section.groupId}-${Instant.now().toString}.csv"
      TextResponse.csvDownload(fileName = fileName, document = writer.toString)
end LegacyGradebookWebControllerImpl

object LegacyGradebookWebControllerImpl:
  import LegacyGradebookWebController.*

  val defaultUserAttributes = List(
    UserAttribute.id,
    UserAttribute.userName,
    UserAttribute.givenName,
    UserAttribute.familyName,
    UserAttribute.emailAddress,
    UserAttribute.externalId
  )

  /** Apply a map of learner to gradebook to a CsvWriter. The resulting csv data will contain a row per user with a
    * column for each grade sorted by the assignment title.
    *
    * TODO: Consider this out to it's own namespace if this gets much larger.
    */
  def writeGrades[A: CsvSink](sink: A)(
    extractor: GradeExtractor,
    gradebooks: Map[UserDTO, StudentGradebook],
    gradeStructure: GradeStructure,
    config: GradebookExportConfig,
    xlate: I18nMessage => String // parameterize translation for testing.
  ): CsvWriter[List[String]] =
    val userAttributes = config.userAttributes.getOrElse(defaultUserAttributes)
    val csvWriter      = sink.asCsvWriter[List[String]](rfc)

    val filteredStructure = filterEmptyCategories(gradeStructure)

    val categoryNames: Seq[String] = for
      cat     <- filteredStructure.categories
      headers <- s"${cat.title} - ${percentage(gradeStructure.categoryWeight(cat))}" +: Seq.fill(cat.columns.length)(
                   ""
                 ) // pad with blank columns to align with assignments
    yield headers

    val averageColumn                = xlate(averageMessage)
    val assignmentNames: Seq[String] = filteredStructure.categories.flatMap({ cat =>
      val titles = cat.columns.map({ case GradeColumn(_, title, isForCredit, pointsPossible) =>
        f"$title%s - $pointsPossible%.2f" + (if !isForCredit then " (NC)" else "")
      })
      titles :+ averageColumn
    })

    val moduleHeader =
      List.fill(userAttributes.length)("") ++ categoryNames // 4 blank columns to align with first assignment
    val assignmentHeader = userAttributes.map(att => xlate(att.msg)) ++ assignmentNames ++ List(xlate(totalMessage))

    csvWriter.write(moduleHeader)
    csvWriter.write(assignmentHeader)
    for
      (user, gb) <- gradebooks
      grades      = renderRow(gb, filteredStructure)
      overall     = gb.overall.map(extractor).map(_.percentage).getOrElse(0d)
    do csvWriter write userAttributes.map(_.extract(user).zNull) ++ grades ++ List(percentage(overall))
    csvWriter
  end writeGrades

  /** Render a row of grades, including the average for every category.
    */
  def renderRow(gb: StudentGradebook, structure: GradeStructure): Seq[String] =
    def renderCategory(category: GradeCategory): Seq[ComponentIdentifier] =
      val columns = category.columns map { col =>
        gb.grades.get(col.path).flatMap(Grade.grade).map(round).cata(_.toString, "")
      }
      val average = gb.grades.get(category.path).map(g => ExtractedGrade(g).percentage).getOrElse(0d)
      columns :+ percentage(average)
    structure.categories.flatMap(renderCategory)

  def round(d: Double): Double =
    BigDecimal(d).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

  def toGradeComponents(
    user: Uzr,
    gb: StudentGradebook,
    histories: Option[UserGradeSyncHistory],
    columns: Option[Set[EdgePath]]
  ): List[GradeComponentDto] =
    gb.grades
      .collect({
        case (path, g) if columns.forall(_.contains(path)) =>
          val gradeScalar = Grade.grade(g)
          val gradeRatio  = Grade.fraction(g)
          val maxScalar   = Grade.max(g)
          val date        = Grade.date(g) | Instant.EPOCH

          GradeComponentDto.empty.copy(
            grade = gradeScalar,
            max = maxScalar,
            column_id = path,
            id = path,
            info = GradeInfoDto.empty.copy(
              grade = gradeScalar,
              score = ScoreDto(gradeScalar, maxScalar),
              rawScore = ScoreDto(gradeScalar, maxScalar),
              submissionDate = date,
              unscaledScore = ScoreDto(gradeScalar, maxScalar),
              status = g.getClass.getSimpleName,
            ),
            user_id = user,
            raw_grade = gradeRatio,
            gradeSyncHistory = toSingleGradeHistory(path)(histories)
          )
      })
      .toList

  def toSingleGradeHistory(ep: EdgePath)(history: Option[UserGradeSyncHistory]): SingleGradeHistory =
    history.cata(
      h =>
        SingleGradeHistory(
          h.outcomes1Callbacks.get(ep).flatMap(_.statusHistory),
          h.agsScores.get(ep),
        ),
      empty
    )

  def percentage(d: Double) = if d.isNaN then "" else f"${d * 100.0}%.1f%%"

  def categoryToColumnComponent(
    dueDates: Map[EdgePath, Instant],
    category: GradeCategory
  ): GradebookColumnComponentDto =
    GradebookColumnComponentDto.empty.copy(
      id = category.path,
      name = category.title,
      `category_id` = category.path,
      dueDate = dueDates.get(category.path),
      Category = GradebookCategoryComponentDto.empty.copy(id = category.path, name = category.title),
      `type` = "Category",
      weight = category.weight.cata(_.doubleValue, 0.0)
    )

  def columnToColumnComponent(
    dueDates: Map[EdgePath, Instant],
    column: GradeColumn,
    category: GradeCategory,
    histories: Option[CourseColumnIntegrations]
  ): GradebookColumnComponentDto =
    GradebookColumnComponentDto.empty.copy(
      id = column.path,
      name = column.title,
      `category_id` = category.path,
      Category = GradebookCategoryComponentDto.empty.copy(id = category.path, name = category.title),
      dueDate = dueDates.get(column.path),
      `type` = "Assignment",
      credit = if column.isForCredit then "Credit" else "NoCredit",
      maximumPoints = column.pointsPossible.doubleValue,
      syncHistory = histories.map(ColumnSyncHistoryDto.from(_, column.path))
    )

  def usersInApiQuery(apiQuery: ApiQuery): Option[List[Uzr]] =
    inApiQuery(apiQuery, "user_id").map(_.map(s => Uzr(s.toLong)))

  def columnsInApiQuery(apiQuery: ApiQuery): Option[Set[EdgePath]] =
    inApiQuery(apiQuery, "column_id").map(_.map(EdgePath.parse).toSet)

  def inApiQuery(apiQuery: ApiQuery, property: String): Option[List[String]] =
    apiQuery.getFilters.asScala
      .find(inOrEquals(property))
      .map(_.getValue)
      .map(idList => if idList.isEmpty then List.empty else idList.split(',').toList)

  private def inOrEquals(prop: String) = (f: ApiFilter) =>
    f.getProperty == prop && (f.getOperator == PredicateOperator.IN || f.getOperator == PredicateOperator.EQUALS)

  def filterNoCreditColumns(gradeStructure: GradeStructure): GradeStructure =
    gradeStructure.copy(
      categories = gradeStructure.categories.map(cat => cat.copy(columns = cat.columns.filter(_.isForCredit)))
    )

  def filterEmptyCategories(gradeStructure: GradeStructure): GradeStructure =
    gradeStructure.copy(categories = gradeStructure.categories.filterNot(_.columns.isEmpty))

  def toCategories(gradeStructure: GradeStructure): Seq[GradebookCategoryComponentDto] =
    gradeStructure.categories
      .filterNot(cat => cat.columns.isEmpty)
      .map(cat =>
        GradebookCategoryComponentDto.empty
          .copy(id = cat.path, name = cat.title, weight = cat.weight.cata(_.doubleValue, 0.0))
      )

  def toColumns(
    dueDates: Map[EdgePath, Instant],
    gradeStructure: GradeStructure,
    histories: Option[CourseColumnIntegrations]
  ): Seq[GradebookColumnComponentDto] =
    val assignments = for
      category <- gradeStructure.categories
      column   <- category.columns
    yield columnToColumnComponent(dueDates, column, category, histories)
    val categories  =
      for category <- gradeStructure.categories
      yield categoryToColumnComponent(dueDates, category)
    assignments ++ categories
  end toColumns

  val averageMessage = I18nMessage("average")
  val totalMessage   = I18nMessage("total")
end LegacyGradebookWebControllerImpl
