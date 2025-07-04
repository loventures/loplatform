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

package loi.cp.discussion.persistence

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.discussion.{CountDto, PostEntity, PostValue, PostValueRaw}
import com.learningobjects.cpxp.component.query.{ApiOrder, ApiPage, BaseApiPage}
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.user.UserId
import com.learningobjects.cpxp.util.{PersistenceIdFactory, ThreadTerminator}
import loi.cp.attachment.AttachmentId
import loi.cp.discussion.*
import loi.cp.discussion.dto.FilteredSeq
import loi.cp.reference.{AssetReference, ContentIdentifier, EdgePath}
import org.hibernate.query.Query
import org.hibernate.{LockMode, Session}
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*

import java.time.Instant
import java.util.Date
import scala.compat.java8.OptionConverters.*
import scala.jdk.CollectionConverters.*
import scala.reflect.*

@Service
trait PostDao:
  def create(
    discussionId: ContentIdentifier,
    user: UserId,
    isModerator: Boolean,
    parentPost: Option[PostEntity],
    title: Option[String],
    content: String,
    searchableContent: String,
    attachmentIds: Seq[AttachmentId],
    createTime: Instant
  ): PostEntity

  def load(postId: PostId): Option[PostEntity]

  def write(post: PostEntity): Unit

  def updateActivityTime(postIds: Seq[PostId], time: Instant): Unit

  // Functions below are for read-only access.

  def readonlyPost(postId: PostId): Option[PostValue]

  /** General fetch posts. Does not include any derivative statistics.
    *
    * @return
    */
  def readonlyPosts(
    discussionIds: Seq[ContentIdentifier] = Nil,
    userIds: Seq[UserId] = Nil
  ): Seq[PostValue]

  /** @param discussionId
    *   the discussion board in question
    * @param targetUserId
    *   which author
    * @param apiOrders
    *   ordering support
    * @param apiPage
    *   paging support
    * @return
    *   the visible posts for a specific user in a discussion board.
    */
  def readonlyPostsForUser(
    discussionId: ContentIdentifier,
    targetUserId: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue]

  /** @param discussionId
    *   the discussion board in question
    * @param targetUserId
    *   do not include this user in new posts
    * @param since
    *   what time should we consider posts from
    * @param apiOrders
    *   ordering support
    * @param apiPage
    *   paging support
    * @return
    *   Posts that have been created after since or threads that have had a reply to them after since
    */
  def readonlyNewPosts(
    discussionId: ContentIdentifier,
    targetUserId: UserId,
    since: Instant,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue]

  /** Return all the posts that are associated with a particular discussion board pursuant to the set of filters
    * defined.
    *
    * @param discussionId
    *   the discussion board in question
    * @param targetUserId
    *   Who's posts do we need to treat special
    * @param toDepth
    *   Depth is absolute with the root posts all being at depth 0
    * @param commonAncestor
    *   The id of what ancestor to query from. Has no bearing on depth
    * @param includeHidden
    *   Include posts that have been hidden or marked inappropriate in stats counts and listing.
    * @param includeDescendantCount
    *   Include child count statistics with the posts.
    * @param newDescendantCountSince
    *   If provided, we will provide a count of how many descendant posts have been created since the date
    * @param postIds
    *   If provided, will only return posts included in this list of ids
    * @param apiOrders
    *   ordering support.
    * @param apiPage
    *   paging support.
    * @return
    *   posts
    */
  def readonlyPosts(
    discussionId: ContentIdentifier,
    targetUserId: UserId,
    toDepth: Option[Long],
    commonAncestor: Option[Long],
    includeHidden: Boolean,
    includeDescendantCount: Boolean,
    newDescendantCountSince: Option[Instant],
    postIds: Option[Seq[PostId]],
    searchFor: Option[String],
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue]

  /** @param discussionId
    *   the discussion board in question
    * @param commonAncestors
    *   Which threads do we want to fetch posts for
    * @param includeHidden
    *   should removed posts be included?
    * @param apiOrders
    *   ordering support
    * @return
    */
  def readonlyPostsInThread(
    discussionId: ContentIdentifier,
    commonAncestors: Seq[PostId],
    includeHidden: Boolean,
    apiOrders: Seq[ApiOrder]
  ): Map[PostId, FilteredSeq[PostValue]]

  def userPostCounts(discussionId: ContentIdentifier): Seq[CountDto]
end PostDao

