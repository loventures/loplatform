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

import com.learningobjects.cpxp.service.domain.DomainFinder
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.cpxp.util.ScalaPropertyAccessStrategy
import jakarta.persistence.*
import org.hibernate.annotations.{AttributeAccessor, CacheConcurrencyStrategy}
import org.hibernate.annotations as hibernate

import java.util.Objects
import scala.annotation.meta.field

// Instead of a @ManyToMany association between project and userfinder
// and a @JoinTable to form the desired link table, we explicitly model
// the link table as this ordinary entity. This way, we can join fetch
// to the link table without also join fetching to the target entity
// (userfinder). This is important because we load 1000+ projects and
// joining to userfinder and returning the same fat user row 1000 times
// is stupid and Hibernate is stupid for making me write all this crap
// instead of letting me control the fetch of the link table in a
// @ManyToMany.
//
// Warning against composite identifiers:
// There are several ways to model a composite identifier in Hibernate and this @MapsId style is
// the only one that works. The appealing dual-@Id@ManyToMany does not. The dual-@Id@ManyToMany
// will always miss the L2 cache, and also spam the cache with distinct puts. This is because
// of broken equality when using HibernateProxy instances from two different sessions
// (reminder: the L2 cache spans sessions; we expect cache-hits in subsequent sessions).
// As our composite identifier is not using HibernateProxy, cache key equality works.
// See Component.isEqual or just debug our SimpleCache on getFromLoad
@Entity
@Table(
  name = "authoringprojectcontributor",
  uniqueConstraints = Array(new UniqueConstraint(columnNames = Array("project_id", "user_id"))),
  indexes = Array(
    new Index(name = "authoringprojectcontributor_root_idx", columnList = "root_id"),
    new Index(name = "authoringprojectcontributor_user_idx", columnList = "user_id")
  )
)
@hibernate.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class ProjectContributorEntity2(
  @(EmbeddedId @field)
  @(AttributeAccessor @field)(strategy = classOf[ScalaPropertyAccessStrategy])
  var id: ProjectContributorId,
  @(MapsId @field)("projectId")
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var project: ProjectEntity2,
  @(MapsId @field)("userId")
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var user: UserFinder,
  @(Column @field)
  var role: String,
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var root: DomainFinder
):

  def this() = this(null, null, null, null, null)

  def this(project: ProjectEntity2, user: UserFinder, role: Option[String], root: DomainFinder) =
    this(new ProjectContributorId(project.id, user.getId), project, user, role.orNull, root)
end ProjectContributorEntity2

@Embeddable
class ProjectContributorId(
  var projectId: java.lang.Long,
  var userId: java.lang.Long
) extends Serializable:
  def this() = this(null, null)

  override def equals(other: Any): Boolean = other match
    case that: ProjectContributorId => Objects.equals(projectId, that.projectId) && Objects.equals(userId, that.userId)
    case _                          => false

  override def hashCode(): Int = Objects.hash(projectId, userId)

  override def toString = s"ProjectContributorId($projectId, $userId)"
end ProjectContributorId
