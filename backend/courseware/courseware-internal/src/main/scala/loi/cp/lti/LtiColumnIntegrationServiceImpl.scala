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

package loi.cp.lti

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.context.ContextId
import loi.cp.storage.impl.CourseStorageDao

@Service
class LtiColumnIntegrationServiceImpl(dao: CourseStorageDao) extends LtiColumnIntegrationService:
  import CourseStorageDao.*

  override def get(crs: ContextId): Option[CourseColumnIntegrations] =
    val entity = dao.loadCourseStorage(crs.id)

    entity
      .flatMap(_.getLtiColumnIntegrations)
      .map(json => decodeColumnIntegrations(crs, json))

  override def modify(
    crs: ContextId
  )(mod: Option[CourseColumnIntegrations] => Option[CourseColumnIntegrations]): Option[CourseColumnIntegrations] =
    val entity       = dao.ensureAndLoadCourseStorageForUpdate(crs.id)
    val integrations = entity.getLtiColumnIntegrations
      .map(data => decodeColumnIntegrations(crs, data))

    val modded  = mod(integrations)
    val encoded = modded.map(CourseColumnIntegrations.codec.encode)
    entity.setLtiColumnIntegrations(encoded)

    modded
  end modify
end LtiColumnIntegrationServiceImpl
