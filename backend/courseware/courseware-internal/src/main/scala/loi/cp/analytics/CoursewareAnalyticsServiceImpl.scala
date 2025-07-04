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

import cats.instances.list.*
import cats.instances.option.*
import cats.syntax.foldable.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.discussion.PostValue
import com.learningobjects.cpxp.component.query.ApiQuery
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.session.SessionDTO
import com.learningobjects.cpxp.service.user.UserDTO
import loi.asset.TitleProperty
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.workspace.ReadWorkspace
import loi.cp.analytics.CoursewareAnalyticsService.courseId
import loi.cp.analytics.CoursewareAnalyticsServiceImpl.{MegaContent, SuperContent}
import loi.cp.analytics.entity.{Score as EScore, *}
import loi.cp.analytics.event.*
import loi.cp.analytics.event.PublishContentEvent.{Asset1, Content1}
import loi.cp.assessment.attempt.AssessmentAttempt
import loi.cp.content.{CourseContent, CourseContentService, CourseContents}
import loi.cp.course.lightweight.{LightweightCourse, Lwc}
import loi.cp.course.{CourseComponent, CourseSection}
import loi.cp.discussion.Discussion
import loi.cp.reference.EdgePath
import loi.cp.survey.{SurveyContentService, SurveyQuestionResponseDto}
import scalaz.std.option.*
import scalaz.syntax.functor.*
import scaloi.data.ListTree
import scaloi.misc.TimeSource

import java.time.Instant
import java.util.UUID
import scala.jdk.CollectionConverters.*

