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

package loi.cp.message

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.messaging.MessageConstants.*
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.query.ApiQueries.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import scaloi.syntax.AnyOps.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.{BaseCondition, QueryBuilder, QueryService}

import scala.jdk.CollectionConverters.*
import scalaz.syntax.std.`boolean`.*

@Component
class MessageRootApiImpl(
  val componentInstance: ComponentInstance,
  implicit val fs: FacadeService,
  implicit val qs: QueryService,
  val ms: MessageService
) extends MessageRootApi
    with ComponentImplementation:

  import MessageRootApiImpl.*

  /** Query your messages. */
  override def get(q: ApiQuery): ApiQueryResults[Message] =
    queryMessages(q) <| preloadMessageStorage // TODO: other preloads

  /** Get a single message. */
  override def get(id: Long): Option[Message] =
    queryMessages(ApiQuery.byId(id)).asOption

  /** Count your messages. */
  override def count(q: ApiQuery): Long =
    queryMessages(
      new ApiQuery.Builder(q.toCountQuery).addPropertyMappings(classOf[Message]).build
    ).getTotalCount.longValue

  /** Post a new message. */
  override def send(message: NewMessage): Message =
    ms.sendMessage(message, None, emailCopy = true)

  /** Reply to a new message. */
  override def reply(id: Long, message: NewMessage): Message =
    Current.getUser.facade[MessageParentFacade].getMessage(id) match
      case Some(parent) => ms.sendMessage(message, Some(parent.getStorage.getId), emailCopy = true)
      case None         => throw new ResourceNotFoundException(s"Unknown message: $id")

  private def queryMessages(q: ApiQuery): ApiQueryResults[Message] =
    ApiQuerySupport.query(getQueryBuilder(q), q, classOf[Message], filterQueryBuilder)
end MessageRootApiImpl

object MessageRootApiImpl:
  // querying for messages is complicated by the fact that internally the message
  // is split across two tables but this is not exposed in the Web API, so we have
  // to partition filtering and ordering among the main query builder and the join
  // query builder based upon the property in question.

  // query for the current user's messages and apply prefilters
  def getQueryBuilder(aq: ApiQuery)(implicit fs: FacadeService): QueryBuilder =
    myMessageParent.queryMessages <| { qb =>
      // This is annoying but necessary b/c we're prefiltering ourselves.
      val dmaq = aq.withDataModel[Message]
      dmaq.getPrefilters.asScala foreach applyFilter(qb, dmaq)
    }

  // apply the paging, ordering and filtering from an api query to a query builder
  def filterQueryBuilder(qb: QueryBuilder, aq: ApiQuery): QueryBuilder =
    // see above whinging
    val dmaq = aq.withDataModel[Message]
    // apply the sequence of filters to the query builder
    dmaq.getFilters.asScala foreach applyFilter(qb, dmaq)
    // apply the sequence of orders to the query builder
    dmaq.getOrders.asScala foreach applyOrder(qb, dmaq)
    // apply the page to the query builder
    applyPage(qb, dmaq.getPage)

  // properties to apply to the main query builder
  val DirectProperties = Set("id", "label", "read", "messageId")

  // apply a filter to a query builder
  def applyFilter(qb: QueryBuilder, aq: ApiQuery)(af: ApiFilter): QueryBuilder =
    // get the query builder condition
    val options   = aq.getRequiredPropertyMapping(af.getProperty)
    val condition = ApiQuerySupport.getApplyFilterCondition(options, af)
    // apply it
    getPropertyQueryBuilder(qb, af.getProperty).addCondition(condition)

  // apply an order to a query builder
  def applyOrder(qb: QueryBuilder, aq: ApiQuery)(ao: ApiOrder): QueryBuilder =
    // get the query builder order
    val options = aq.getRequiredPropertyMapping(ao.getProperty)
    val order   = ApiQuerySupport.getApplyOrder(options, ao)
    // apply it
    qb.addJoinOrder(getPropertyQueryBuilder(qb, ao.getProperty), order)

  // apply a page to a query builder
  private def applyPage(qb: QueryBuilder, ap: ApiPage): QueryBuilder =
    if ap.isSet then qb.setFirstResult(ap.getOffset).setLimit(ap.getLimit)
    qb

  // get the query builder or join query builder against which to apply a property
  def getPropertyQueryBuilder(qb: QueryBuilder, property: String): QueryBuilder =
    DirectProperties
      .contains(property)
      .fold(qb, qb.getOrCreateJoinQuery(DATA_TYPE_MESSAGE_STORAGE, ITEM_TYPE_MESSAGE_STORAGE))

  // preload message storages
  def preloadMessageStorage(messages: ApiQueryResults[Message])(implicit qs: QueryService): Unit =
    qs.queryAllDomains(ITEM_TYPE_MESSAGE_STORAGE)
      .setLogQuery(true)
      .addInitialQuery(queryStorageIds(messages))
      .getItems() // load and discard
    ()

  // get all the storage ids. I cannot just messages.map(_.getStorageId) because that would
  // trigger 1 by 1 loading to check the del flags. TODO: Some mechanism to bypass that
  def queryStorageIds(messages: ApiQueryResults[Message])(implicit qs: QueryService): QueryBuilder =
    qs.queryAllDomains(ITEM_TYPE_MESSAGE)
      .addCondition(BaseCondition.inIterable(DataTypes.META_DATA_TYPE_ID, messages))
      .setDataProjection(DATA_TYPE_MESSAGE_STORAGE)

  private def myMessageParent(implicit fs: FacadeService): MessageParentFacade =
    Current.getUser.facade[MessageParentFacade]

  /** The component configuration for the message root */
  case class Config(
    /** Should sending a new message notify the recipients? */
    sendMessageNotifications: Boolean = false
  )
end MessageRootApiImpl
