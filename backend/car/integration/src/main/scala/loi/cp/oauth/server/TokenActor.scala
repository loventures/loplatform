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

package loi.cp.oauth.server

import org.apache.pekko.actor.{Actor, ActorRef, Props}
import com.learningobjects.cpxp.scala.actor.{ClusterActors, CpxpActorSystem}
import loi.cp.oauth.TokenResponse
import scalaz.{@@, Tag}

import scala.collection.mutable
import scala.concurrent.duration.*

class TokenActor extends Actor:
  import TokenActor.*

  val tokens: mutable.Map[Long, TokenState] = mutable.Map.empty

  override val receive: Receive = {
    case GetToken(connectorId: Long)                            => handleGetToken(connectorId)
    case AddOrUpdateToken(connectorId: Long, token: TokenState) => handleAddOrUpdateToken(connectorId, token)
  }

  private def handleGetToken(connectorId: Long): Unit =
    sender() ! TokenRequestResponse(tokens.get(connectorId))

  private def handleAddOrUpdateToken(connectorId: Long, token: TokenState): Unit =
    tokens.update(connectorId, token)
end TokenActor

object TokenActor:
  type Ref = ActorRef @@ TokenActor

  val props: Props = Props(new TokenActor())

  lazy val clusterActor: Ref =
    Tag.of[TokenActor](ClusterActors.singleton(props, "tokens")(using CpxpActorSystem.system))

  case class GetToken(connectorId: Long)
  case class AddOrUpdateToken(connectorId: Long, token: TokenState)
end TokenActor

case class TokenRequestResponse(token: Option[TokenState])

case class TokenState(
  expiration: Long,
  token: TokenResponse
):
  def hasExpired: Boolean = expiration > System.currentTimeMillis

object TokenState:
  def apply(token: TokenResponse): TokenState = TokenState(
    expiration = System.currentTimeMillis + token.expiresIn.fold(1.hour)(_.seconds).toMillis,
    token = token
  )
