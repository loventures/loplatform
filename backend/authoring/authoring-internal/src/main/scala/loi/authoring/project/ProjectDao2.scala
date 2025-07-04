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

import cats.syntax.functor.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainFinder}
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.HibernateQueryOps.*
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import com.learningobjects.cpxp.util.{StringUtils, ThreadTerminator}
import loi.authoring.project.exception.ProjectError
import loi.authoring.workspace.ProjectWorkspace
import loi.cp.i18n.BundleMessage
import mouse.boolean.*
import org.hibernate.query.NativeQuery
import org.hibernate.{CacheMode, LockMode, Session}
import scalaz.ValidationNel
import scalaz.syntax.validation.*
import scaloi.syntax.boxes.*
import scaloi.syntax.classTag.classTagClass

import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

@Service
class ProjectDao2(
  domainDto: => DomainDTO,
  commitDao2: CommitDao2,
  session: => Session,
  userDto: => UserDTO
):

  def loadEntity(id: Long): Option[ProjectEntity2] =
    ThreadTerminator.check()
    Option(session.find(classOf[ProjectEntity2], id)).filter(_.del == null)

  def loadReference(project: Project2): ProjectEntity2 =
    ThreadTerminator.check()
    session.ref[ProjectEntity2](project.id)

  def load(id: Long): Option[Project2] =
    loadEntity(id).map(_.toProject2)

  def load(ids: Iterable[Long]): List[Project2] =
    ThreadTerminator.check()
    session
      .byMultipleIds(classOf[ProjectEntity2])
      .enableSessionCheck(true)
      .enableOrderedReturn(false)
      .`with`(CacheMode.NORMAL)
      .multiLoad(ids.boxInsideTo[java.util.List]())
      .asScala
      .filter(_.del == null)
      .map(_.toProject2)
      .toList
  end load

  def loadForUpdate(id: Long): Option[Project2] =
    session.flush()

    Option(session.find(classOf[ProjectEntity2], id))
      .map(entity =>
        session.lock(entity, LockMode.PESSIMISTIC_WRITE)
        session.refresh(entity)
        entity
      )
      .filter(_.del == null)
      .map(_.toProject2)
  end loadForUpdate

  /** Finds the home node ids that enclose every node in ths workspace. For local nodes, the found home node id is the
    * id for the workpsace.homeName. For remote nodes, the home node id is an ancestor that may or may not be claimed.
    *
    * @return
    *   the home node ids for every node in `commitId`
    */
  // html.1s rely upon some css and js in their course.1 ancestor.
  // this is hopefully the only occurrence of a necessary element _not_ being a descendant
  // when ancestor copy is dead, we should make said css and js a descendant and remove this
  def loadHomeIds(ws: ProjectWorkspace): Map[UUID, Long] =
    val depCommitIds          = ws.depInfos.values.map(_.commitId)
    val initializedDepCommits = commitDao2.loadWithInitializedDocs(depCommitIds)

    lazy val remoteHomeIds = for
      (projectId, depInfo) <- ws.depInfos
      initializedCommit    <- initializedDepCommits.get(depInfo.commitId)
      homeId               <- initializedCommit.comboDoc.getNodeId(initializedCommit.homeName)
    yield (projectId, homeId)

    val localHomeId = ws.getNodeId(ws.homeName)

    ws.nodeElems
      .flatMap(elem =>
        val homeId = if elem.isLocal then localHomeId else remoteHomeIds.get(elem.projectId)
        homeId.tupleLeft(elem.name)
      )
      .toMap
  end loadHomeIds

  def loadAll(activeFilter: Boolean, userFilter: Boolean): List[Project2] =
    ThreadTerminator.check()
    session
      .createQuery(
        // make sure to join fetch anything accessed in ProjectEntity2.toProject2
        s"""SELECT project
         |FROM ProjectEntity2 project
         |JOIN FETCH project.head
         |LEFT JOIN FETCH project.contributors contributor
         |WHERE project.root = :root
         |  AND project.del IS NULL
         |  ${activeFilter ?? "AND project.archived = FALSE"}
         |  ${userFilter ??
            """AND (
              |  project.id IN (SELECT pce.project.id
              |    FROM ProjectContributorEntity2 pce WHERE pce.user = :user)
              |  OR project.ownedBy = :user
              |)""".stripMargin}
         |ORDER BY project.name ASC
         |""".stripMargin,
        classOf[ProjectEntity2]
      )
      .setParameter("root", session.ref[DomainFinder](domainDto.id))
      .setParameterWhen(userFilter)("user", session.ref[UserFinder](userDto.id))
      .getResultList
      .asScala
      .toList
      .map(_.toProject2)
  end loadAll

  /** @return
    *   all project ids that `projectId` depends on, directly and transitively.
    */
  def loadTransitiveDependencies(projectId: Long): Set[Long] =

    session
      .createNativeQuery(
        s"""WITH RECURSIVE project_dep(project_id, idpath, cycle) AS (
         |  SELECT
         |    CAST(depproject_id AS BIGINT),
         |    ARRAY[:startId, CAST(depproject_id AS BIGINT)],
         |    false
         |  FROM authoringproject p
         |  JOIN authoringcommit head ON head.id = p.head_id
         |  JOIN authoringcommitdoc kfdoc ON head.kfdoc_id = kfdoc.id
         |  CROSS JOIN jsonb_object_keys(kfdoc.deps) AS depproject_id
         |  WHERE p.id = :startId
         |  UNION ALL
         |  SELECT
         |    CAST(depproject_id AS BIGINT),
         |    project_dep.idpath || CAST(depproject_id AS BIGINT),
         |    CAST(depproject_id AS BIGINT) = ANY(project_dep.idpath)
         |  FROM authoringproject p
         |  JOIN project_dep ON p.id = project_dep.project_id
         |  JOIN authoringcommit head ON head.id = p.head_id
         |  JOIN authoringcommitdoc kfdoc ON head.kfdoc_id = kfdoc.id
         |  CROSS JOIN jsonb_object_keys(kfdoc.deps) AS depproject_id
         |  WHERE NOT cycle
         |)
         |SELECT project_id FROM project_dep""".stripMargin,
      )
      .unwrap(classOf[NativeQuery[Number]])
      .addSynchronizedEntityClass(classOf[ProjectEntity2])
      .addSynchronizedEntityClass(classOf[CommitEntity2])
      .addSynchronizedEntityClass(classOf[CommitDocEntity])
      .setParameter("startId", projectId)
      .getResultList
      .asScala
      .view
      .map(_.longValue)
      .toSet

  /** The projects that directly reference this as a basis layer. */
  def loadImmediateDependentProjects(projectId: Long): List[Project2] =

    // IIRC there are ways to native query directly to JPA entities (even those with eager associations)
    // but
    val dependentProjectIds = session
      .createNativeQuery(
        s"""SELECT p.id
           |FROM authoringproject p
           |JOIN authoringcommit c ON c.id = p.head_id
           |JOIN authoringcommitdoc kfdoc ON kfdoc.id = c.kfdoc_id
           |CROSS JOIN jsonb_object_keys(kfdoc.deps) AS depproject_id
           |WHERE CAST(depproject_id AS BIGINT) = :projectId""".stripMargin,
      )
      .unwrap(classOf[NativeQuery[Number]])
      .addSynchronizedEntityClass(classOf[ProjectEntity2])
      .addSynchronizedEntityClass(classOf[CommitEntity2])
      .addSynchronizedEntityClass(classOf[CommitDocEntity])
      .setParameter("projectId", projectId)
      .getResultList
      .asScala
      .map(_.longValue)

    load(dependentProjectIds)
  end loadImmediateDependentProjects

  def delete(id: Long, deleteIdentifier: String): Unit =
    ThreadTerminator.check()
    loadEntity(id).foreach(p => p.del = deleteIdentifier)

  def loadProjectProps(prop: ProjectDao2.ProjectProp, startsWith: Option[String] = None): List[String] =
    ThreadTerminator.check()

    def query[A: ClassTag](branchy: Boolean): scala.collection.mutable.Buffer[String] =
      // Criteria API will protec from injection, unsure escape certain chars is needed
      val column = StringUtils.escapeSqlLike(prop)
      val sw     = startsWith.map(_.toLowerCase).map(StringUtils.escapeSqlLike)

      val cb   = session.getCriteriaBuilder
      val cr   = cb.createQuery(classOf[String])
      val root = cr.from(classTagClass[A])

      val predicates = Seq(
        cb.equal(if branchy then root.get("root") else root.get("root").get("id"), domainDto.id),
        cb.isNull(root.get("del")),
        cb.isNotNull(root.get(column))
      ) ++ sw.map(sw => cb.like(cb.lower(root.get(column)), sw + "%", '\\'))

      cr.select(root.get(column))
        .distinct(true)
        .where(predicates*)

      val query   = session.createQuery(cr)
      val results = query.getResultList
      results.asScala
    end query

    val p2s = query[ProjectEntity2](branchy = false)

    // can't union in db so have to sort in the application
    // BUT THIS WAY WE CAN SWITCH TO ORACLE
    p2s.toList.distinct.sorted
  end loadProjectProps
end ProjectDao2

object ProjectDao2:
  // patiently waiting for opaque types in scala3
  type ProjectProp = String

  object ProjectProp:
    def validate(prop0: String): ValidationNel[BundleMessage, ProjectProp] =
      val prop = prop0.trim
      if ValidProjectProps.contains(prop) then prop.successNel
      else ProjectError.invalidProp(prop).failureNel

  // list of column names, but case-sensitive because JPA metamodel is
  private val ValidProjectProps: Set[String] =
    Set("code", "productType", "category", "subCategory", "revision", "launchDate")
end ProjectDao2
