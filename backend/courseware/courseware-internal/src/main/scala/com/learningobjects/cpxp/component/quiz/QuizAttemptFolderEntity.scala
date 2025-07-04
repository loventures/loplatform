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

package com.learningobjects.cpxp.component.quiz

import com.learningobjects.cpxp.entity.DomainEntity
import com.learningobjects.cpxp.entity.annotation.DataType
import com.learningobjects.cpxp.service.user.UserFinder
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

/** An entity for all user's attempt to go under. This allows for locking the collection of all attempt to prevent
  * concurrency issues.
  */
@Entity
@Table(name = QuizAttemptFolderEntity.ITEM_TYPE_QUIZ_ATTEMPT_FOLDER)
@HCache(usage = READ_WRITE)
class QuizAttemptFolderEntity extends DomainEntity:

  @DataType(QuizAttemptFolderEntity.DATA_TYPE_QUIZ_ATTEMPT_FOLDER_USER)
  @OneToOne(optional = false, fetch = FetchType.LAZY, targetEntity = classOf[UserFinder])
  var user: UserFinder = scala.compiletime.uninitialized

object QuizAttemptFolderEntity:
  final val ITEM_TYPE_QUIZ_ATTEMPT_FOLDER      = "QuizAttemptFolder"
  final val DATA_TYPE_QUIZ_ATTEMPT_FOLDER_USER = "QuizAttemptFolder.user"
