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

package loi.authoring.edge.store

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainFinder}
import loi.authoring.branch.Branch
import loi.authoring.edge.{DurableEdge, Group}
import loi.authoring.project.ProjectEntity2
import loi.authoring.write.LayeredWriteService.{ClaimEdge, InsertEdge}
import org.hibernate.Session
import org.hibernate.query.NativeQuery
import scaloi.syntax.any.*

import java.util.UUID
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@Service
class DurableEdgeDao2(
  domainDto: => DomainDTO,
  session: => Session
):

  def create(insert: InsertEdge, project: ProjectEntity2, root: DomainFinder): DurableEdgeEntity2 =
    new DurableEdgeEntity2(
      null,
      insert.srcName,
      insert.tgtName,
      insert.grp.entryName,
      insert.name,
      project,
      root
    ).tap(session.persist)

  def create(claim: ClaimEdge, project: ProjectEntity2, root: DomainFinder): DurableEdgeEntity2 =
    new DurableEdgeEntity2(
      null,
      claim.edge.srcName,
      claim.edge.tgtName,
      claim.edge.grp.entryName,
      claim.edge.name,
      project,
      root
    ).tap(session.persist)

  def create(origin: DurableEdgeEntity2, project: ProjectEntity2): DurableEdgeEntity2 =
    new DurableEdgeEntity2(
      null,
      origin.sourceName,
      origin.targetName,
      origin.group,
      origin.name,
      project,
      origin.root
    ).tap(session.persist)

  def selectNames(keys: Iterable[DurableEdge.Key], projectId: Long): Map[DurableEdge.Key, UUID] =
    val srcNames = mutable.ListBuffer.empty[String]
    val tgtNames = mutable.ListBuffer.empty[String]
    val groups   = mutable.ListBuffer.empty[String]

    for DurableEdge.Key(sourceName, targetName, group) <- keys do
      srcNames.append(sourceName.toString)
      tgtNames.append(targetName.toString)
      groups.append(group.entryName)

    // I'm stumped how to do this in a JPQL query.
    // `unnest` is not supported by the Hibernate Postgres Dialect.
    // Jpa 2.1 added the `function` function (section 4.6.17.3 of jpa 2.1 spec) i.e. `SELECT function('abs', -1)`
    // but I can't figure out how to pass an array to the `function` function.
    // `SELECT CAST('{1, 2, 3}' AS integer[])` does not JPQL parse, the opening square bracket is unexpected.
    session
      .createNativeQuery("""WITH input(sourcename, targetname, "group") AS (
          |  SELECT * FROM unnest(
          |    CAST(:srcNames as UUID[]),
          |    CAST(:tgtNames AS UUID[]),
          |    CAST(:groups AS TEXT[])
          |  )
          |)
          |SELECT DISTINCT de.sourcename, de.targetname, de."group", de.name
          |FROM authoringdurableedge de
          |JOIN input USING (sourcename, targetname, "group")
          |WHERE de.project_id = :projectId
          |  AND de.root_id = :rootId
          |""".stripMargin)
      .setParameter("srcNames", srcNames.mkString("{", ",", "}"))
      .setParameter("tgtNames", tgtNames.mkString("{", ",", "}"))
      .setParameter("groups", groups.mkString("{", ",", "}"))
      .setParameter("projectId", projectId)
      .setParameter("rootId", domainDto.id)
      .unwrap(classOf[NativeQuery[?]])
      .addSynchronizedEntityClass(classOf[DurableEdgeEntity2])
      .getResultList
      .asInstanceOf[java.util.List[Array[Object]]]
      .asScala
      .map(row =>
        (
          DurableEdge.Key(
            row(0).asInstanceOf[UUID],
            row(1).asInstanceOf[UUID],
            Group.withName(row(2).asInstanceOf[String]),
          ),
          row(3).asInstanceOf[UUID]
        )
      )
      .toMap
  end selectNames

  def selectAll(projectId: Long): List[DurableEdgeEntity2] =
    session
      .createQuery(
        "FROM DurableEdgeEntity2 d WHERE d.project.id = :projectId AND d.root.id = :rootId",
        classOf[DurableEdgeEntity2]
      )
      .setParameter("projectId", projectId)
      .setParameter("rootId", domainDto.id)
      .getResultList
      .asScala
      .toList
end DurableEdgeDao2

object DurableEdgeDao2:
  def entityToEdge(branch: Branch)(entity: DurableEdgeEntity2): DurableEdge = DurableEdge(
    entity.sourceName,
    entity.targetName,
    Group.withName(entity.group),
    entity.name,
    branch
  )
