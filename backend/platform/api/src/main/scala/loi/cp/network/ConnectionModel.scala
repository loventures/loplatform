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

package loi.cp.network

import enumeratum.{Enum, EnumEntry}

// TODO: It may be that the connection model is in fact modeled by grants
// on the network that allow, for example, faculty to establish connections
// so then it would be Grant: Role -> Make|Request

/** Supported social network connection models.
  */
sealed trait ConnectionModel extends EnumEntry

/** Connection model companion.
  */
object ConnectionModel extends Enum[ConnectionModel]:

  /** All connection models. */
  override val values = findValues

  /** Only the system (administrators or integration) will establish connections. */
  case object System extends ConnectionModel

  /** Users may establish connections unilaterally. */
  case object User extends ConnectionModel

  /** Users may request connections, reciprocation is required. */
  case object Request extends ConnectionModel
end ConnectionModel
