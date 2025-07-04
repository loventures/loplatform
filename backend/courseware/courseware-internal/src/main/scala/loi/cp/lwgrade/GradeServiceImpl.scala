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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.analytics.entity.Score as AnalyticsScore
import loi.cp.appevent.AppEventService
import loi.cp.content.gate.GatingEventListener
import loi.cp.content.{CourseContent, CourseContents}
import loi.cp.context.ContextId as Crs
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.course.{CourseConfigurationService, CoursePreferences, CourseSection, CourseSectionService}
import loi.cp.lti.GradeTarget
import loi.cp.lti.lightweight.LtiGradeSyncService
import loi.cp.lti.storage.UserGradeSyncHistory
import loi.cp.lwgrade.Grade.{Graded, NoCredit, Pending}
import loi.cp.lwgrade.GradeServiceImpl.antiLoop
import loi.cp.lwgrade.error.ColumnMissing
import loi.cp.lwgrade.update.GradeUpdate
import loi.cp.offering.PublishAnalysis
import loi.cp.presence.PresenceService
import loi.cp.progress.{LightweightProgressService, ProgressChange}
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStorageService
import loi.cp.user.LightweightUserService
import scalaz.syntax.std.option.*
import scalaz.{NonEmptyList, \/}
import scaloi.syntax.disjunction.*
import scaloi.syntax.map.*

import java.time.Instant
import scala.util.DynamicVariable

