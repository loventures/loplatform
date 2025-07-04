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

package com.learningobjects.cpxp.service.subtenant

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.service.attachment.AttachmentFinder
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
class SubtenantFinder extends PeerEntity:

  @Column
  var name: String = scala.compiletime.uninitialized

  @Column
  @FriendlyName
  var tenantId: String = scala.compiletime.uninitialized

  @Column
  var shortName: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var logo: AttachmentFinder = scala.compiletime.uninitialized
end SubtenantFinder

object SubtenantFinder:
  final val ITEM_TYPE_SUBTENANT            = "Subtenant"
  final val DATA_TYPE_TENANT_ID            = "Subtenant.tenantId"
  final val DATA_TYPE_SUBTENANT_NAME       = "Subtenant.name"
  final val DATA_TYPE_SUBTENANT_SHORT_NAME = "Subtenant.shortName"
  final val DATA_TYPE_SUBTENANT_LOGO       = "Subtenant.logo"
