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
import com.learningobjects.cpxp.component.discussion.{
  PostInteractionEntity,
  PostInteractionValue,
  PostInteractionValueRaw
}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.user.{UserFinder, UserId}
import com.learningobjects.cpxp.util.{PersistenceIdFactory, ThreadTerminator}
import loi.cp.discussion.PostId
import org.hibernate.{LockMode, Session}
import scaloi.GetOrCreate

import scala.jdk.CollectionConverters.*
import scala.compat.java8.OptionConverters.*

@Service
trait PostInteractionDao:

  /** Get the interaction for update purposes.
    */
  def getOrCreate(user: UserId, postId: PostId): PostInteractionEntity

  /** Write out a modified entity that was previously fetched using #getOrCreate
    */
  def write(postInteractionEntity: PostInteractionEntity): Unit

  /** Get a read only version of the interaction if it exists.
    */
  def read(user: UserId, postId: PostId): Option[PostInteractionValue]

  /** Get the interactions for a bunch of posts for a specific user for read only purposes
    *
    * @param user
    *   whose interactions are important right now
    * @param postIds
    *   for which posts
    * @return
    *   a map that may not contain all the posts originally passed in.
    */
  def userInteractions(
    user: UserId,
    postIds: Seq[PostId]
  ): Map[PostId, PostInteractionValue]
end PostInteractionDao

@Service
class PostInteractionDaoImpl(session: => Session, idFactory: PersistenceIdFactory, domain: => DomainDTO)
    extends PostInteractionDao:

  val userParam: String = "userId"
  val postParam: String = "postId"

  override def getOrCreate(user: UserId, postId: PostId): PostInteractionEntity =
    GetOrCreate[PostInteractionEntity](
      queryEntity = () => queryInteractions(user, postId),
      createEntity = () => createInteraction(user, postId, domain),
      lockCollection = () => lockOnUser(user)
    ).result

  private def queryInteractions(user: UserId, postId: PostId): Option[PostInteractionEntity] =
    session
      .byNaturalId(classOf[PostInteractionEntity])
      .using(userParam, user.id)
      .using(postParam, postId)
      .loadOptional()
      .asScala

  private def createInteraction(user: UserId, postId: PostId, domainDTO: DomainDTO): PostInteractionEntity =
    val domainItem: Item              = session.getReference(classOf[Item], domainDTO.id)
    val entity: PostInteractionEntity = PostInteractionEntity(idFactory.generateId(), postId, user)
    entity.setRoot(domainItem)
    session.persist(entity)
    entity

  private def lockOnUser(user: UserId): Unit =
    val userEntity: UserFinder = session.getReference(classOf[UserFinder], user.id)
    session.lock(userEntity, LockMode.PESSIMISTIC_WRITE)

  override def write(interaction: PostInteractionEntity): Unit =
    ThreadTerminator.check()
    session.merge(interaction)

  override def read(user: UserId, postId: PostId): Option[PostInteractionValue] =
    Option(
      session
        .createQuery(
          interactionValueQuery +
            s" WHERE userId = :$userParam " +
            s" AND postId = :$postParam",
          classOf[PostInteractionValueRaw]
        )
        .setParameter(userParam, Long.box(user.id))
        .setParameter(postParam, Long.box(postId))
        .getSingleResult
    )

  override def userInteractions(
    user: UserId,
    postIds: Seq[PostId]
  ): Map[PostId, PostInteractionValue] =
    if postIds.isEmpty then Map.empty
    else
      val query = session
        .createQuery(
          interactionValueQuery +
            s" WHERE userId = :$userParam " +
            s" AND postId in (:$postParam)",
          classOf[PostInteractionValueRaw]
        )
        .setParameter(userParam, Long.box(user.id))
        .setParameter(postParam, postIds.map(Long.box).asJava)

      query.getResultList.asScala
        .map(p => (p.postId, p))
        .toMap

  private val interactionValueQuery: String =
    s"""select new ${classOf[PostInteractionValueRaw].getName}(
       | e.postId,
       | e.userId,
       | e.favorited,
       | e.bookmarked,
       | e.viewed
       | ) from ${classOf[PostInteractionEntity].getSimpleName} e
     """.stripMargin.replaceAll("\n", " ")
end PostInteractionDaoImpl
