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

package com.learningobjects.cpxp.service.localmail

import java.util.Date

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.FunctionalIndex
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
class LocalmailFinder extends PeerEntity:

  @Column(columnDefinition = "TEXT")
  var body: String = scala.compiletime.uninitialized

  @Column
  var date: Date = scala.compiletime.uninitialized

  @Column
  var fromAddress: String = scala.compiletime.uninitialized

  @Column
  var fromName: String = scala.compiletime.uninitialized

  @Column
  var inReplyTo: String = scala.compiletime.uninitialized

  @Column
  var messageId: String = scala.compiletime.uninitialized

  @Column
  var subject: String = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(function = IndexType.LCASE, byParent = true, nonDeleted = true)
  var toAddress: String = scala.compiletime.uninitialized

  @Column
  var toName: String = scala.compiletime.uninitialized
end LocalmailFinder

object LocalmailFinder:
  final val ITEM_TYPE_LOCALMAIL              = "Localmail"
  final val DATA_TYPE_LOCALMAIL_SUBJECT      = "Localmail.subject"
  final val DATA_TYPE_LOCALMAIL_TO_NAME      = "Localmail.toName"
  final val DATA_TYPE_LOCALMAIL_IN_REPLY_TO  = "Localmail.inReplyTo"
  final val DATA_TYPE_LOCALMAIL_DATE         = "Localmail.date"
  final val DATA_TYPE_LOCALMAIL_BODY         = "Localmail.body"
  final val DATA_TYPE_LOCALMAIL_TO_ADDRESS   = "Localmail.toAddress"
  final val DATA_TYPE_LOCALMAIL_FROM_ADDRESS = "Localmail.fromAddress"
  final val DATA_TYPE_LOCALMAIL_FROM_NAME    = "Localmail.fromName"
  final val DATA_TYPE_LOCALMAIL_MESSAGE_ID   = "Localmail.messageId"
end LocalmailFinder
