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

package loi.authoring.project

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import loi.authoring.CommitId
import org.hibernate.graph.RootGraph
import org.hibernate.query.NativeQuery
import org.hibernate.{CacheMode, Session}
import scaloi.syntax.boxes.*

import java.util.UUID
import scala.jdk.CollectionConverters.*

@Service
class CommitDao2(
  session: => Session
):

  def loadEntity(id: Long): Option[CommitEntity2] = Option(session.find(classOf[CommitEntity2], id))
  def load(id: Long): Option[Commit2]             = loadEntity(id).map(_.toCommit)
  def ref(commit: Commit2): CommitEntity2         = session.ref[CommitEntity2](commit.id)

  def loadWithInitializedDocs(commit: Commit2): CommitEntity2 = loadWithInitializedDocs(commit.id).get

  def loadWithInitializedDocs(commitId: CommitId): Option[CommitEntity2] =
    loadWithInitializedDocs(Seq(commitId)).get(commitId)

  // load commits with kfDoc and driftDocs fully initialized
  def loadWithInitializedDocs(ids: Iterable[Long]): Map[CommitId, CommitEntity2] =

    val rootGraph = session.getEntityGraph("commit.docs").asInstanceOf[RootGraph[CommitEntity2]]

    session
      .byMultipleIds(classOf[CommitEntity2])
      .enableSessionCheck(true)
      .enableOrderedReturn(false)
      .`with`(CacheMode.NORMAL)
      .withLoadGraph(rootGraph)
      .multiLoad(ids.boxInsideTo[java.util.List]())
      .asScala
      .view
      .map(c => c.id.toLong -> c)
      .toMap
  end loadWithInitializedDocs

  def loadBigCommit(id: Long): Option[BigCommit] = loadWithInitializedDocs(id).map(_.toBigCommit)

  def loadAncestor(commitId: Long, ancestorId: Long): Option[CommitEntity2] =
    session
      .createNativeQuery[CommitEntity2](
        """WITH RECURSIVE ancestor AS (
          |  SELECT * FROM authoringcommit WHERE id = :commitId
          |UNION ALL
          |  SELECT parent.*
          |  FROM authoringcommit parent
          |  JOIN ancestor ON parent.id = ancestor.parent_id
          |)
          |SELECT * FROM ancestor
          |WHERE id = :ancestorId""".stripMargin,
        classOf[CommitEntity2]
      )
      .unwrap(classOf[NativeQuery[CommitEntity2]])
      .addSynchronizedEntityClass(classOf[CommitEntity2])
      .setParameter("commitId", commitId)
      .setParameter("ancestorId", ancestorId)
      .getResultList
      .asScala
      .headOption

  def loadAncestors(commitId: Long, limit: Int): List[CommitEntity2] =
    session
      .createNativeQuery[CommitEntity2](
        """WITH RECURSIVE ancestor AS (
          |  SELECT * FROM authoringcommit WHERE id = :commitId
          |UNION ALL
          |  SELECT parent.*
          |  FROM authoringcommit parent
          |  JOIN ancestor ON parent.id = ancestor.parent_id
          |)
          |SELECT * FROM ancestor
          |LIMIT :limit""".stripMargin,
        classOf[CommitEntity2]
      )
      .unwrap(classOf[NativeQuery[CommitEntity2]])
      .addSynchronizedEntityClass(classOf[CommitEntity2])
      .setParameter("commitId", commitId)
      .setParameter("limit", limit)
      .getResultList
      .asScala
      .toList

  def loadAncestorsUntilCommit(commitId: Long, untilId: Long, limit: Int): List[CommitEntity2] =
    session
      .createNativeQuery[CommitEntity2](
        """WITH RECURSIVE ancestor AS (
          |  SELECT * FROM authoringcommit WHERE id = :commitId
          |UNION ALL
          |  SELECT parent.*
          |  FROM authoringcommit parent
          |  JOIN ancestor ON parent.id = ancestor.parent_id AND parent.id != :untilId
          |)
          |SELECT * FROM ancestor
          |LIMIT :limit""".stripMargin,
        classOf[CommitEntity2]
      )
      .unwrap(classOf[NativeQuery[CommitEntity2]])
      .addSynchronizedEntityClass(classOf[CommitEntity2])
      .setParameter("commitId", commitId)
      .setParameter("untilId", untilId)
      .setParameter("limit", limit)
      .getResultList
      .asScala
      .toList

  def loadAncestorsAffectingName(commitId: Long, name: UUID): List[CommitEntity2] =

    val vars = JsonNodeFactory.instance.objectNode().put("tgt", name.toString)

    session
      .createNativeQuery[CommitEntity2](
        """WITH RECURSIVE ancestor AS (
          |  SELECT * FROM authoringcommit WHERE id = :commitId
          |UNION ALL
          |  SELECT parent.*
          |  FROM authoringcommit parent
          |  JOIN ancestor ON parent.id = ancestor.parent_id
          |)
          |SELECT * FROM ancestor
          |WHERE jsonb_path_exists(ops, '$[*].name ? (@ == $tgt)', :vars)
          |   OR jsonb_path_exists(ops, '$[*].sourceName ? (@ == $tgt)', :vars)
          |   OR jsonb_path_exists(ops, '$[*].narrativelyUpdatedNodeNames[*] ? (@ == $tgt)', :vars)
          |""".stripMargin,
        classOf[CommitEntity2]
      )
      .unwrap(classOf[NativeQuery[CommitEntity2]])
      .addSynchronizedEntityClass(classOf[CommitEntity2])
      .setParameter("commitId", commitId)
      .setParameter("vars", vars)
      .getResultList
      .asScala
      .toList
  end loadAncestorsAffectingName
end CommitDao2
