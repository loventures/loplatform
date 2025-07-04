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

import argonaut.Json
import com.learningobjects.cpxp.entity.LeafEntity
import com.learningobjects.cpxp.entity.annotation.FunctionalIndex
import com.learningobjects.cpxp.postgresql.ArgonautUserType
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.de.web.Queryable
import io.hypersistence.utils.hibernate.`type`.array.LongArrayType
import jakarta.persistence.*
import org.hibernate.annotations.{JdbcType, Type}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.lang
import java.util.Date

@Entity
class FeedbackActivityFinder extends LeafEntity:
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @Queryable
  @FunctionalIndex(byParent = false, nonDeleted = true)
  var feedback: AssetFeedbackFinder = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var created: Date = scala.compiletime.uninitialized

  @Column
  @Queryable
  var edited: Date = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @Queryable
  var creator: UserFinder = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var event: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @Type(classOf[ArgonautUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var value: Json = scala.compiletime.uninitialized

  @Type(classOf[LongArrayType])
  @Column(nullable = false, columnDefinition = "BIGINT[]")
  var attachments: Array[lang.Long] = scala.compiletime.uninitialized
end FeedbackActivityFinder

object FeedbackActivityFinder:
  final val Feedback = "FeedbackActivity.feedback"
  final val Created  = "FeedbackActivity.created"
  final val Creator  = "FeedbackActivity.creator"
  final val Event    = "FeedbackActivity.event"
  final val Value    = "FeedbackActivity.value"
