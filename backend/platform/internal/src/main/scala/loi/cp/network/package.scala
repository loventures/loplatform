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
import com.learningobjects.cpxp.service.network.ConnectionFinder.*
import com.learningobjects.cpxp.service.network.NetworkFinder.*

/** Data model evidence for social networks.
  */
package object network:

  /** Network component data model evidence.
    */
  implicit val NetworkDataModel: DataModel[Network] =
    DataModel(
      ITEM_TYPE_NETWORK,
      singleton = true,
      schemaMapped = false,
      Map(
        Network.NetworkIdProperty       -> DATA_TYPE_NETWORK_NETWORK_ID,
        Network.NameProperty            -> DATA_TYPE_NETWORK_NAME,
        Network.ConnectionModelProperty -> DATA_TYPE_NETWORK_CONNECTION_MODEL,
        Network.PeerNetworkProperty     -> DATA_TYPE_NETWORK_PEER,
        Network.PeerNetworkIdProperty   -> DATA_TYPE_NETWORK_PEER
      )
    )

  /** Connection component data model evidence.
    */
  implicit val ConnectionDataModel: DataModel[Connection] =
    DataModel(
      ITEM_TYPE_CONNECTION,
      singleton = true,
      schemaMapped = false,
      Map(
        Connection.NetworkProperty   -> DATA_TYPE_CONNECTION_NETWORK,
        Connection.NetworkIdProperty -> DATA_TYPE_CONNECTION_NETWORK,
        Connection.UserProperty      -> DATA_TYPE_CONNECTION_USER,
        Connection.UserIdProperty    -> DATA_TYPE_CONNECTION_USER,
        Connection.CreatedProperty   -> DATA_TYPE_CONNECTION_CREATED,
        Connection.ActiveProperty    -> DATA_TYPE_CONNECTION_ACTIVE
      )
    )
end network
