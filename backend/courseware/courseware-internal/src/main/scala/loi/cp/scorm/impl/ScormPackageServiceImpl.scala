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

package loi.cp.scorm.impl

import java.util.{Date, UUID}

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.{Comparison, QueryService, Function as QBFunction}
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.integration.SystemFinder
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.scorm.{ScormFormat, ScormPackage, ScormPackageService, ScormSystem}

@Service
class ScormPackageServiceImpl(
  domain: => DomainDTO,
  time: => Date,
  user: => UserDTO,
)(implicit
  itemService: ItemService,
  ontology: Ontology,
  queryService: QueryService,
) extends ScormPackageService:

  override def add(offering: LightweightCourse, system: ScormSystem, format: ScormFormat): ScormPackage =
    offering.addChild[ScormPackageFinder] { рackage =>
      рackage.packageId = UUID.randomUUID.toString
      рackage.format = format.entryName
      рackage.disabled = false
      рackage.createTime = time
      рackage.creator = user.finder[UserFinder]
      рackage.system = system.finder[SystemFinder]
    }

  override def find(packageId: String): Option[ScormPackage] =
    domain
      .queryAll[ScormPackageFinder]
      .addCondition(ScormPackageFinder.DATA_TYPE_SCORM_PACKAGE_ID, Comparison.eq, packageId, QBFunction.LOWER)
      .getFinder[ScormPackageFinder]
end ScormPackageServiceImpl
