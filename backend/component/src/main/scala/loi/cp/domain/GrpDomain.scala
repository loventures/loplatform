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

package loi.cp.domain

import java.time.Instant

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{BaseComponent, ComponentEnvironment, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.de.enrollment.EnrollmentOwnerImpl
import loi.cp.enrollment.EnrollmentComponent
import loi.cp.role.SupportedRoleService

/** This is an abomination, but it makes the domain respond as a vestigial group component for the purpose of reasoning
  * about enrollments. Most methods will throw, but the group type will be DOMAIN which can be reasonably understood.
  */
// my death is now yet more imminent
@Component
class GrpDomain(
  ci: ComponentInstance,
  domain: => DomainDTO,
  env: ComponentEnvironment,
  override val enrollmentWebService: EnrollmentWebService,
  override val supportedRoleService: SupportedRoleService,
  override val componentService: ComponentService
) extends BaseComponent(ci)
    with GrpDomainComponent
    with EnrollmentOwnerImpl:

  override def getId: java.lang.Long = domain.id

  override def getName: String = domain.name

  override def getGroupType: GroupType = GroupType.DOMAIN

  @SuppressWarnings(Array("unused")) // lohtml embed component config
  def getComponentConfiguration: java.util.Map[String, AnyRef] =
    env.getJsonConfiguration(getComponentInstance.getIdentifier)

  override def getUrl: String = throw new UnsupportedOperationException

  override def setUrl(url: String): Unit = throw new UnsupportedOperationException

  override def setName(name: String): Unit = throw new UnsupportedOperationException

  override def getGroupId: String = throw new UnsupportedOperationException

  override def setGroupId(groupId: String): Unit = throw new UnsupportedOperationException

  override def getExternalId: java.util.Optional[String] = throw new UnsupportedOperationException

  override def setExternalId(externalId: java.util.Optional[String]): Unit = throw new UnsupportedOperationException

  override def getCreateTime: Instant = throw new UnsupportedOperationException

  override def setCreateTime(createTime: Instant): Unit = throw new UnsupportedOperationException

  override def setGroupType(groupType: GroupType): Unit = throw new UnsupportedOperationException

  override def getInDirectory: java.lang.Boolean = throw new UnsupportedOperationException

  override def setInDirectory(inDirectory: java.lang.Boolean): Unit = throw new UnsupportedOperationException

  override def getDisabled = throw new UnsupportedOperationException

  override def setDisabled(disabled: java.lang.Boolean): Unit = throw new UnsupportedOperationException

  override def setProject(projectId: java.lang.Long): Unit = throw new UnsupportedOperationException

  override def getUnavailable: java.lang.Boolean = throw new UnsupportedOperationException

  override def setUnavailable(unavailable: java.lang.Boolean): Unit = throw new UnsupportedOperationException

  override def getBranchId: java.util.Optional[java.lang.Long] = throw new UnsupportedOperationException

  override def getProjectId: java.util.Optional[java.lang.Long] = throw new UnsupportedOperationException

  override def getCommitId: java.util.Optional[java.lang.Long] = throw new UnsupportedOperationException

  override def setCommitId(commitId: java.util.Optional[java.lang.Long]): Unit = ()

  override def getRights: java.util.Set[String] = throw new UnsupportedOperationException

  override def getProperties: JsonNode = throw new UnsupportedOperationException

  override def setProperty(propertyName: String, value: Object): Unit = throw new UnsupportedOperationException

  override def getProperty[T](propertyName: String, `type`: Class[T]) = throw new UnsupportedOperationException

  override def getEnrollments(userId: Long): Seq[EnrollmentComponent] = throw new UnsupportedOperationException
end GrpDomain
