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
import com.learningobjects.cpxp.service.data.DataTypes.*
import com.learningobjects.cpxp.service.email.EmailFinder.*

package object email:

  /** Email data model evidence.
    */
  implicit val EmailDataModel: DataModel[Email] =
    DataModel(
      ITEM_TYPE_EMAIL,
      singleton = false,
      schemaMapped = true,
      Map(
        Email.EntityProperty  -> DATA_TYPE_EMAIL_ENTITY,
        Email.SuccessProperty -> DATA_TYPE_EMAIL_SUCCESS,
        Email.SentProperty    -> DATA_TYPE_EMAIL_SENT,
        Email.UserIdProperty  -> META_DATA_TYPE_PARENT_ID,
        Email.UserProperty    -> META_DATA_TYPE_PARENT_ID
      )
    )
end email
