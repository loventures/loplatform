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

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.util.Stopwatch
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainWebService}
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.{GroupConstants, GroupFacade}
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.subtenant.SubtenantFacade
import com.learningobjects.cpxp.service.user.{UserDTO, UserFacade}
import com.learningobjects.cpxp.util.tx.BeforeTransactionCompletionListener
import com.learningobjects.cpxp.util.{EntityContext, FormattingUtils}
import loi.cp.analytics.entity.{CourseId, ExternallyIdentifiableEntity, UserData}
import loi.cp.analytics.event.*
import loi.cp.bus.BusFailureNotificationService
import loi.cp.config.ConfigurationService
import loi.cp.course.CourseComponent

import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@Service
class AnalyticsServiceImpl(
  domain: => DomainDTO,
  ec: => EntityContext
)(implicit
  fs: FacadeService,
  cs: ComponentService
) extends AnalyticsService:
  import AnalyticsServiceImpl.*

  override def emitEvent(event: Event): Unit =
    if Event.sectionId(event).forall(isCourseSection) then
      ec.getOrAddCompletionListener(classOf[AnalyticsCompletionListener], () => new AnalyticsCompletionListener)
        .push(event)

  override def insertEvent(event: Event, domain: Long): Unit =
    domain.addFacade[AnalyticFacade] { a =>
      a.setGuid(event.id)
      a.setTime(event.time)
      a.setDataJson(event)
    }

  private def isCourseSection(sectionId: Long): Boolean =
    sectionId.facade_?[GroupFacade].exists(_.getGroupType == GroupConstants.GroupType.CourseSection)

  private[analytics] override def pumpPoller(): Unit =
    poller foreach (_.pump())

  override def courseId(courseId: Long): Option[CourseId] =
    courseId.tryComponent[CourseComponent].map { course =>
      CourseId(
        section = ExternallyIdentifiableEntity(
          id = course.id,
          externalId = course.getExternalId.toScala
        ),
        offeringId = course.getOfferingId,
        branchId = course.getBranchId.toScala.map(_.longValue()),
        commitId = course.getCommitId.toScala.map(_.longValue()),
        assetGuid = Some(course.loadCourse().info.name),
        projectId = course.getProjectId.toScala.map(_.longValue())
      )
    }

  // This technically reaches out to the database though in most cases should hit the query and L2 caches.
  override def userData(u: UserDTO): UserData =
    val facadeData = u.facade_?[UserFacade] map { user =>
      // 99.99% of users have 1, the remainder have duplicates
      val su = for
        integration <- user.getIntegrations.asScala.sortBy(_.getId).headOption
        uniqueId    <- Option(integration.getUniqueId)
        system      <- Option(integration.getExternalSystemFacade)
      yield system.getSystemId -> uniqueId

      (su, FormattingUtils.userStr(user))
    }

    val subtenant = u.subtenantId.flatMap(_.facade_?[SubtenantFacade])

    UserData(
      u.id,
      u.externalId,
      Option(u.emailAddress),
      Option(u.userName),
      Option(u.givenName),
      Option(u.familyName),
      facadeData.map(_._2),
      subtenant.map(_.getTenantId),
      subtenant.map(_.getName),
      facadeData.flatMap(_._1.map(_._1)),
      facadeData.flatMap(_._1.map(_._2))
    )
  end userData

  class AnalyticsCompletionListener extends BeforeTransactionCompletionListener:
    private val did    = domain.id
    private val events = mutable.Buffer.empty[Event]

    private[analytics] def push(event: Event): Unit = events.append(event)

    override def beforeCommit(): Unit =
      val sw = new Stopwatch
      log.info(s"Emitting ${events.size} analytics events")
      events foreach { event =>
        insertEvent(event, did)
      }
      log.info(s"Emitted ${events.size} analytics events in ${sw.elapsed}")
    end beforeCommit

    override def beforeRollback(): Unit = ()
  end AnalyticsCompletionListener
end AnalyticsServiceImpl

object AnalyticsServiceImpl:

  private val log = org.log4s.getLogger

  /** Analytics poller. */
  private var poller: Option[AnalyticsPoller] = None

  /** Start the analytics poller at startup.
    */
  @SuppressWarnings(Array("unused"))
  def startAnalytics(implicit
    cs: ConfigurationService,
    dws: DomainWebService,
    es: EmailService,
    fs: FacadeService,
    is: ItemService,
    mapper: ObjectMapper,
    ows: OverlordWebService,
    qs: QueryService,
    sm: ServiceMeta,
    fns: BusFailureNotificationService,
    cs2: ComponentService
  ): Unit =
    poller = Some(AnalyticsPoller.start)

  /** Stop the analytics poller at shutdown.
    */
  @PreUnload
  @PreShutdown
  @SuppressWarnings(Array("unused"))
  def stopAnalytics(): Unit =
    poller foreach { _.shutdown() }
    poller = None
end AnalyticsServiceImpl
