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

package loi.cp.offering

import cats.syntax.option.*
import com.google.common.base.Stopwatch
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.component.ComponentConstants
import com.learningobjects.cpxp.service.data.DataTypes.DATA_TYPE_DISABLED
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants.*
import com.learningobjects.cpxp.service.query.{Comparison, QueryBuilder, Function as QBFunction}
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.LocalFileInfo
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.index.OfferingIndexService
import loi.authoring.node.AssetNodeService
import loi.authoring.project.{AccessRestriction, Project, ProjectService}
import loi.authoring.workspace.AttachedReadWorkspace
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.content.{CourseContentServiceImpl, CourseContents}
import loi.cp.course.lightweight.{LightweightCourse, LightweightCourseService, Lwc}
import loi.cp.course.{CourseComponent, CourseConfigurationService, CourseFolderFacade, CoursePreferences}
import loi.cp.customisation.Customisation
import loi.cp.email.{CanAttach, MarshalEmailSupport}
import loi.cp.lti.{CourseColumnIntegrations, LtiColumnIntegrationService}
import loi.cp.lwgrade.{GradeService, GradeStructure}
import loi.cp.notification.NotificationService
import loi.cp.progress.LightweightProgressService
import loi.cp.structure.CourseStructureUploadService
import scalaz.syntax.either.*

import java.nio.charset.StandardCharsets
import javax.mail.internet.InternetAddress
import javax.mail.{Address, Message}
import scala.util.{Failure, Success, Try}

