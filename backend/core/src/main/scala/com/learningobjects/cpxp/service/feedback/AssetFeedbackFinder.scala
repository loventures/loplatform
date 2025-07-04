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

package com.learningobjects.cpxp.service.feedback

import com.learningobjects.cpxp.component.query.ApiFilter
import com.learningobjects.cpxp.entity.LeafEntity
import com.learningobjects.cpxp.entity.annotation.{FunctionalIndex, SqlIndex}
import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.query.{BaseCondition, Comparison, QueryBuilder}
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.de.web.{QueryHandler, Queryable, QueryableProperties}
import io.hypersistence.utils.hibernate.`type`.array.LongArrayType
import jakarta.persistence.{Column, Entity, FetchType, ManyToOne}
import org.hibernate.annotations.Type

import java.lang
import java.util.{Date, UUID}

@Entity
@QueryableProperties(Array(new Queryable(name = "person", handler = classOf[FeedbackPersonHandler])))
@SqlIndex(name = "assetfeedbackfinder_remotes_idx", value = "USING GIN(remotes) WHERE del IS NULL")
class AssetFeedbackFinder extends LeafEntity:
  @Column(nullable = false)
  @Queryable
  var project: lang.Long = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  @FunctionalIndex(byParent = false, nonDeleted = true)
  var branch: lang.Long = scala.compiletime.uninitialized

  @Type(classOf[LongArrayType])
  @Column(nullable = false, columnDefinition = "BIGINT[]")
  @Queryable
  var remotes: Array[lang.Long] = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var assetName: UUID = scala.compiletime.uninitialized

  @Column(nullable = true)
  @Queryable
  var contentName: UUID = scala.compiletime.uninitialized

  @Column(nullable = true)
  @Queryable
  var lessonName: UUID = scala.compiletime.uninitialized

  @Column(nullable = true)
  @Queryable
  var moduleName: UUID = scala.compiletime.uninitialized

  @Column(nullable = true)
  @Queryable
  var unitName: UUID = scala.compiletime.uninitialized

  @Column(nullable = true)
  @Queryable
  var identifier: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @Queryable
  var section: GroupFinder = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var created: Date = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @Queryable
  var creator: UserFinder = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var modified: Date = scala.compiletime.uninitialized // any change to the feedback or activity on the feedback

  @Column(nullable = true)
  @Queryable
  var edited: Date = scala.compiletime.uninitialized // specifically the feedback text/attachments edited

  @Column(nullable = false)
  @Queryable
  var role: String = scala.compiletime.uninitialized

  @Column(nullable = true)
  @Queryable
  var status: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @Queryable
  var assignee: UserFinder = scala.compiletime.uninitialized

  @Column(columnDefinition = "TEXT")
  var quote: String = scala.compiletime.uninitialized

  @Column(nullable = false, columnDefinition = "TEXT")
  var feedback: String = scala.compiletime.uninitialized

  @Type(classOf[LongArrayType])
  @Column(nullable = false, columnDefinition = "BIGINT[]")
  var attachments: Array[lang.Long] = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var closed: lang.Boolean = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var replies: lang.Long = scala.compiletime.uninitialized

  @Column // (nullable = false)
  @Queryable
  var archived: lang.Boolean = scala.compiletime.uninitialized
end AssetFeedbackFinder

object AssetFeedbackFinder:
  final val Branch      = "AssetFeedback.branch"
  final val Creator     = "AssetFeedback.creator"
  final val Created     = "AssetFeedback.created"
  final val AssetName   = "AssetFeedback.assetName"
  final val ContentName = "AssetFeedback.contentName"
  final val LessonName  = "AssetFeedback.lessonName"
  final val ModuleName  = "AssetFeedback.moduleName"
  final val UnitName    = "AssetFeedback.unitName"
  final val Assignee    = "AssetFeedback.assignee"
  final val Archived    = "AssetFeedback.archived"
  final val Identifier  = "AssetFeedback.identifier"
end AssetFeedbackFinder

/** Matches either creator or assignee. */
class FeedbackPersonHandler extends QueryHandler:
  override def applyFilter(qb: QueryBuilder, filter: ApiFilter): Unit =
    qb.addDisjunction(
      Seq(
        BaseCondition.getInstance(AssetFeedbackFinder.Creator, Comparison.eq, filter.getValue.toLong),
        BaseCondition.getInstance(AssetFeedbackFinder.Assignee, Comparison.eq, filter.getValue.toLong)
      )
    )
