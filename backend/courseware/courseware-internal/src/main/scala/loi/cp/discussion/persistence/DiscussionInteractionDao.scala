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

import java.time.Instant
import java.util.Date

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.discussion.{
  DiscussionInteraction,
  DiscussionInteractionEntity,
  DiscussionInteractionRaw
}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.user.{UserFinder, UserId}
import com.learningobjects.cpxp.util.PersistenceIdFactory
import loi.cp.reference.{ContentIdentifier, EdgePath}
import org.hibernate.{LockMode, Session}
import scaloi.GetOrCreate

import scala.jdk.CollectionConverters.*

@Service
trait DiscussionInteractionDao:

  /** Get the interaction for update purposes.
    */
  def visitDiscussionBoard(discussionId: ContentIdentifier, user: UserId, visit: Instant): DiscussionInteraction

  def getOrCreate(discussionId: ContentIdentifier, user: UserId): DiscussionInteraction

  def read(discussionId: ContentIdentifier, user: UserId): Option[DiscussionInteraction]

  def setEditability(discussionId: ContentIdentifier, user: UserId, canEditOwnPosts: Boolean): DiscussionInteraction

  def userInteractions(
    discussionId: Seq[ContentIdentifier],
    user: UserId
  ): Map[ContentIdentifier, DiscussionInteraction]
end DiscussionInteractionDao

@Service
class DiscussionInteractionDaoImpl(session: => Session, idFactory: PersistenceIdFactory, domain: => DomainDTO)
    extends DiscussionInteractionDao:

  import loi.cp.reference.UserContentIdentifierEntity.*

  override def visitDiscussionBoard(
    discussionId: ContentIdentifier,
    user: UserId,
    visit: Instant
  ): DiscussionInteraction =
    val interaction = goc(discussionId, user)
    interaction.visited = Date.from(visit)
    session.merge(interaction)
    DiscussionInteraction(interaction)

  override def getOrCreate(discussionId: ContentIdentifier, user: UserId): DiscussionInteraction =
    DiscussionInteraction(goc(discussionId, user))

  def read(discussionId: ContentIdentifier, user: UserId): Option[DiscussionInteraction] =
    queryInteractions(discussionId, user).map(DiscussionInteraction.apply)

  override def setEditability(
    discussionId: ContentIdentifier,
    user: UserId,
    canEditOwnPosts: Boolean
  ): DiscussionInteraction =

    val interaction = goc(discussionId, user)
    interaction.canEditOwnPosts = canEditOwnPosts.booleanValue()
    session.merge(interaction)
    DiscussionInteraction(interaction)
  end setEditability

  private def queryInteractions(discussionId: ContentIdentifier, user: UserId): Option[DiscussionInteractionEntity] =
    val query = session
      .createQuery(
        """from DiscussionInteractionEntity
          | where edgePath = :edgePath
          | AND contextId = :contextId
          | AND userId = :userId""".stripMargin,
        classOf[DiscussionInteractionEntity]
      )
      .setParameter("edgePath", discussionId.edgePath.toString)
      .setParameter("contextId", discussionId.contextId.value)
      .setParameter("userId", user.id)

    val maybeInteractions = query.getResultList.asScala
    maybeInteractions.headOption
  end queryInteractions

  private def createInteraction(
    discussionId: ContentIdentifier,
    user: UserId,
    domain: DomainDTO,
    visit: Instant = Instant.EPOCH
  ): DiscussionInteractionEntity =
    val domainItem: Item                                         = session.getReference(classOf[Item], domain.id)
    val discussionInteractionEntity: DiscussionInteractionEntity =
      DiscussionInteractionEntity(idFactory.generateId(), domainItem, discussionId, user, visit)

    session.persist(discussionInteractionEntity)
    discussionInteractionEntity
  end createInteraction

  private def lockOnUser(user: UserId): Unit =
    val userEntity: UserFinder = session.getReference(classOf[UserFinder], user.id)
    session.lock(userEntity, LockMode.PESSIMISTIC_WRITE)

  override def userInteractions(
    discussionIds: Seq[ContentIdentifier],
    user: UserId
  ): Map[ContentIdentifier, DiscussionInteraction] =
    val pathReferencesByContext: Map[Long, Seq[EdgePath]] = explodeContentIdentifiers(discussionIds)
    session
      .createQuery(
        interactionValueQuery +
          " WHERE " +
          contentIdCondition(pathReferencesByContext).toSeq.mkString(" AND ") +
          " AND userId = :userId",
        classOf[DiscussionInteractionRaw]
      )
      .setContentIdParameters(pathReferencesByContext)
      .setParameter("userId", user.id.longValue())
      .getResultList
      .asScala
      .map(raw => raw.contentId -> raw)
      .toMap
  end userInteractions

  private val interactionValueQuery: String =
    s"""select new ${classOf[DiscussionInteractionRaw]}(
       | e.userId,
       | e.edgePath,
       | e.contextId,
       | e.visited,
       | e.canEditOwnPosts
       | ) from ${classOf[DiscussionInteractionEntity].getSimpleName} e
     """.stripMargin.replaceAll("\n", " ")

  private def goc(discussionId: ContentIdentifier, user: UserId): DiscussionInteractionEntity =
    GetOrCreate[DiscussionInteractionEntity](
      queryEntity = () => queryInteractions(discussionId, user),
      createEntity = () => createInteraction(discussionId, user, domain),
      lockCollection = () => lockOnUser(user)
    ).result
end DiscussionInteractionDaoImpl