@Service
class ProjectOfferingServiceImpl(
  coursewareAnalyticsService: CoursewareAnalyticsService,
  courseConfigurationService: CourseConfigurationService,
  courseContentService: CourseContentServiceImpl,
  emailService: EmailService,
  gradeService: GradeService,
  ltiColumnIntegrationService: LtiColumnIntegrationService,
  userDto: => UserDTO
)(implicit
  facadeService: FacadeService,
  nodeService: AssetNodeService,
  notificationService: NotificationService,
  projectService: ProjectService,
  reindexService: OfferingIndexService,
  workspaceService: ReadWorkspaceService,
  lwcService: LightweightCourseService,
  progressService: LightweightProgressService,
  courseStructureUploadService: CourseStructureUploadService
) extends ProjectOfferingService:
  import ProjectOfferingServiceImpl.*

  override def reindexAllOfferings(): Unit =
    queryOfferings
      .addCondition(DATA_TYPE_DISABLED, Comparison.eq, false)
      .getComponents[LightweightCourse] foreach { offering =>
      reindexService.indexOffering(offering.id, offering.branch.id, offering.commitId)
    }

  override def analyzePublish(project: Project): PublishAnalysis =
    val branch   = projectService.loadMasterBranch(project)
    val sections = staleSections(branch)

    val updateOutcomesOnPublish =
      courseConfigurationService.getProjectDetail(CoursePreferences, project.id).value.updateOutcomesOnPublish.enabled

    if updateOutcomesOnPublish then

      lazy val (freshWs, _, freshCourse) = getCourse(project)
      lazy val freshContents             = courseContentService.computeCourseContentsImpl(freshWs, freshCourse, Customisation.empty)
      lazy val freshStructure            = GradeStructure(freshContents)

      val lisResultServiceChanges = for
        section            <- sections
        columnIntegrations <- ltiColumnIntegrationService.get(section)
        container          <- buildLineItemContainer(section, columnIntegrations, freshStructure, freshContents)
      yield container

      PublishAnalysis(sections.size, lisResultServiceChanges.toList)
    else PublishAnalysis(sections.size, Nil)
    end if
  end analyzePublish

  private def buildLineItemContainer(
    section: LightweightCourse,
    columnIntegrations: CourseColumnIntegrations,
    freshStructure: GradeStructure,
    freshContents: => CourseContents
  ): Option[PublishAnalysis.LineItemContainer] =
    val staleContents  = courseContentService.getCourseContents(section).get
    val staleStructure = GradeStructure(staleContents)

    val staleEdgePaths = staleStructure.columns.view.map(_.path).toSet
    val freshEdgePaths = freshStructure.columns.view.map(_.path).toSet
    val newEdgePaths   = freshEdgePaths -- staleEdgePaths
    val delEdgePaths   = staleEdgePaths -- freshEdgePaths

    val creates = for
      newEdgePath <- newEdgePaths.view
      column      <- freshStructure.findColumnForEdgePath(newEdgePath)
      content     <- freshContents.tree.findPath(_.edgePath == newEdgePath)
    yield PublishAnalysis.CreateLineItem.from(content, column)

    val updates = for
      prevColumn  <- staleStructure.columns
      column      <- freshStructure.findColumnForEdgePath(prevColumn.path)
      content     <- freshContents.tree.findPath(_.edgePath == prevColumn.path)
      syncHistory <- columnIntegrations.lineItems.get(prevColumn.path)
      lastSync    <- syncHistory.lastValid
      prevPp       = Option(prevColumn.pointsPossible).filter(_ != column.pointsPossible)
      prevFc       = Option(prevColumn.isForCredit).filter(_ != column.isForCredit)
      if prevPp.isDefined || prevFc.isDefined
    yield PublishAnalysis.UpdateLineItem.from(content, column, lastSync.syncedValue.id, prevPp, prevFc)

    val deletes = for
      delEdgePath <- delEdgePaths.view
      column      <- staleStructure.findColumnForEdgePath(delEdgePath)
      content     <- staleContents.tree.findPath(_.edgePath == delEdgePath)
      syncHistory <- columnIntegrations.lineItems.get(column.path)
      lastSync    <- syncHistory.lastValid
    yield PublishAnalysis.DeleteLineItem.from(content, column, lastSync.syncedValue.id)

    val container = PublishAnalysis.LineItemContainer(
      section.id,
      section.groupId,
      columnIntegrations.lineItemsUrl,
      columnIntegrations.systemId,
      creates.toList,
      updates,
      deletes.toList,
    )

    if container.hasChanges then container.some else None
  end buildLineItemContainer

  override def publishProject(project: Project): Unit =
    projectService.markPublished(project)
    val (ws, branch, course) = getCourse(project)
    val offering             = offerBranch(branch, course)

    coursewareAnalyticsService.emitPublishEvent(ws, offering, List.empty)
    courseStructureUploadService.pushOfferingToS3(offering)
    reindexService.indexOffering(offering.id, offering.branch.id, offering.commitId)

  override def updateProject(project: Project, message: Option[String]): Int =
    val stopwatch            = Stopwatch.createStarted()
    val (ws, branch, course) = getCourse(project)
    val offering             = offerBranch(branch, course)

    val projectConfig = courseConfigurationService.getProjectDetail(CoursePreferences, project.id).value

    val analysis = analyzePublish(project)
    val sections = staleSections(branch)
    message.foreach(notifySections(sections, _))
    updateOfferingsCommit(branch)
    updateCourseSections(sections, branch, course, analysis, projectConfig.updateOutcomesOnPublish.enabled)

    val updateOutcomesOnPublish = projectConfig.updateOutcomesOnPublish
    if updateOutcomesOnPublish.enabled && updateOutcomesOnPublish.emailTo.isDefined && analysis.lisResultChanges.nonEmpty
    then
      val toAddress   = parse(updateOutcomesOnPublish.emailTo.get)
      val ccAddresses = updateOutcomesOnPublish.emailCc.map(parse).getOrElse(Array.empty[Address])
      val subjCode    = project.code.getOrElse(project.name)

      val fileInfo = LocalFileInfo.createTempFile(s"${project.id}-lis-changes", ".csv")
      analysis.writeCsv(fileInfo.getFile)

      val content = MarshalEmailSupport.contentPart(
        s"The line items for project $subjCode have been changed. See attached CSV for list of changes",
        asHtml = false,
        fileInfo.getFile
      )(using CanAttach.forFile(s"$subjCode - LIS Result Line Item Changes.csv", MediaType.CSV_UTF_8.toString))

      emailService.sendEmail { email =>
        email.setFrom(MarshalEmailSupport.noreplyAtDomain)
        email.addRecipients(Message.RecipientType.TO, toAddress)
        email.addRecipients(Message.RecipientType.CC, ccAddresses)
        email.setSubject(s"$subjCode Line Items Updated", StandardCharsets.UTF_8.name())
        email.setContent(content)
      }
    end if

    coursewareAnalyticsService.emitPublishEvent(ws, offering, sections.toList)
    courseStructureUploadService.pushOfferingToS3(offering)
    reindexService.indexOffering(offering.id, offering.branch.id, offering.commitId)

    logger info s"updated content of ${sections.size} course sections (took $stopwatch)"
    sections.size
  end updateProject

  private def parse(addresses: String): Array[Address] =
    addresses
      .split(",")
      .map(_.trim())
      .flatMap(address =>
        Try(new InternetAddress(address)) match
          case Success(addr) => Some(addr)
          case Failure(ex)   =>
            logger.warn(ex)(s"failed to parse email address \"$address\"")
            None
      )

  override def countCourseSections(branch: Branch): Try[Int] =
    Success {
      sectionsFolder.queryGroups
        .addCondition(DATA_TYPE_GROUP_BRANCH, Comparison.eq, branch.id)
        .getAggregateResult(QBFunction.COUNT)
        .toInt
    }

  // TODO: this needs to get replaced when Malik's CourseOffering is available..
  // by which he meant to use the web controller DTO instead of this custom one.
  override def getOfferingForBranch(branch: Branch): Option[CourseOfferingDto] =
    allOfferings(branch).headOption map { lwc =>
      CourseOfferingDto(
        lwc.getCreatedBy.map(_.id),
        lwc.getCreateTime
      )
    }

  override def getOfferingComponentForBranch(branch: Branch): Option[LightweightCourse] =
    allOfferings(branch).headOption

  override def getCourseSections(offering: CourseComponent): List[LightweightCourse] =
    sectionsFolder
      .queryGroups()
      .addCondition(DATA_TYPE_GROUP_MASTER_COURSE, Comparison.eq, offering.id)
      .getComponents[LightweightCourse]
      .toList

  override def getOffering(course: Asset[Course], branch: Branch): Option[LightweightCourse] =
    queryOfferings
      .addCondition(DATA_TYPE_GROUP_BRANCH, Comparison.eq, branch.id)
      .addCondition(DATA_TYPE_GROUP_LINKED_ASSET_NAME, Comparison.eq, course.info.name.toString)
      .getComponent[LightweightCourse]

  override def offeredBranches(branches: Seq[Branch]): Set[Long] =
    queryOfferings
      .addCondition(DATA_TYPE_GROUP_BRANCH, Comparison.in, branches.map(_.id))
      .addCondition(DATA_TYPE_DISABLED, Comparison.eq, false)
      .addCondition(DATA_TYPE_GROUP_ARCHIVED, Comparison.eq, false)
      .setDataProjection(DATA_TYPE_GROUP_BRANCH)
      .getValues[Long]
      .toSet

  override def deleteProject(project: Project): Unit =
    // Archive course offerings
    allOfferings(project).foreach(_.setArchived(true))
    offeringsFolder.invalidate()
    // Delete test sections
    allTestSections(project).foreach(_.delete())

  override def queryOfferings: QueryBuilder =
    offeringsFolder.queryGroups

  override def invalidateOfferings(): Unit =
    offeringsFolder.invalidate()

  private def queryTestSections: QueryBuilder =
    testSectionsFolder.queryGroups

  /** Find all test sections on this branch. */
  private def allTestSections(branch: Branch): Seq[LightweightCourse] =
    queryTestSections
      .addCondition(DATA_TYPE_GROUP_BRANCH, Comparison.eq, branch.id)
      .getComponents[LightweightCourse]

  /** Find all test sections tied to this project. */
  private def allTestSections(project: Project): Seq[LightweightCourse] =
    queryTestSections
      .addCondition(DATA_TYPE_GROUP_PROJECT, Comparison.eq, project.id)
      .getComponents[LightweightCourse]

  /** Create new course offerings for all unoffered courses in this branch. */
  private def offerBranch(
    branch: Branch,
    course: Asset[Course]
  ): Lwc =
    allOfferings(branch).headOption match
      case Some(existing) => updateOffering(course, existing)
      case None           => offerCourse(branch, course)

  /** Create an offering for a course in a branch. */
  private def offerCourse(branch: Branch, course: Asset[Course]): LightweightCourse =
    logger info s"Öffering course ${course.data.title}"
    val init     = new CourseComponent.Init(
      name = course.data.title,
      groupId = java.util.UUID.randomUUID().toString,
      groupType = GroupType.CourseOffering,
      createdBy = userDto,
      source = (course -> branch).right
    )
    val offering = offeringsFolder.addCourse(LightweightCourse.Identifier, init).asInstanceOf[LightweightCourse]
    courseConfigurationService.copyConfigOrThrow(CoursePreferences, branch.requireProject.id, offering)
    offering
  end offerCourse

  /** Update an offering. */
  private def updateOffering(course: Asset[Course], lwc: LightweightCourse): LightweightCourse =
    courseConfigurationService.copyConfigOrThrow(CoursePreferences, lwc.getProjectId.get, lwc)
    lwc.setCourseId(course.info.id)
    lwc.setName(course.data.title)
    lwcService.incrementGeneration(lwc)
    lwc

  /** Notify instructors in all stale sections of the update. */
  private def notifySections(staleSections: Seq[LightweightCourse], message: String): Unit =
    staleSections foreach { section =>
      val init = UpdateNotificationData(section.getId, message)
      notificationService.nοtify[UpdateNotification](section, init)
    }

  /** Find all course sections not on this commit of a branch. */
  private def staleSections(branch: Branch): Seq[LightweightCourse] =
    sectionsFolder.queryGroups
      .addCondition(
        ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER,
        Comparison.eq,
        LightweightCourse.Identifier
      ) // TODO: NOT THIS BECAUSE CUSTOM COURSE TYPES
      .addCondition(DATA_TYPE_GROUP_BRANCH, Comparison.eq, branch.id)
      .addCondition(DATA_TYPE_COMMIT, Comparison.ne, branch.head.id)
      .getComponents[LightweightCourse]

  /** Find all course offerings on this branch. */
  private def allOfferings(branch: Branch): Seq[LightweightCourse] =
    queryOfferings
      .addCondition(DATA_TYPE_GROUP_BRANCH, Comparison.eq, branch.id)
      .getComponents[LightweightCourse]

  /** Find all course offerings tied to this project.. */
  private def allOfferings(project: Project): Seq[LightweightCourse] =
    queryOfferings
      .addCondition(DATA_TYPE_GROUP_PROJECT, Comparison.eq, project.id)
      .getComponents[LightweightCourse]

  private def getCourse(project: Project): (AttachedReadWorkspace, Branch, Asset[Course]) =
    // For audit-failing domains, this is a bug.
    // `groupfinder g where g.project = project.id` may exist such that `g.branch != <project's master branch>`
    val branch = projectService.loadMasterBranch(project)

    val ws     = workspaceService.requireReadWorkspace(branch.id, AccessRestriction.none)
    val course = nodeService.loadA[Course](ws).byName(ws.homeName).get

    (ws, branch, course)

  /** Update the commit id of all offerings from a branch. */
  private def updateOfferingsCommit(branch: Branch): Unit =
    offeringsFolder.invalidate()
    allOfferings(branch) foreach { offering =>
      offering.setCommitId(branch.head.id)
    }

  private def updateCourseSections(
    staleSections: Seq[LightweightCourse],
    branch: Branch,
    course: Asset[Course],
    analysis: PublishAnalysis,
    updateOutcomes: Boolean,
  ): Unit =
    sectionsFolder.invalidate()
    staleSections foreach { section =>
      section.setCommitId(branch.head.id)
      section.setCourseId(course.info.id)
      lwcService.updateSection(section)
      // TODO only update progress documents if there is a relevant change in content
      progressService.scheduleProgressUpdate(section.id)

      if updateOutcomes then
        analysis.lisResultChanges.find(_.sectionId == section.id).foreach { changes =>
          gradeService.scheduleGradeUpdate(section, changes)
        }
    }
  end updateCourseSections

  private def offeringsFolder: CourseFolderFacade =
    ID_FOLDER_COURSE_OFFERINGS.facade[CourseFolderFacade]

  private def sectionsFolder: CourseFolderFacade =
    ID_FOLDER_COURSES.facade[CourseFolderFacade]

  private def testSectionsFolder: CourseFolderFacade =
    ID_FOLDER_TEST_SECTIONS.facade[CourseFolderFacade]
end ProjectOfferingServiceImpl

object ProjectOfferingServiceImpl:
  private final val logger = org.log4s.getLogger
