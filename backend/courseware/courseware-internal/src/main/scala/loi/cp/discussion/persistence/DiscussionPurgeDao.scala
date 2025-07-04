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
import com.learningobjects.cpxp.component.discussion.PostEntity
import com.learningobjects.cpxp.util.ThreadTerminator
import org.hibernate.Session
import org.hibernate.query.NativeQuery

import java.util.Date
import scala.jdk.CollectionConverters.*

@Service
class DiscussionPurgeDao(
  session: => Session
):

  def purgeablePostIds(sectionId: Long, purgeBefore: Date, timezoneOffset: String): Map[String, List[Long]] =
    ThreadTerminator.check()
    val query = session
      .createNativeQuery(s"""
           |SELECT p.edgepath, p.id
           |      FROM discussionpost p
           |      WHERE p.contextid = :sectionId
           |      AND date_trunc('day', descendantactivity AT TIME ZONE :timezoneOffset) <= :purgeBefore
           |      AND pinnedon IS NULL
           |      AND purged IS NULL
         """.stripMargin)
      .setParameter("sectionId", sectionId)
      .setParameter("timezoneOffset", timezoneOffset)
      .setParameter("purgeBefore", purgeBefore)

    query.getResultList
      .asInstanceOf[java.util.List[Array[Object]]]
      .asScala
      .toList
      .map(row =>
        val path = row(0).asInstanceOf[String]
        val id   = row(1).asInstanceOf[Number]
        (path, id.longValue())
      )
      .groupMap(e => e._1)(e => e._2)
  end purgeablePostIds

  def purgePostIds(postIds: List[Long], delGuid: String): Int =
    ThreadTerminator.check()
    val query = session
      .createNativeQuery(s"""
         |UPDATE discussionpost
         |SET purged = :delGuid
         |WHERE id = ANY (cast (:postIds AS BIGINT[]))
         """.stripMargin)
      .unwrap(classOf[NativeQuery[PostEntity]])
      .addSynchronizedEntityClass(classOf[PostEntity])
      .setParameter("delGuid", delGuid)
      .setParameter("postIds", s"{${postIds.mkString(",")}}")

    query.executeUpdate()
  end purgePostIds
end DiscussionPurgeDao
