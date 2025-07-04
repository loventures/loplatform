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

package com.learningobjects.cpxp.service.qna

import com.learningobjects.cpxp.entity.LeafEntity
import com.learningobjects.cpxp.entity.annotation.{FinderDataDef, FunctionalIndex}
import com.learningobjects.cpxp.postgresql.TSVectorUserType
import com.learningobjects.cpxp.service.data.DataFormat
import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.de.web.Queryable
import io.hypersistence.utils.hibernate.`type`.array.LongArrayType
import jakarta.persistence.{Column, Entity, FetchType, ManyToOne}
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{ColumnTransformer, Type, Cache as HCache}

import java.lang
import java.util.Date

// alter table qnamessagefinder add column section_id bigint;
// update qnamessagefinder m set section_id = q.section_id from qnaquestionfinder q where q.id = m.parent_id;
// alter table qnamessagefinder alter column section_id set not null;

// Parent is the question
@Entity
@HCache(usage = READ_WRITE)
class QnaMessageFinder extends LeafEntity:
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @Queryable
  @FunctionalIndex(byParent = false, nonDeleted = false)
  var section: GroupFinder = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var created: Date = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @Queryable
  var creator: UserFinder = scala.compiletime.uninitialized

  @Column(nullable = true)
  @Queryable
  var edited: Date = scala.compiletime.uninitialized

  @Column(nullable = false, columnDefinition = "TEXT")
  var message: String = scala.compiletime.uninitialized

  @Type(classOf[LongArrayType])
  @Column(nullable = false, columnDefinition = "BIGINT[]")
  var attachments: Array[lang.Long] = scala.compiletime.uninitialized

  // An index here is of no value because the query plan will always use a section index and then a row filter
  @Column(columnDefinition = "TSVECTOR")
  @ColumnTransformer(write = "to_tsvector('english',LOWER(?))")
  @Type(classOf[TSVectorUserType])
  @FinderDataDef(DataFormat.tsvector)
  var search: String = scala.compiletime.uninitialized
end QnaMessageFinder

object QnaMessageFinder:
  final val ItemType = "QnaMessage"
  final val Section  = "QnaMessage.section"
  final val Created  = "QnaMessage.created"
  final val Search   = "QnaMessage.search"
