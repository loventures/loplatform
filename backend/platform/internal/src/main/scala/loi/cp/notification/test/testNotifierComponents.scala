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

package loi.cp.notification.test

import java.lang.Long as jLong
import java.util.Date

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{
  ComponentImplementation,
  ComponentInstance,
  ComponentInterface,
  ComponentService
}
import com.learningobjects.cpxp.dto.FacadeItem
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.component.ComponentConstants
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupWebService
import com.learningobjects.cpxp.service.query.{Comparison, QueryService}
import com.learningobjects.cpxp.service.script.ComponentFacade
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.web.QueryableId
import loi.cp.context.ContextId
import loi.cp.notification.*

/** Test event emitting entity.
  */
@VisibleForTesting
@Schema("testNotifier")
trait TestNotifier extends ComponentInterface with QueryableId:
  @JsonProperty("context_id")
  def getContext: Long

  @RequestMapping(path = "notify", method = Method.POST)
  def notify(@RequestBody notification: TestNotificationData, @QueryParam("immediate") immediate: Boolean): Unit

  @RequestMapping(method = Method.DELETE)
  def delete(): Unit
end TestNotifier

@VisibleForTesting
@Component(enabled = false)
class TestNotifierImpl(
  val componentInstance: ComponentInstance,
  self: TestNotifierFacade,
  ns: NotificationServiceImpl
) extends TestNotifier
    with ComponentImplementation:
  override def getId: jLong = componentInstance.getId

  override def getContext: Long = self.getParentId

  override def notify(notification: TestNotificationData, immediate: Boolean): Unit =
    if immediate then ns.notifyImmediate(this, classOf[TestNotificationImpl], notification)
    else ns.notify(this, classOf[TestNotificationImpl], notification)

  override def delete(): Unit = self.delete()
end TestNotifierImpl

@FacadeItem(ComponentConstants.ITEM_TYPE_COMPONENT)
trait TestNotifierFacade extends ComponentFacade

case class TestNotificationData(
  time: Date,
  @JsonDeserialize(contentAs = classOf[jLong]) sender: Option[Long],
  topic: Option[String],
  @JsonDeserialize(contentAs = classOf[jLong]) context: Option[Long],
  @JsonDeserialize(contentAs = classOf[jLong]) audience: Seq[Long],
  interest: Interest
)

/** A root component for managing test notifiers.
  */
@VisibleForTesting
@Controller(value = "testNotifiers", root = true)
@RequestMapping(path = "testNotifiers")
trait TestNotifierRootApi extends ApiRootComponent:
  @RequestMapping(method = Method.POST)
  def create(@RequestBody init: TestNotifierInit): TestNotifier

  @RequestMapping(path = "{id}", method = Method.GET)
  def get(@PathVariable("id") id: Long): Option[TestNotifier]

  @RequestMapping(method = Method.GET)
  def getAll: Seq[TestNotifier]

  @RequestMapping(path = "{id}/subscribe", method = Method.POST)
  def subscribe(@PathVariable("id") id: Long, @RequestBody interest: Interest): Unit

  @RequestMapping(path = "{id}/unsubscribe", method = Method.POST)
  def unsubscribe(@PathVariable("id") id: Long): Unit
end TestNotifierRootApi

final case class TestNotifierInit(@JsonDeserialize(contentAs = classOf[jLong]) parent: Option[Long])

@VisibleForTesting
@Component(enabled = false)
class TestNotifierRootApiImpl(val componentInstance: ComponentInstance)(implicit
  fs: FacadeService,
  gws: GroupWebService,
  qs: QueryService,
  ss: SubscriptionService,
  currentUser: UserDTO,
  currentDomain: DomainDTO,
  cs: ComponentService
) extends TestNotifierRootApi
    with ComponentImplementation:
  override def get(id: Long): Option[TestNotifier] =
    Option(id.component[TestNotifier])

  override def getAll: Seq[TestNotifier] =
    qs.queryParent(testCourse.id, ComponentConstants.ITEM_TYPE_COMPONENT)
      .addCondition(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER, Comparison.eq, classOf[TestNotifierImpl].getName)
      .getComponents[TestNotifier]

  override def create(init: TestNotifierInit): TestNotifier  =
    val f = fs.addFacade(init.parent.getOrElse(testCourse.id), classOf[TestNotifierFacade])
    f.setIdentifier(classOf[TestNotifierImpl].getName)
    f.component[TestNotifier]
  override def subscribe(id: Long, interest: Interest): Unit =
    get(id) foreach { notif =>
      val path =
        if notif.getContext == testCourse.id then SubscriptionPath(notif.getId)
        else SubscriptionPath(notif.getContext, notif.getId)
      ss.subscribe(currentUser, testCourse, path, interest)
    }

  override def unsubscribe(id: Long): Unit =
    get(id) foreach { notif =>
      val path =
        if notif.getContext == testCourse.id then SubscriptionPath(notif.getId)
        else SubscriptionPath(notif.getContext, notif.getId)
      ss.unsubscribe(currentUser, testCourse, path)
    }

  private def testCourse: ContextId =
    val group = Option(gws.getGroupByGroupId(currentDomain.id, CourseIdValue))
      .getOrElse(gws.addGroup(currentDomain.id))
    group.setGroupId(CourseIdValue)
    ContextId(group.getId)

  private final val CourseIdValue = "testNotif"
end TestNotifierRootApiImpl

/** A test notification.
  */
@VisibleForTesting
@Schema(TestNotification.Schema)
trait TestNotification extends Notification:
  @JsonProperty
  def getData: TestNotificationData

object TestNotification:
  final val Schema = "testNotification"

@VisibleForTesting
@Component(enabled = false)
class TestNotificationImpl(
  val componentInstance: ComponentInstance,
  protected val self: NotificationFacade,
  implicit val fs: FacadeService
) extends TestNotification
    with NotificationImplementation
    with ComponentImplementation:
  @PostCreate
  private def initialize(init: TestNotificationData): Unit =
    self.setTime(init.time)
    self.setSender(init.sender)
    self.setContext(init.context)
    self.setTopic(init.topic)
    self.setData(init)

  override lazy val getData: TestNotificationData =
    self.getData[TestNotificationData]

  override def audience: Seq[Long] = getData.audience

  override def interest: Interest = getData.interest

  override def subscriptionPath: Option[SubscriptionPath] = getData.topic.map(topic => SubscriptionPath(topic))

  override def aggregationKey: Option[String] = getData.topic.map(topic => s"$schemaName:$topic")
end TestNotificationImpl