@Service
class PostDaoImpl(session: => Session, idFactory: PersistenceIdFactory, domain: => DomainDTO) extends PostDao:
// needs filtering
  import PostDao.*
  import loi.cp.reference.UserContentIdentifierEntity.*

  override def create(
    discussionId: ContentIdentifier,
    user: UserId,
    isModerator: Boolean,
    parentPost: Option[PostEntity],
    title: Option[String],
    content: String,
    searchableContent: String,
    attachmentIds: Seq[AttachmentId],
    createTime: Instant
  ): PostEntity =
    val domainItem: Item       = session.getReference(classOf[Item], domain.id)
    val postEntity: PostEntity =
      PostEntity(
        idFactory.generateId(),
        domainItem,
        discussionId,
        user,
        isModerator,
        parentPost,
        createTime,
        title,
        content,
        searchableContent,
        attachmentIds
      )

    session.persist(postEntity)

    postEntity
  end create

  override def updateActivityTime(postIds: Seq[PostId], time: Instant): Unit =
    // Not locking because it doesn't really matter if something at the same time whomps
    // this.  The times should be close enough that it doesn't matter for our purposes.
    val query =
      session.createMutationQuery(s"update PostEntity set descendantActivity = :$sinceParam where id in :$postIdParam")
    query.setParameter(sinceParam, Date.from(time))
    query.setParameter(postIdParam, postIds.map(long2Long).asJava)
    query.executeUpdate()

  override def load(postId: PostId): Option[PostEntity] =
    ThreadTerminator.check() // TODO: Why?
    Option(session.find(classOf[PostEntity], postId, LockMode.PESSIMISTIC_WRITE))

  override def write(post: PostEntity): Unit =
    ThreadTerminator.check()
    session.merge(post) // ??

  override def readonlyPost(postId: PostId): Option[PostValue] =
    val queryString = postSelectionPrefix + noStatPostfix + fromClause + s" WHERE id = :$postIdParam"
    session
      .createQuery(queryString, classOf[PostValueRaw])
      .setParameter(postIdParam, long2Long(postId))
      .uniqueResultOptional
      .asScala

  override def readonlyPosts(
    discussionIds: Seq[ContentIdentifier],
    users: Seq[UserId]
  ): Seq[PostValue] =

    val pathReferencesByContext: Map[Long, Seq[EdgePath]] = explodeContentIdentifiers(discussionIds)
    val queryString                                       =
      postSelectionPrefix + noStatPostfix + fromClause +
        "WHERE purged IS NULL AND " +
        (contentIdCondition(pathReferencesByContext).toSeq ++ userIdCondition(users).toSeq).mkString(" AND ")

    session
      .createQuery(queryString, classOf[PostValueRaw])
      .setContentIdParameters(pathReferencesByContext)
      .setUserIdParameters(users)
      .getResultList
      .asScala
      .toSeq
  end readonlyPosts

  override def readonlyPosts(
    discussionId: ContentIdentifier,
    targetUserId: UserId,
    toDepth: Option[Long],
    commonAncestorId: Option[Long],
    includeHidden: Boolean,
    includeDescendantCount: Boolean,
    newDescendantCountSince: Option[Instant],
    postIds: Option[Seq[PostId]],
    searchFor: Option[String],
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue] =

    val commonAncestor = commonAncestorId.map(load(_).get)

    def constructQuery[A: ClassTag](resultPart: String, orderPart: String, isPostQuery: Boolean): Query[A] =
      val queryString =
        resultPart +
          s"WHERE purged IS NULL" +
          s" AND ${AssetReference.DATA_TYPE_CONTEXT_ID} = :$contextIdParam " +
          s" AND ${AssetReference.DATA_TYPE_EDGE_PATH} = :$edgePathParam " +
          (!includeHidden ?? s" AND (removed is null OR removed = false OR userId = :$userParam) ") +
          (!includeHidden ?? s" AND (inappropriate is null OR inappropriate = false OR userId = :$userParam) ") +
          toDepth.fold("")(_ => s" AND depth <= :$depthParam ") +
          commonAncestor.fold("")(_ => s" AND post.threadId = :$threadIdParam") +
          commonAncestor.fold("")(_ => s" AND post.postPath LIKE :$parentPostParam ") +
          postIds.fold("")(_ => s" AND post.id in :$postIdParam") +
          searchFor.fold("")(_ =>
            s" AND CAST (fts(post.searchableContent, :$searchableContentParam) AS BOOLEAN) = true"
          ) +
          orderPart

      val query = session
        .createQuery(queryString, classTag[A].runtimeClass.asInstanceOf[Class[A]])
        .setParameter(contextIdParam, long2Long(discussionId.contextId.value))
        .setParameter(edgePathParam, discussionId.edgePath.toString)

      if !includeHidden || (newDescendantCountSince.isDefined && isPostQuery) then
        query.setParameter(userParam, long2Long(targetUserId.id))
      toDepth.foreach(d => query.setParameter(depthParam, long2Long(d)))

      commonAncestor.foreach(ancestor =>
        query.setParameter(parentPostParam, ancestor.postPath + "_%")
        query.setParameter(threadIdParam, ancestor.threadId)
      )

      searchFor.foreach(searchText => query.setParameter(searchableContentParam, searchText))

      postIds.foreach(pids => query.setParameter(postIdParam, pids.map(_.longValue()).asJava))

      if newDescendantCountSince.isDefined && isPostQuery then
        query.setParameter(sinceParam, Date.from(newDescendantCountSince.get))

      query
    end constructQuery

    val postfix   = constructSelectionPostfix(includeHidden, includeDescendantCount, newDescendantCountSince)
    val orderPart = constructOrderPart(apiOrders)

    val postQuery  = constructQuery[PostValueRaw](postSelectionPrefix + postfix, orderPart, isPostQuery = true)
    val countQuery = constructQuery[JLong](totalCountQuery, orderPart = "", isPostQuery = false)

    if apiPage.isSet then
      postQuery
        .setMaxResults(apiPage.getLimit)
        .setFirstResult(apiPage.getOffset)

    FilteredSeq[PostValue](
      countQuery.getSingleResult.longValue,
      postQuery.getFirstResult,
      postQuery.getResultList.asScala.toSeq
    )
  end readonlyPosts

  private case class Restriction(clause: String, params: Map[String, Any])

  private def readonlyPostsWithRestriction(
    discussionId: ContentIdentifier,
    restriction: Restriction,
    excludeHidden: Boolean,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue] =

    def constructQuery[A: ClassTag](resultPart: String, orderClause: String): Query[A] =
      val queryString =
        resultPart +
          s"WHERE purged IS NULL" +
          s" AND ${AssetReference.DATA_TYPE_CONTEXT_ID} = :$contextIdParam " +
          s" AND ${AssetReference.DATA_TYPE_EDGE_PATH} = :$edgePathParam " +
          restriction.clause +
          excludeHidden.option(s" AND $notHiddenClause").getOrElse("") +
          orderClause

      val query = session
        .createQuery(queryString, classTag[A].runtimeClass.asInstanceOf[Class[A]])
        .setParameter(contextIdParam, discussionId.contextId.value.longValue())
        .setParameter(edgePathParam, discussionId.edgePath.toString)

      restriction.params.foreach { case (key, value) => query.setParameter(key, value) }
      query
    end constructQuery

    val orderPart                      = constructOrderPart(apiOrders)
    val postQuery: Query[PostValueRaw] =
      constructQuery[PostValueRaw](postSelectionPrefix + noStatPostfix + fromClause, orderPart)
    val countQuery: Query[JLong]       = constructQuery[JLong](totalCountQuery, orderClause = "")

    if apiPage.isSet then
      postQuery
        .setMaxResults(apiPage.getLimit)
        .setFirstResult(apiPage.getOffset)

    FilteredSeq[PostValue](
      countQuery.getSingleResult.longValue,
      postQuery.getFirstResult,
      postQuery.getResultList.asScala.toSeq
    )
  end readonlyPostsWithRestriction

  override def readonlyPostsForUser(
    discussionId: ContentIdentifier,
    targetUserId: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue] =
    val restriction = Restriction(
      s" AND post.userId = :$userParam AND (post.inappropriate is null or post.inappropriate = false)",
      Map(userParam -> targetUserId.id.longValue())
    )
    readonlyPostsWithRestriction(discussionId, restriction, excludeHidden = true, apiOrders, apiPage)
  end readonlyPostsForUser

  override def readonlyNewPosts(
    discussionId: ContentIdentifier,
    targetUserId: UserId,
    since: Instant,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue] =

    val restriction = Restriction(
      s"AND ((post.created >= :$sinceParam " +
        s"     AND post.userId != :$userParam)" +
        s"  OR (post.depth = 0 " +
        s"     AND post.descendantActivity >= :$sinceParam)" +
        s" ) " +
        s" AND $notInappropriateClause",
      Map(sinceParam -> Date.from(since), userParam -> Long2long(targetUserId.id))
    )
    readonlyPostsWithRestriction(discussionId, restriction, excludeHidden = true, apiOrders, apiPage)
  end readonlyNewPosts

  def readonlyPostsInThread(
    discussionId: ContentIdentifier,
    threadIds: Seq[PostId],
    includeHidden: Boolean,
    apiOrders: Seq[ApiOrder]
  ): Map[PostId, FilteredSeq[PostValue]] =

    val restriction =
      Restriction(
        s"AND p.threadId in :$threadIdParam ",
        Map(threadIdParam -> threadIds.map(long2Long).asJavaCollection)
      )
    val posts       = readonlyPostsWithRestriction(discussionId, restriction, !includeHidden, apiOrders, BaseApiPage.ALL)
    posts.partialResults
      .groupBy(pvr => pvr.threadId)
      .view
      .mapValues(pvrList => FilteredSeq[PostValue](pvrList.size, 0, pvrList))
      .toMap
  end readonlyPostsInThread

  def userPostCounts(discussionId: ContentIdentifier): Seq[CountDto] =

    session
      .createQuery(
        s""" select new ${classOf[CountDto].getName}(post.userId, count(post.id))
           | from ${classOf[PostEntity].getSimpleName} post
           | WHERE post.purged IS NULL
           |   AND ${AssetReference.DATA_TYPE_CONTEXT_ID} = :$contextIdParam
           |   AND ${AssetReference.DATA_TYPE_EDGE_PATH} = :$edgePathParam
           |   AND $notHiddenClause
           |   AND (post.inappropriate is null or post.inappropriate = false)
           | group by post.userId
         """.stripMargin,
        classOf[CountDto]
      )
      .setParameter(contextIdParam, Long2long(discussionId.contextId.id))
      .setParameter(edgePathParam, discussionId.edgePath.toString)
      .getResultList
      .asScala
      .toSeq
