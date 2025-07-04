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

package loi.cp.lti.ags

import argonaut.{DecodeJson, EncodeJson}
import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.bus.{DeliveryFailure, YieldTx}
import loi.cp.integration.LtiSystemComponent
import scalaz.\/

import scala.reflect.ClassTag

@Service
trait LtiRequestService:

  /** Signs and sends an LTI POST request.
    * @param url
    * @param system
    * @param contentType
    * @param entity
    * @param untx
    * @tparam Body
    *   the type of the request Body
    * @tparam Resp
    *   the type of the expected Response
    * @return
    */
  def sendLtiServicePost[Body: EncodeJson, Resp: DecodeJson: ClassTag](
    url: String,
    system: LtiSystemComponent[?],
    contentType: String,
    entity: Body,
    untx: YieldTx
  ): DeliveryFailure \/ Resp

  /** Signs and sends an LTI POST request, and doesn't try to deserialize the response
    */
  def sendLtiServicePostWithoutResp[Body: EncodeJson](
    url: String,
    system: LtiSystemComponent[?],
    contentType: String,
    entity: Body,
    untx: YieldTx
  ): DeliveryFailure \/ Unit

  def sendLtiServiceDelete(
    url: String,
    system: LtiSystemComponent[?],
    untx: YieldTx
  ): DeliveryFailure \/ Unit
end LtiRequestService
