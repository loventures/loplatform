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

package loi.cp.asset.exchange.impl

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.scala.util.EnumDeserializer
import enumeratum.EnumEntry.Uppercase
import enumeratum.{ArgonautEnum, Enum, EnumEntry}

@JsonDeserialize(`using` = classOf[AssetExchangeRequestStatusDeserializer])
sealed trait AssetExchangeRequestStatus extends EnumEntry with Uppercase

object AssetExchangeRequestStatus
    extends Enum[AssetExchangeRequestStatus]
    with ArgonautEnum[AssetExchangeRequestStatus]:

  val values = findValues

  /** Non-LOAF file to be imported has been validated and is ready to bake. On the UI, this is between when a user has
    * submitted a file, (it passes validation internally), and is presented with a preview of the LOAF's contents.
    */
  case object Validated extends AssetExchangeRequestStatus

  /** The request is made but the clone has not begun.
    */
  case object Requested extends AssetExchangeRequestStatus

  /** The clone is underway.
    */
  case object Underway extends AssetExchangeRequestStatus

  /** The clone finished successfully.
    */
  case object Success extends AssetExchangeRequestStatus

  /** The clone encountered a terrible error of some kind.
    */
  case object Failure extends AssetExchangeRequestStatus
end AssetExchangeRequestStatus

private class AssetExchangeRequestStatusDeserializer
    extends EnumDeserializer[AssetExchangeRequestStatus](AssetExchangeRequestStatus)