@Service
final class GradeServiceImpl(
  appEventService: AppEventService,
  courseConfigurationService: CourseConfigurationService,
  coursewareAnalyticsService: CoursewareAnalyticsService,
  dao: GradeDao,
  gating: GatingEventListener,
  ltiGradeSyncService: LtiGradeSyncService,
  presence: PresenceService,
  courseSectionService: CourseSectionService,
  courseStorageService: CourseStorageService,
  lightweightProgressService: LightweightProgressService,
  lightweightUserService: LightweightUserService,
  userDto: => UserDTO,
) extends GradeService:

  import GradeServiceImpl.log

  override def getCourseGradebooks(
    course: Crs,
    contents: CourseContents,
    users: NonEmptyList[UserId]
  ): Map[UserId, StudentGradebook] =
    val structure = GradeStructure(contents)
    dao.load(users, course).mapValuesEagerly(StudentGradebook(_)(structure))

  override def getGrades(
    user: UserId,
    course: Crs,
    contents: CourseContents
  ): StudentGradebook = getGrades(user, course, GradeStructure(contents))

  private def getGrades(
    user: UserId,
    course: Crs,
    structure: GradeStructure
  ): StudentGradebook =
    val gradesByPath = dao.loadOrCreate(user, course)
    StudentGradebook(gradesByPath)(structure)

  override def deleteGradebook(
    section: CourseSection,
    user: UserId,
  ): Unit =
    dao.delete(user, section)

  override def transferGrades(
    user: UserId,
    srcCourse: Crs,
    tgtCourse: Crs
  ): Unit =
    dao.transferGrades(user, srcCourse, tgtCourse)

  override def setGradePercent(
    user: UserDTO,
    section: CourseSection,
    content: CourseContent,
    percent: Double,
    when: Instant,
  ): ColumnMissing \/ Grade =
    val structure = GradeStructure(section.contents)
    structure
      .findColumnForEdgePath(content.edgePath)
      .toRightDisjunction(ColumnMissing(Crs(section.id), content.edgePath))
      .map(column =>
        setGradePercent(
          user,
          section,
          content,
          structure,
          column,
          percent,
          when,
        )
      )
  end setGradePercent

  override def setGradePercent(
    user: UserDTO,
    section: CourseSection,
    content: CourseContent,
    structure: GradeStructure,
    column: GradeColumn,
    percent: Double,
    when: Instant,
  ): Grade =
    log.info(s"Setting grade percent to $percent") // most callers have LogMeta.put-ed
    val pointsScored   = percent * column.pointsPossible.doubleValue
    val pointsPossible = column.pointsPossible.doubleValue
    val newGrade       =
      if column.isForCredit then Graded(pointsScored, pointsPossible, when)
      else NoCredit(pointsScored, pointsPossible, when)
    val result         = doGradeModification(user, section, structure, column, newGrade)._2

    val analyticsScore = AnalyticsScore(pointsScored, pointsPossible)
    coursewareAnalyticsService.emitGradePutEvent(user, section, content, analyticsScore)

    result
  end setGradePercent

  override def setGradePending(
    user: UserDTO,
    section: CourseSection,
    content: CourseContent,
    structure: GradeStructure,
    column: GradeColumn,
    when: Instant
  ): (Option[Grade], Grade) =
    log.info("Resetting grade to pending") // most callers have LogMeta.put-ed
    val newGrade = Pending(column.pointsPossible.doubleValue, when)
    val result   = doGradeModification(user, section, structure, column, newGrade)
    if result._1.flatMap(Grade.grade).isDefined then
      coursewareAnalyticsService.emitGradeUnsetEvent(content, section, user, userDto)
    result
  end setGradePending

  private def doGradeModification(
    user: UserDTO,
    section: CourseSection,
    structure: GradeStructure,
    column: GradeColumn,
    newGrade: Grade
  ): (Option[Grade], Grade) =
    val oldGradebook = getGrades(user, Crs(section.id), structure)
    val oldGrade     = oldGradebook.get(column.path)
    val newGradebook = oldGradebook.+(column.path, newGrade)
    dao.save(user, Crs(section.id), newGradebook.grades)
    sendGradeUpdates(section, user, column.path, structure, oldGradebook, newGradebook)
    oldGrade -> newGrade
  end doGradeModification

  override def unsetGrade(
    user: UserDTO,
    section: CourseSection,
    content: CourseContent
  ): ColumnMissing \/ Grade =

    val structure = GradeStructure(section.contents)
    structure
      .findColumnForEdgePath(content.edgePath)
      .toRightDisjunction(ColumnMissing(Crs(section.id), content.edgePath))
      .map(column =>
        log.info("Unsetting grade") // most callers have LogMeta.put-ed
        val newGrade = gradeDefault(column)
        doGradeModification(user, section, structure, column, newGrade)
        coursewareAnalyticsService.emitGradeUnsetEvent(content, section, user, userDto)
        newGrade
      )
  end unsetGrade

  override def scheduleGradeUpdate(section: LightweightCourse, changes: PublishAnalysis.LineItemContainer): Unit =
    if courseConfigurationService.getGroupConfig(CoursePreferences, section).updateOutcomesOnPublish.enabled then
      appEventService.fireEvent(
        section,
        RecalcGradesAppEvent(section.id, changes.creates, changes.updates, changes.deletes)
      )

  private def sendGradeUpdates(
    section: CourseSection,
    user: UserDTO,
    edgePath: EdgePath,
    structure: GradeStructure,
    oldGradebook: StudentGradebook,
    newGradebook: StudentGradebook,
  ): Unit =
    val columnOpt                 = structure.findColumnForEdgePath(edgePath)
    val categoryOpt               = columnOpt.map(structure.columnCategory).orElse(structure.findCategoryByEdgePath(edgePath))
    val overallGrade              = newGradebook.get(EdgePath.Root)
    // use the configured projected grade view for presence
    val newGradebookConfigApplied =
      StudentGradebook.applyRollupGradeViewConfig(courseConfigurationService, section, newGradebook)
    val displayedGrade            = newGradebookConfigApplied.get(EdgePath.Root)

    def sendToPresence(): Unit =
      // send column, category and overall updates as appropriate. presence gets the
      // projected overall grades if so configured.
      (columnOpt.map(_.path) ++ categoryOpt.map(_.path) ++ List(EdgePath.Root))
        .foreach { path =>
          val update =
            GradeUpdate(section.id, user.id, path, newGradebookConfigApplied.grades(path))
          presence.deliverToUsers(update)(user.id)
        }

    def sendLtiGrades(): Unit =
      // TODO: how is this related to `StudentGradebook.diff`?
      // but this is what it was like in 46ee968b2f6, and I'm just keeping that
      val diffs =
        for (path, _) <- (oldGradebook.grades.toSet &~ newGradebook.grades.toSet).iterator
        yield GradeChange(path, oldGradebook.get(path), newGradebook.get(path))
      // this rolls back if the messages could not be enqueued,
      // but not if they can't be delivered.
      diffs foreach { change =>
        ltiGradeSyncService.syncOutcomes1Grade(user, Crs(section.id), change.edgePath, change.newGrade)
        ltiGradeSyncService.syncAgsGrade(
          user,
          Crs(section.id),
          change.edgePath,
          change.newGrade,
        )
      }
    end sendLtiGrades

    /** If this course was launched via course link from an origin course, send a grade back. */
    def sendCourseLinkGrade(): Unit =
      for
        data          <- courseStorageService.get[UserGradeSyncHistory](section, user).courseLinkData
        if !antiLoop.value.contains(data.section)
        originSection <- courseSectionService.getCourseSection(data.section)
        originContent <- originSection.contents.get(data.edgePath)
      do
        overallGrade match
          case Some(Grade.Rollup(grade, max, when)) if isComplete(structure, newGradebook) =>
            // We only send a grade to the origin course when you've completed this course
            for
              // In preview sections there is a different target user
              person <- data.user.cata(lightweightUserService.getUserById, Some(user))
            do
              setGradePercent(person, originSection, originContent, grade / max, when)
              setLightweightProgress(GradeTarget(person, originSection, originContent), List(ProgressChange.visited))

          case _ =>
            if hasGrade(user, originSection, originContent) then
              unsetGrade(user, originSection, originContent)
              setLightweightProgress(GradeTarget(user, originSection, originContent), List(ProgressChange.unvisit))

    def updateGating(): Unit =
      gating.onGradeChange(user, Crs(section.id), oldGradebook, newGradebook)

    sendToPresence()
    sendLtiGrades()
    updateGating()

    antiLoop.withValue(antiLoop.value + section.id)(sendCourseLinkGrade())
  end sendGradeUpdates

  /** Has the student received grades for all for-credit assignments, aka vendor course completion. */
  private def isComplete(structure: GradeStructure, gradebook: StudentGradebook): Boolean =
    structure.columns.filter(_.isForCredit).forall(col => gradebook.get(col.path).exists(Grade.isGraded))

  /** Has the student received a grade for this content in this section. */
  private def hasGrade(user: UserDTO, section: CourseSection, content: CourseContent): Boolean =
    getGrades(user, Crs(section.id), section.contents).get(content.edgePath).exists(Grade.isGraded)

  private def setLightweightProgress(lwgt: GradeTarget, changes: List[EdgePath => ProgressChange]): Unit =
    lightweightProgressService
      .updateProgress(
        lwgt.section,
        lwgt.student,
        getGradebook(lwgt.section, lwgt.student),
        changes.map(_.apply(lwgt.edgePath))
      )
      .leftTap(e => s"Failed to update progress for ${lwgt.student.id} on ${lwgt.edgePath}: ${e.msg}")
end GradeServiceImpl

object GradeServiceImpl:
  private val log: org.log4s.Logger = org.log4s.getLogger

  // To avoid local grade update loops, capture the sections that this thread has already updated
  private val antiLoop = new DynamicVariable(Set.empty[Long])