end PostDaoImpl

object PostDao:

  val userParam: String              = "userId"
  val depthParam: String             = "depth"
  val edgePathParam: String          = "edgePath"
  val contextIdParam: String         = "contextId"
  val threadIdParam: String          = "threadId"
  val postIdParam: String            = "postId"
  val sinceParam: String             = "since"
  val parentPostParam: String        = "parentPostParam"
  val searchableContentParam: String = "searchable"

  val postSelectionPrefix: String =
    s"""select new ${classOf[PostValueRaw].getName}(
       | post.id,
       | post.contextId,
       | post.edgePath,
       | post.parentPostId,
       | post.threadId,
       | post.postPath,
       | post.userId,
       | post.title,
       | post.content,
       | post.moderatorPost,
       | post.inappropriate,
       | post.removed,
       | post.pinnedOn,
       | post.created,
       | post.updated,
       | post.descendantActivity,
       | post.depth,
       | post.attachmentIds,
       | """.stripMargin.replaceAll("\n", " ")

  // Note the threadId is to use the threadId index which precludes the need for edgepath or context in these subqueries
  val countReplies: String =
    s"select count(*) from ${classOf[PostEntity].getSimpleName} reply where reply.purged is null" +
      s" and reply.threadId = post.threadId and reply.postPath like concat(post.postPath, '_%')"

  val countNewReplies: String =
    s"$countReplies and reply.updated >= :$sinceParam and reply.userId != :$userParam"

  val notHiddenReplyClause: String =
    "(reply.removed is null or reply.removed = false)"

  val notInappropriateReplyClause: String =
    s"(reply.inappropriate is null or reply.inappropriate = false or reply.userId = :$userParam)"

  val notHiddenClause: String =
    "(post.removed is null or post.removed = false)"

  val notInappropriateClause: String =
    s"(post.inappropriate is null or post.inappropriate = false or post.userId = :$userParam)"

  val noStat                = " cast(null as long) "
  val noStatPostfix: String = s" $noStat, $noStat "

  val fromClause: String =
    s") from ${classOf[PostEntity].getSimpleName} post "

  /** this does not need a where clause containing 'purged IS NULL' because it is only ever used as a parameter of
    * constructQuery, a function that already provides a WHERE clause with the purged check in it
    */
  val totalCountQuery: String =
    s"select count(post) from ${classOf[PostEntity].getSimpleName} post "

  def constructSelectionPostfix(
    includeHidden: Boolean,
    includeStats: Boolean,
    newChildCountSinceTime: Option[Instant]
  ): String =
    def maybeWithHidden(hidden: Boolean, clausePrefix: String): String =
      if hidden then s"($clausePrefix)"
      else s"($clausePrefix and $notHiddenReplyClause)"

    val mayCount    = if includeStats then maybeWithHidden(includeHidden, countReplies) else noStat
    val mayCountNew =
      if newChildCountSinceTime.isDefined then maybeWithHidden(includeHidden, countNewReplies) else noStat

    s"$mayCount, $mayCountNew $fromClause"
  end constructSelectionPostfix

  implicit class PostDaoQueryOps[T](query: Query[T]):
    def setPathConditions(parentPosts: Seq[PostId]): Query[T] =
      parentPosts.zipWithIndex.foreach { case (postId, index) =>
        query.setParameter(s"parentPost$index", postId)
      }
      query

  private lazy val validOrderByProperties: Set[String] =
    classOf[PostEntity].getDeclaredFields.toSeq.map(field => field.getName).toSet

  def constructOrderPart(apiOrders: Seq[ApiOrder]): String =
    // Make sure given order is a valid property of PostEntity
    val validOrderings = apiOrders
      .filter(order => validOrderByProperties.contains(order.getProperty))

    if validOrderings.nonEmpty then
      " ORDER BY " + validOrderings
        .map(order => s"post.${order.getProperty} ${order.getQbDirection}")
        .mkString(", ")
    else ""
  end constructOrderPart
end PostDao
