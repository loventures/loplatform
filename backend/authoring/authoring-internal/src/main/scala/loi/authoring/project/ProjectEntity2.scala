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

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.domain.DomainFinder
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.cpxp.util.ScalaPropertyAccessStrategy
import jakarta.persistence.*
import org.hibernate.annotations.{AttributeAccessor, CacheConcurrencyStrategy}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType
import org.hibernate.annotations as hibernate

import java.time.{LocalDate, LocalDateTime}
import scala.annotation.meta.field
import scala.jdk.CollectionConverters.*

/** @param id
  * @param name
  * @param rootName
  *   root.1 node name, never presented
  * @param homeName
  *   course.1 node name
  * @param head
  * @param created
  *   only here to save us from a large head.parent iteration
  * @param createdBy
  *   only here to save us from a large head.parent iteration
  * @param ownedBy
  * @param contributors
  *   unordered list (aka a bag) of users
  * @param archived
  * @param del
  * @param code
  *   open to customer interpretation, course code, e.g. 0bh
  * @param productType
  *   open to customer interpretation
  * @param category
  *   open to customer interpretation
  * @param subCategory
  *   open to customer interpretation
  * @param revision
  *   open to customer interpretation
  * @param launchDate
  *   open to customer interpretation
  * @param liveVersion
  *   open to customer interpretation (status stored in this string)
  * @param s3
  *   open to customer interpretation (an "S3 directory where the course assets are stored")
  * @param maintenance
  *   prevent authoring access to this project
  * @param root
  */
@Entity
@Table(
  name = "authoringproject",
  indexes = Array(
    new Index(name = "authoringproject_root_idx", columnList = "root_id")
  )
)
@hibernate.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class ProjectEntity2(
  @(Id @field)
  @(GeneratedValue @field)(generator = "cpxp-sequence")
  @(AttributeAccessor @field)(strategy = classOf[ScalaPropertyAccessStrategy])
  var id: java.lang.Long,
  @(Basic @field)(optional = false)
  var name: String,
  // is eager because we always access the head's created and createdBy
  // which are the "last modified" info for the project
  @(ManyToOne @field)(fetch = FetchType.EAGER, optional = false)
  var head: CommitEntity2,
  @(Basic @field)(optional = false)
  var created: LocalDateTime,
  @(ManyToOne @field)(fetch = FetchType.LAZY)
  var createdBy: UserFinder,
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var ownedBy: UserFinder,
  // is eager because we always access contributor's user.id
  @(OneToMany @field)(
    cascade = Array(CascadeType.ALL),
    fetch = FetchType.EAGER,
    mappedBy = "project",
    orphanRemoval = true
  )
  @(hibernate.Cache @field)(usage = CacheConcurrencyStrategy.READ_WRITE)
  var contributors: java.util.List[ProjectContributorEntity2] /* a bag, has no order */,
  @(Basic @field)(optional = false)
  var archived: java.lang.Boolean,
  @(Column @field)(columnDefinition = "BOOLEAN DEFAULT FALSE")
  @(Basic @field)(optional = false)
  var published: java.lang.Boolean,
  var del: String,
  var code: String,
  var productType: String,
  var category: String,
  var subCategory: String,
  var revision: java.lang.Integer,
  var launchDate: LocalDate,
  var liveVersion: String,
  var s3: String,
  @(Column @field)(columnDefinition = "JSONB")
  @(hibernate.Type @field)(classOf[JsonNodeUserType])
  @(hibernate.JdbcType @field)(classOf[PostgreSQLJsonPGObjectJsonbType])
  var configuration: JsonNode /* document is same as what ConfigFacade accesses on an Ontological entity */,
  var maintenance: java.lang.Boolean,
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var root: DomainFinder,
):

  // zero arg constructor required by Hibernate
  def this() = this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    null, null, null, null, null)

  def this(
    name: String,
    head: CommitEntity2,
    created: LocalDateTime,
    createdBy: UserFinder,
    ownedBy: UserFinder,
    code: String,
    productType: String,
    category: String,
    subCategory: String,
    revision: java.lang.Integer,
    launchDate: LocalDate,
    liveVersion: String,
    s3: String,
    configuration: JsonNode,
    root: DomainFinder
  ) =
    this(
      null,
      name,
      head,
      created,
      createdBy,
      ownedBy,
      new java.util.ArrayList(),
      archived = false,
      published = false,
      null,
      code,
      productType,
      category,
      subCategory,
      revision,
      launchDate,
      liveVersion,
      s3,
      configuration,
      null,
      root,
    )

  def addContributor(contributor: UserFinder, role: Option[String]): Unit =
    val pc = new ProjectContributorEntity2(this, contributor, role, root)
    contributors.add(pc)

  def removeContributor(contributor: UserFinder): Boolean =
    contributors.removeIf(_.user.getId == contributor.getId)

  def toProject2: Project2 = Project2(
    id,
    name,
    head.toCommit,
    created,
    Option(createdBy).map(_.getId),
    ownedBy.getId,
    contributors.asScala.view.map(c => c.user.getId.longValue -> Option(c.role)).toMap,
    archived,
    published,
    Option(code),
    Option(productType),
    Option(category),
    Option(subCategory),
    Option(revision).map(Integer2int),
    Option(launchDate),
    Option(liveVersion),
    Option(s3),
    Option(configuration),
    Option(maintenance).contains(true),
  )
end ProjectEntity2
