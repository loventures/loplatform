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

package com.learningobjects.cpxp.service.integration

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
class ExternalRoleFinder extends PeerEntity:
  import ExternalRoleFinder.*

  @Column
  @DataType(DATA_TYPE_EXTERNAL_ROLE_ID)
  var externalRoleId: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_EXTERNAL_ROLE_TYPE)
  var externalRoleType: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_NAME)
  var name: String = scala.compiletime.uninitialized
end ExternalRoleFinder

object ExternalRoleFinder:
  final val ITEM_TYPE_EXTERNAL_ROLE      = "ExternalRole"
  final val DATA_TYPE_EXTERNAL_ROLE_TYPE = "externalRoleType"
  final val DATA_TYPE_EXTERNAL_ROLE_ID   = "externalRoleId"
  final val DATA_TYPE_NAME               = "name"
