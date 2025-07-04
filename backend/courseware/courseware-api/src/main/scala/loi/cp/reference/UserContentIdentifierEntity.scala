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

package loi.cp.reference

import com.learningobjects.cpxp.entity.annotation.DataType
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.cpxp.service.finder.Finder
import com.learningobjects.cpxp.service.user.UserId
import jakarta.persistence.Column
import loi.cp.context.ContextId
import org.hibernate.query.Query

import scala.jdk.CollectionConverters.*

trait UserContentIdentifierEntity:
  self: Finder =>

  /** the column containing the reference to a context */
  @DataType(AssetReference.DATA_TYPE_CONTEXT_ID)
  var contextId: JLong = scala.compiletime.uninitialized

  /** the column containing the reference to the location of the content in the context */
  @DataType(AssetReference.DATA_TYPE_EDGE_PATH)
  var edgePath: String = scala.compiletime.uninitialized

  /** a column containing the id of the subject (the driver of the attempt may be a different user) */
  @Column(nullable = false)
  var userId: JLong = scala.compiletime.uninitialized

  def contentIdentifier: ContentIdentifier = ContentIdentifier(ContextId(contextId), EdgePath.parse(edgePath))
end UserContentIdentifierEntity

object UserContentIdentifierEntity:

  def explodeContentIdentifiers(ids: Seq[ContentIdentifier]): Map[Long, Seq[EdgePath]] =
    ids
      .map(id => id.contextId -> id.edgePath)
      .groupBy(_._1.value)
      .transform((_, tuples) => tuples.map(_._2))

  def contentIdCondition(pathReferencesByContext: Map[Long, Seq[EdgePath]]): Option[String] =
    if pathReferencesByContext.nonEmpty then
      val contextAndReferenceConditions: Seq[String] =
        for index <- pathReferencesByContext.toSeq.indices
        yield s"(${AssetReference.DATA_TYPE_CONTEXT_ID} = :contextId$index AND ${AssetReference.DATA_TYPE_EDGE_PATH} in (:pathReferences$index))"

      // Any of the pairs of contextId and assetPath in that context can match to fulfill this condition
      Some("(" + contextAndReferenceConditions.mkString(" OR ") + ")")
    else None

  def userIdCondition(userIds: Seq[UserId]): Option[String] =
    if userIds.nonEmpty then Some("(userId in (:userIds))")
    else None

  implicit class UserInteractionQueryOps[T](query: Query[T]):
    def setContentIdParameters(pathReferencesByContext: Map[Long, Seq[EdgePath]]): Query[T] =
      contentIdCondition(pathReferencesByContext).foreach { _ =>
        val entries: Seq[(Long, Seq[EdgePath])] = pathReferencesByContext.toSeq
        for ((contextId, paths), index) <- entries.zipWithIndex
        do
          query.setParameter(s"contextId$index", contextId)
          query.setParameter(s"pathReferences$index", paths.map(_.toString).asJava)
      }

      query
    end setContentIdParameters

    def setUserIdParameters(users: Seq[UserId]): Query[T] =
      userIdCondition(users).foreach { _ =>
        query.setParameter("userIds", users.map(u => u.value.longValue()).asJava)
      }

      query
  end UserInteractionQueryOps
end UserContentIdentifierEntity