@Service
class CoursewareAnalyticsServiceImpl(
  analyticsService: AnalyticsService,
  courseContentService: CourseContentService,
  surveyContentService: SurveyContentService,
  domainDto: => DomainDTO,
  sessionDto: => SessionDTO,
  timeSource: TimeSource,
  userDto: => UserDTO,
) extends CoursewareAnalyticsService:

  override def emitSectionEntryEvent(sectionId: Long, role: String, originSectionId: Option[Long]): Unit =

    val event = SectionEntryEvent1(
      id = UUID.randomUUID(),
      session = sessionDto.id,
      source = domainDto.hostName,
      time = timeSource.date,
      user = analyticsService.userData(userDto),
      role = role,
      sectionId = sectionId,
      originSectionId = originSectionId,
    )

    analyticsService.emitEvent(event)
  end emitSectionEntryEvent

  override def emitSectionCreateEvent(section: CourseComponent, offering: LightweightCourse): Unit =

    val contents = courseContentService.getCourseContents(offering).get
    val ws       = section.getWorkspace

    val eeeContents     = toPceContents(contents)
    val (_, sqContents) = surveyQuestions(ws, contents)

    val event = SectionCreateEvent2(
      id = UUID.randomUUID(),
      session = sessionDto.id,
      source = domainDto.hostName,
      time = timeSource.date,
      section = SectionData(section.id, section.externalId, section.getName),
      offeringId = offering.getId,
      offeringGroupId = offering.getGroupId,
      offeringName = offering.getName,
      contents = eeeContents ++ sqContents,
      integration = getIntegrationData(section),
      disabled = section.getDisabled,
      startDate = section.getStartDate,
      endDate = section.getEndDate,
      groupId = Option(section.groupId)
    )

    analyticsService.emitEvent(event)
  end emitSectionCreateEvent

  override def emitSectionUpdateEvent(section: CourseComponent): Unit =

    val offering = section.getOffering
    val event    = SectionUpdateEvent2(
      id = UUID.randomUUID(),
      session = sessionDto.id,
      source = domainDto.hostName,
      time = timeSource.date,
      section = SectionData(section.id, section.externalId, section.getName),
      offeringId = offering.getId,
      offeringGroupId = offering.getGroupId,
      offeringName = offering.getName,
      integration = getIntegrationData(section),
      disabled = section.getDisabled,
      startDate = section.getStartDate,
      endDate = section.getEndDate,
      groupId = Option(section.groupId)
    )

    analyticsService.emitEvent(event)
  end emitSectionUpdateEvent

  override def emitPublishEvent(
    ws: ReadWorkspace,
    offering: Lwc,
    sections: List[Lwc]
  ): PublishContentEvent1 =

    val contents = courseContentService.getCourseContents(offering).get

    val (eeeAssets, eeeContents) = elementsElementsElements(contents)
    val (sqAssets, sqContents)   = surveyQuestions(ws, contents)

    val event = PublishContentEvent1(
      id = UUID.randomUUID(),
      time = timeSource.date,
      session = sessionDto.id,
      source = domainDto.hostName,
      assets = eeeAssets ++ sqAssets,
      contents = eeeContents ++ sqContents,
      sectionIds = sections.map(_.getId.toLong),
    )

    analyticsService.emitEvent(event)

    event
  end emitPublishEvent

  private def labelWithAncestors(ancestors: List[SuperContent], label: CourseContent, i: Int): SuperContent =
    val parent = ancestors.headOption
    parent match
      case None    => SuperContent(label, i) // label is the course root content
      case Some(p) =>
        p.content.asset.assetType.id match
          case AssetTypeId.Course => SuperContent(label, i, Some(p), Some(p))
          case AssetTypeId.Module => SuperContent(label, i, Some(p), p.course, Some(p), None)
          case AssetTypeId.Lesson => SuperContent(label, i, Some(p), p.course, p.module, Some(p))
          case _                  => SuperContent(label, i, Some(p), p.course, p.module, p.lesson)
  end labelWithAncestors

  private def labelWithDescendants(
    label: SuperContent,
    descendants: List[ListTree[MegaContent]]
  ): ListTree[MegaContent] =
    val (pointsPossible, itemCount) = if label.content.isContainer then
      val children = descendants.map(_.rootLabel)
      val points   = children.map(_.forCreditPointsPossible).combineAll
      val items    = children.map(_.itemCountOr1ForSelf).sum
      (points, Some(items))
    else
      val points =
        if label.content.gradingPolicy.exists(_.isForCredit) then
          label.content.gradingPolicy.map(_.pointsPossible).map(BigDecimal.apply)
        else None
      (points, None)

    ListTree.Node(label.megaContent(pointsPossible, itemCount), descendants)
  end labelWithDescendants

  private def toPceContents(courseContents: CourseContents): List[PublishContentEvent.Content1] =

    courseContents.tree
      .tdhistoWithIndex(labelWithAncestors) // map from top down first to access properties of ancestors
      .rebuild(labelWithDescendants)        // map from bottom up second to access properties of descendants
      .map(label =>
        PublishContentEvent.Content1(
          label.content.asset.info.id,
          label.content.edgePath.toString,
          Some(label.learningPathIndex),
          label.forCreditPointsPossible,
          label.forCreditItemCount,
          label.parent.map(_.superAssetId),
          label.course.map(_.superAssetId),
          label.module.map(_.superAssetId),
          label.lesson.map(_.superAssetId)
        )
      )
      .flatten

  private def elementsElementsElements(courseContents: CourseContents): (List[Asset1], List[Content1]) =

    val assets   = courseContents.tree.flatCollect({ case c =>
      PublishContentEvent.Asset1(
        c.asset.info.id,
        c.asset.info.name,
        c.asset.info.typeId.entryName,
        Some(c.title),
        c.asset.keywords,
        c.gradingPolicy.map(_.isForCredit),
        c.gradingPolicy.map(_.pointsPossible)
      )
    })
    val contents = toPceContents(courseContents)

    (assets, contents)
  end elementsElementsElements

  private def surveyQuestions(
    ws: ReadWorkspace,
    courseContents: CourseContents
  ): (List[Asset1], List[Content1]) =
    val surveyTrees = surveyContentService.loadSurveyTrees(ws, courseContents.nonRootElements)

    surveyTrees
      .flatMap(tree =>
        surveyStuff2AnalyticContents(tree.surveyAsset, tree.surveyEdgePath) +:
          tree.questions.map(content => surveyStuff2AnalyticContents(content.asset, content.edgePath))
      )
      .unzip
  end surveyQuestions

  private def surveyStuff2AnalyticContents(asset: Asset[?], edgePath: EdgePath): (Asset1, Content1) = (
    PublishContentEvent.Asset1(
      asset.info.id,
      asset.info.name,
      asset.info.typeId.entryName,
      TitleProperty.fromNode(asset),
      asset.keywords,
      None,
      None
    ),
    PublishContentEvent.Content1(
      asset.info.id,
      edgePath.toString,
      None,
      None,
      None,
      // `content` is not in the elements-elements-elements region by decree
      None,
      None,
      None,
      None,
    )
  )

  override def emitSurveySubmissionEvent(
    section: Lwc,
    content: CourseContent,
    survey: Asset[?],
    surveyEdgePath: EdgePath,
    responses: List[SurveyQuestionResponseDto]
  ): Unit =

    analyticsService.emitEvent(
      SurveySubmissionEvent2(
        id = UUID.randomUUID(),
        time = timeSource.date,
        session = sessionDto.id,
        source = domainDto.hostName,
        userId = userDto.id,
        sectionId = section.id,
        attemptId = UUID.randomUUID(),
        contentAssetId = content.asset.info.id,
        contentEdgePath = content.edgePath.toString,
        surveyAssetId = survey.info.id,
        surveyEdgePath = surveyEdgePath.toString,
        responses = responses.map(r => SurveySubmissionEvent.QuestionResponse1(r.questionAssetId, r.response)),
      )
    )

  def emitDiscussionPostPutEvent(
    post: PostValue,
    discussion: Discussion,
    instructorReplyUserId: Option[Long],
    instructorReplyTime: Option[Instant]
  ): Unit =

    analyticsService.emitEvent(
      DiscussionPostPutEvent1(
        id = UUID.randomUUID(),
        session = sessionDto.id,
        source = domainDto.hostName,
        time = timeSource.date,
        postId = post.id,
        userId = post.user.id,
        sectionId = discussion.section.id,
        edgePath = discussion.courseContent.edgePath.toString,
        assetId = discussion.courseContent.asset.info.id,
        // in general: a cheat. but specifically to vendor: not a cheat
        role = if post.moderatorPost then "instructor" else "student",
        depth = post.depth.toInt,
        createTime = post.created,
        instructorReplyUserId = instructorReplyUserId,
        instructorReplyTime = instructorReplyTime,
      )
    )

  private def getIntegrationData(section: CourseComponent): Option[IntegrationData] =
    section.getIntegrationRoot
      .getIntegrations(ApiQuery.ALL)
      .asScala
      .headOption
      .map(i =>
        IntegrationData(i.getUniqueId, Option(i.getSystem).map(s => SystemData(s.getId, s.getSystemId, s.getName)))
      )

  override def emitAttemptPutEvent(
    attempt: AssessmentAttempt,
    manualScore: Boolean,
    maintenance: Boolean
  ): Unit =
    analyticsService.emitEvent(
      AttemptPutEvent1(
        id = UUID.randomUUID(),
        time = timeSource.date,
        source = domainDto.hostName,
        session = sessionDto.id,
        attemptId = attempt.id.getId,
        userId = attempt.user.id,
        sectionId = attempt.contextId,
        edgePath = attempt.edgePath.toString,
        assetId = attempt.assessment.courseContent.asset.info.id,
        state = attempt.state.entryName,
        valid = attempt.valid,
        manualScore = manualScore,
        createTime = attempt.createTime,
        submitTime = attempt.submitTime,
        scoreTime = attempt.scoreTime,
        scorePointsAwarded = attempt.score.map(_.pointsAwarded),
        scorePointsPossible = attempt.score.map(_.pointsPossible),
        scorerUserId = attempt.scorer,
        maintenance = if maintenance then Some(true) else None,
        maxMinutes = attempt.maxMinutes,
        autoSubmitted = attempt.maxMinutes.as(attempt.autoSubmitted),
      )
    )

  override def emitGradePutEvent(
    learner: UserDTO,
    section: CourseSection,
    content: CourseContent,
    score: EScore,
    maintenance: Boolean = false,
  ): Unit =
    analyticsService.emitEvent(
      GradePutEvent1(
        id = UUID.randomUUID(),
        time = timeSource.date,
        source = domainDto.hostName,
        sessionId = Some(sessionDto.id),
        learner = learner,
        section = ExternallyIdentifiableEntity(section.id, section.externalId),
        edgePath = content.edgePath.toString,
        assetId = content.asset.info.id,
        forCredit = content.gradingPolicy.exists(_.isForCredit),
        score = score,
        maintenance = if maintenance then Some(true) else None
      )
    )

  override def emitGradeUnsetEvent(
    content: CourseContent,
    section: CourseSection,
    learner: UserDTO,
    scorer: UserDTO,
    maintenance: Boolean,
  ): Unit =
    analyticsService.emitEvent(
      GradeUnsetEvent(
        id = UUID.randomUUID(),
        session = sessionDto.id,
        source = domainDto.hostName,
        time = timeSource.date,
        course = courseId(section),
        contentId = ContentId(content.edgePath.toString, content.asset.info.name),
        learner = learner,
        subject = scorer,
        maintenance = if maintenance then Some(true) else None
      )
    )

