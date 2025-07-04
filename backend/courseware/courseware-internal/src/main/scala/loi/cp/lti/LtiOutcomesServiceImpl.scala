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

package loi.cp.lti

import argonaut.Json
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.user.UserId
import de.tomcat.juli.LogMeta
import jakarta.servlet.http.HttpServletRequest
import loi.cp.asset.assessmenttype.WithAssetAssessmentType
import loi.cp.bus.MessageBusService
import loi.cp.content.CourseContent
import loi.cp.context.ContextId
import loi.cp.course.CourseSection
import loi.cp.integration.BasicLtiSystemComponent
import loi.cp.lti.LtiItemSyncStatus.*
import loi.cp.lti.lightweight.{CreateLineItemMessage, DeleteLineItemMessage}
import loi.cp.lti.storage.{LtiCallback, UserGradeSyncHistory}
import loi.cp.lwgrade.{GradeColumn, GradeStructure}
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStorageService
import loi.cp.user.UserComponent
import scalaz.syntax.std.option.*
import scalaz.{State, \/}
import scaloi.misc.{Stringomorphism, TimeSource}
import scaloi.syntax.option.*

import java.net.{URI, URL}
import scala.util.{Failure, Success, Try}

@Service
final class LtiOutcomesServiceImpl(
  courseStorageService: CourseStorageService,
  ltiColumnIntegrationService: LtiColumnIntegrationService,
  messageBusService: MessageBusService,
  ts: TimeSource
)(implicit cs: ComponentService)
    extends LtiOutcomesService:
  import LtiOutcomesServiceImpl.*

  override def processLaunch(
    course: CourseSection,
    user: UserComponent,
    content: Option[CourseContent],
    systemId: Long,
    instructorLike: Boolean
  )(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent
  ): LtiError \/ Unit =
    if isAgs && !system.getBasicLtiConfiguration.forceLti1Outcomes.isTrue then configureAgs(course, systemId)
    else configureOutcomes1(course, user, content, instructorLike)

  import scalaz.std.list.*
  import scalaz.syntax.traverse.*

  override def manuallySyncColumns(lwc: CourseSection): Unit =
    for
      config <- Try(ltiColumnIntegrationService.get(lwc)).toOption.flatten
      system <- config.systemId.component_?[BasicLtiSystemComponent]
    do
      val (newConfig, _) = GradeStructure(lwc.contents).columns
        .traverse({ column => syncAgsStateful(lwc, column, config.lineItemsUrl, system) })
        .apply(Some(config))
      ltiColumnIntegrationService.set(lwc, newConfig)

  override def manuallySyncColumn(lwc: CourseSection, column: GradeColumn): Unit =
    for
      config <- Try(ltiColumnIntegrationService.get(lwc)).toOption.flatten
      system <- config.systemId.component_?[BasicLtiSystemComponent]
    yield syncAgs(lwc, column, config.lineItemsUrl, system)

  override def deleteColumn(lwc: CourseSection, edgePath: EdgePath): Unit = for
    config   <- Try(ltiColumnIntegrationService.get(lwc)).toOption.flatten
    system   <- config.systemId.component_?[BasicLtiSystemComponent]
    lineItem <- config.lineItems.get(edgePath)
    synced   <- lineItem.lastValid
  do
    val id = synced.syncedValue.id
    Try(URI.create(id)) match
      case Success(_) =>
        messageBusService.publishMessage(system, DeleteLineItemMessage(id, edgePath, lwc.id))
      case Failure(e) =>
        LogMeta.let(
          "sectionId" -> Json.jNumber(lwc.id),
          "edgePath"  -> Json.jString(edgePath.toString)
        )(logger.info(e)("skip DeleteLineItem; line item id is not a URI"))

  private def isAgs(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): Boolean =
    ltiParam(AgsLineItemsUrlParameter).exists(_.isDefined)

  private def configureOutcomes1(
    course: ContextId,
    user: UserComponent,
    content: Option[CourseContent],
    instructorLike: Boolean,
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ Unit =
    for
      resultSourceDidOpt <- ltiParam(BasicOutcomesResultSourceDidParameter)
      serviceUrlOpt      <- ltiParamT[URL](BasicOutcomesServiceUrlParameter)
    yield for
      resultSourceDid <- resultSourceDidOpt
      serviceUrl      <- serviceUrlOpt
      if !instructorLike
    do
      val edgePath = content.cata(_.edgePath, EdgePath.Root)
      setOutcomes1Config(course, UserId(user.id()), edgePath, serviceUrl, resultSourceDid, system)

  override def setOutcomes1Config(
    context: ContextId,
    user: UserId,
    edgePath: EdgePath,
    serviceUrl: URL,
    resultSourceDid: String,
    system: BasicLtiSystemComponent
  ): Unit =
    val callback = LtiCallback(system.getId, serviceUrl, resultSourceDid, None)
    courseStorageService
      .modify[UserGradeSyncHistory](context, user)(_.addOutcomes1Callback(edgePath)(callback))
  end setOutcomes1Config

  private def configureAgs(
    course: CourseSection,
    systemId: Long
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ Unit =
    for lineItemsUrlOpt <- ltiParamT[URL](AgsLineItemsUrlParameter)
    yield
      // If line items url is specified and the course has no config then sync things.
      lineItemsUrlOpt.when(agsConfig(course).isEmpty) foreach { lineItemsUrl =>
        setAgsConfig(course, lineItemsUrl, system)
      }

  override def setAgsConfig(section: CourseSection, lineItemsUrl: URL, system: BasicLtiSystemComponent): Unit =
    GradeStructure(section.contents).columns foreach { column =>
      syncAgs(section, column, lineItemsUrl.toExternalForm, system)
    }

  private def agsConfig(lwc: ContextId): Option[CourseColumnIntegrations] =
    ltiColumnIntegrationService.get(lwc)

  private def syncAgsStateful(
    lwc: CourseSection,
    column: GradeColumn,
    lineItemsUrl: String,
    system: BasicLtiSystemComponent
  ): State[Option[CourseColumnIntegrations], Unit] =
    State(prior =>
      val newStorage = lwc.contents
        .get(column.path)
        .fold(prior) { content =>
          val message = CreateLineItemMessage(
            lineItemsUrl = lineItemsUrl,
            content = column.path,
            contextId = lwc.getId,
            assetAssessmentType = WithAssetAssessmentType.maybeFromAsset(content.asset),
            assetType = content.asset.info.typeId,
            assetName = content.asset.info.name,
            column = column
          )
          messageBusService.publishMessage(system, message)
          prior.map(_.pushAgsStatus(content.edgePath, Queued(ts.instant)))
        }
      (newStorage, ())
    )

  private def syncAgs(
    lwc: CourseSection,
    column: GradeColumn,
    lineItemsUrl: String,
    system: BasicLtiSystemComponent
  ): Unit =
    val existing  = agsConfig(lwc)
    val newConfig = syncAgsStateful(lwc, column, lineItemsUrl, system)
      .apply(Some(existing.getOrElse(CourseColumnIntegrations.empty(system.getId, lineItemsUrl))))
    ltiColumnIntegrationService.set(lwc, newConfig._1)
  end syncAgs
end LtiOutcomesServiceImpl

object LtiOutcomesServiceImpl:

  private final val AgsLineItemsUrlParameter              = "custom_lineitems_url"
  private final val BasicOutcomesResultSourceDidParameter = "lis_result_sourcedid"
  private final val BasicOutcomesServiceUrlParameter      = "lis_outcome_service_url"

  implicit val urlStringomorphism: Stringomorphism[URL] = s => Try(new URI(s).toURL)

  private final val logger: org.log4s.Logger = org.log4s.getLogger
