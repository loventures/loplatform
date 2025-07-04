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

package loi.cp

import com.learningobjects.cpxp.component.DataModel
import com.learningobjects.cpxp.component.messaging.MessageConstants.*

package object message:
  import Message.*

  implicit final val messageDataModel: DataModel[Message] =
    DataModel[Message](
      itemType = ITEM_TYPE_MESSAGE,
      singleton = true,
      schemaMapped = false,
      dataTypes = Map(
        LabelProperty     -> DATA_TYPE_MESSAGE_LABEL,
        ReadProperty      -> DATA_TYPE_MESSAGE_READ,
        SubjectProperty   -> DATA_TYPE_MESSAGE_SUBJECT,
        // DispositionProperty -> DATA_TYPE_MESSAGE_DISPOSITION,
        MessageIdProperty -> DATA_TYPE_MESSAGE_STORAGE,
        InReplyToProperty -> DATA_TYPE_MESSAGE_IN_REPLY_TO,
        ThreadProperty    -> DATA_TYPE_MESSAGE_THREAD,
        TimestampProperty -> DATA_TYPE_MESSAGE_TIMESTAMP,
        SenderIdProperty  -> DATA_TYPE_MESSAGE_SENDER,
        ContextIdProperty -> DATA_TYPE_MESSAGE_CONTEXT
      )
    )
end message
