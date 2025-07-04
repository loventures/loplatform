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

package loi.cp.notification

import argonaut.{CodecJson, DecodeJson, EncodeJson}
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.notification.SubscribeFinder
import com.learningobjects.cpxp.scala.json.JEnumCodec.*
import scaloi.json.ArgoExtras

@FacadeItem(SubscribeFinder.ITEM_TYPE_SUBSCRIBE)
trait SubscribeFacade extends Facade:
  @FacadeData(SubscribeFinder.DATA_TYPE_SUBSCRIBE_CONTEXT)
  def getContext: Option[Long]

  @FacadeData(SubscribeFinder.DATA_TYPE_SUBSCRIBE_SUBSCRIPTIONS)
  def getSubscriptions(implicit
    d: DecodeJson[Map[SubscriptionPath, Subscribe]]
  ): Option[Map[SubscriptionPath, Subscribe]]
  def setSubscriptions(subscriptions: Map[SubscriptionPath, Subscribe])(implicit
    e: EncodeJson[Map[SubscriptionPath, Subscribe]]
  ): Unit

  /** deprecated */
  @FacadeData(SubscribeFinder.DATA_TYPE_SUBSCRIBE_INTEREST)
  def getInterest: Option[Long]
  def setInterest(interest: Long): Unit

  /** deprecated */
  @FacadeData(SubscribeFinder.DATA_TYPE_SUBSCRIBE_ITEM)
  def getItem: Option[Long]
  def setItem(item: Id): Unit
end SubscribeFacade

/** Model of a subscription to a path. Currently just describes your interest. */
case class Subscribe(
  interest: Interest
)

object Subscribe:
  implicit def subscribeCodec: CodecJson[Subscribe] =
    CodecJson.casecodec1(Subscribe.apply, ArgoExtras.unapply1)("interest")
