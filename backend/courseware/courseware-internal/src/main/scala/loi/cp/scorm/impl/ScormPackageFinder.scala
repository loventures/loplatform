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

import java.{lang as jl, util as ju}

import com.learningobjects.cpxp.entity.annotation.{FriendlyName, FunctionalIndex}
import com.learningobjects.cpxp.entity.{IndexType, LeafEntity}
import com.learningobjects.cpxp.service.integration.SystemFinder
import com.learningobjects.cpxp.service.user.UserFinder
import jakarta.persistence.*
import loi.cp.scorm.ScormPackage
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
private[impl] class ScormPackageFinder extends LeafEntity with ScormPackage:
  @Column
  @FriendlyName
  @FunctionalIndex(function = IndexType.LCASE, byParent = false, nonDeleted = true)
  var packageId: String = scala.compiletime.uninitialized

  @Column
  var format: String = scala.compiletime.uninitialized

  @Column
  var createTime: ju.Date = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var creator: UserFinder = scala.compiletime.uninitialized

  @Column
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var system: SystemFinder = scala.compiletime.uninitialized
end ScormPackageFinder

private[impl] object ScormPackageFinder:
  final val DATA_TYPE_SCORM_PACKAGE_ID = "ScormPackage.packageId"
