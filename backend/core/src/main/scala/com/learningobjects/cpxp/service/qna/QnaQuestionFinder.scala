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

import com.learningobjects.cpxp.entity.PeerEntity
import com.learningobjects.cpxp.entity.annotation.SqlIndex
import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.de.web.{Queryable, QueryableProperties}
import io.hypersistence.utils.hibernate.`type`.array.LongArrayType
import jakarta.persistence.{Column, Entity, FetchType, ManyToOne}
import org.hibernate.annotations as hibernate
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.lang
import java.util.Date

// Index supports basic cases okay. Could add a specific index
// where open is true ordered by create time for the instructor
// dashboard but .. overkill?
@Entity
@HCache(usage = READ_WRITE)
@SqlIndex("(section_id, creator_id, edgePath) WHERE del IS NULL")
@QueryableProperties(
  Array(
    new Queryable(
      name = QnaQuestionFinder.MessagesProperty,
      handler = classOf[QnaMessageSearchHandler],
      traits = Array(Queryable.Trait.NOT_SORTABLE)
    ),
    new Queryable(
      name = QnaQuestionFinder.SentProperty,
      handler = classOf[QnaMessageSentHandler],
      traits = Array(Queryable.Trait.NOT_SORTABLE)
    )
  )
)
class QnaQuestionFinder extends PeerEntity:
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @Queryable
  var section: GroupFinder = scala.compiletime.uninitialized

  /** This thread tracks the first student message of the current thread for analytics integrity */
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @Queryable
  var thread: QnaMessageFinder = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var edgePath: String = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var created: Date = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @Queryable
  var creator: UserFinder = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var modified: Date = scala.compiletime.uninitialized

  @Column
  var subject: String = scala.compiletime.uninitialized

  // null is student message, empty array means all learner enrollments
  @hibernate.Type(classOf[LongArrayType])
  @Column(columnDefinition = "BIGINT[]")
  @Queryable
  var recipients: Array[java.lang.Long] = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @Queryable
  var replyTo: QnaQuestionFinder = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var open: lang.Boolean = scala.compiletime.uninitialized // open means awaiting instructor

  @Column(nullable = false)
  @Queryable
  var closed: lang.Boolean = scala.compiletime.uninitialized // student or instructor closed it

  @Column
  var category: String = scala.compiletime.uninitialized

  @Column
  var subcategory: String = scala.compiletime.uninitialized
end QnaQuestionFinder

object QnaQuestionFinder:
  final val Section    = "QnaQuestion.section"
  final val EdgePath   = "QnaQuestion.edgePath"
  final val Creator    = "QnaQuestion.creator"
  final val Created    = "QnaQuestion.created"
  final val Modified   = "QnaQuestion.modified"
  final val Open       = "QnaQuestion.open"
  final val Closed     = "QnaQuestion.closed"
  final val Recipients = "QnaQuestion.recipients"

  final val MessagesProperty = "messages"
  final val SentProperty     = "sent"
end QnaQuestionFinder