end CoursewareAnalyticsServiceImpl

object CoursewareAnalyticsServiceImpl:

  // CourseContent with ancestor data
  private case class SuperContent(
    content: CourseContent,
    learningPathIndex: Int,
    parent: Option[SuperContent] = None,
    course: Option[SuperContent] = None,
    module: Option[SuperContent] = None,
    lesson: Option[SuperContent] = None,
  ):
    val superAssetId: PublishContentEvent.SuperAssetId =
      PublishContentEvent.SuperAssetId(content.asset.info.id, content.edgePath.toString)

    def megaContent(forCreditPointsPossible: Option[BigDecimal], forCreditItemCount: Option[Int]): MegaContent =
      MegaContent(
        content,
        learningPathIndex,
        parent,
        course,
        module,
        lesson,
        forCreditPointsPossible,
        forCreditItemCount
      )
  end SuperContent

  // CourseContent with ancestors data and descendant data
  private case class MegaContent(
    content: CourseContent,
    learningPathIndex: Int,
    parent: Option[SuperContent],
    course: Option[SuperContent],
    module: Option[SuperContent],
    lesson: Option[SuperContent],
    forCreditPointsPossible: Option[BigDecimal],
    forCreditItemCount: Option[Int]
  ):

    // `forCreditItemCount` is None for a non-container because its a non-container content item.
    // But when counting children for a container content item, I need each child to count itself if it is for-credit
    val itemCountOr1ForSelf: Int =
      if content.isContainer then forCreditItemCount.getOrElse(0)
      else if content.gradingPolicy.exists(_.isForCredit) then 1
      else 0
  end MegaContent
end CoursewareAnalyticsServiceImpl
