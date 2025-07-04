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
import com.learningobjects.cpxp.service.subtenant.SubtenantConstants.*

package object subtenant:

  implicit val SubtenantDataModel: DataModel[Subtenant] =
    DataModel(
      ITEM_TYPE_SUBTENANT,
      singleton = true,
      schemaMapped = false,
      Map(
        Subtenant.TenantIdProperty  -> DATA_TYPE_TENANT_ID,
        Subtenant.NameProperty      -> DATA_TYPE_SUBTENANT_NAME,
        Subtenant.ShortNameProperty -> DATA_TYPE_SUBTENANT_SHORT_NAME
      )
    )
end subtenant
