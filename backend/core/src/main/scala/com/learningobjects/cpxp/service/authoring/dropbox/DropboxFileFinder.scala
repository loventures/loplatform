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

package com.learningobjects.cpxp.service.authoring.dropbox

import com.learningobjects.cpxp.component.query.ApiFilter
import com.learningobjects.cpxp.entity.LeafEntity
import com.learningobjects.cpxp.entity.annotation.SqlIndex
import com.learningobjects.cpxp.service.attachment.AttachmentFinder
import com.learningobjects.cpxp.service.query.QueryBuilder
import com.learningobjects.de.web.{QueryHandler, Queryable, QueryableProperties}
import jakarta.persistence.{Column, Entity, FetchType, ManyToOne}

import java.lang
import java.util.UUID

@Entity
@SqlIndex("(project, branch, asset, archived) WHERE del IS NULL")
@QueryableProperties(Array(new Queryable(name = "isDirectory", handler = classOf[isDirectoryHandler])))
class DropboxFileFinder extends LeafEntity:
  @Column(nullable = false)
  @Queryable
  var project: lang.Long = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var branch: lang.Long = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var asset: UUID = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var archived: lang.Boolean = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @Queryable
  var folder: DropboxFileFinder = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @Queryable(joinComponent = classOf[AttachmentFinder])
  var file: AttachmentFinder = scala.compiletime.uninitialized
end DropboxFileFinder

class isDirectoryHandler extends QueryHandler:
  override def applyFilter(qb: QueryBuilder, filter: ApiFilter): Unit =
    qb.getOrCreateJoinQuery("DropboxFile.file", "Attachment")
      .addCondition("Attachment.digest", "eq", null)
