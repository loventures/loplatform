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

package loi.cp.bus

import argonaut.{CodecJson, DecodeJson, EncodeJson}
import loi.cp.Widen
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.util.EntityUtils
import scalaz.\/
import scaloi.json.ArgoExtras

/** Represents the possible outcomes of an attempt to deliver something that may succeed, fail permanently or fail
  * temporarily.
  */
sealed trait DeliveryResult:
  def isSuccess: Boolean = this == Delivered
  def isFailure: Boolean = !isSuccess

/** Delivery result companion. */
object DeliveryResult:

  /** Attempt to execute a delivery function that may transiently fail.
    * @param f
    *   the function to attempt
    * @return
    *   delivery success or a transient failure if an exception occurs
    */
  def ofTransientlyFallible(f: => Unit): DeliveryResult =
    TransientFailure.attempt { f; Delivered }
end DeliveryResult

/** Represents a failure. */
sealed trait DeliveryFailure extends DeliveryResult with Widen[DeliveryFailure]

/** The payload was delivered successfully. */
case object Delivered extends DeliveryResult

/** A permanent failure occurred during the delivery process. A typical permanent failure would be an internal error.
  */
final case class PermanentFailure(th: Throwable) extends DeliveryFailure

/** Permanent failure companion. */
object PermanentFailure:

  /** Construct a permanent failure from an error string. */
  def apply(message: String): PermanentFailure = PermanentFailure(DeliveryException(message))

/** A transient failure occurred during the delivery process. A typical transient failure would be a network error.
  */
final case class TransientFailure(failure: Throwable Either FailureInformation) extends DeliveryFailure

case class FailureInformation(req: Request, resp: Response Either Throwable)

object FailureInformation:
  def apply(req: Request, t: Throwable): FailureInformation   = new FailureInformation(req, Right(t))
  def apply(req: Request, resp: Response): FailureInformation = new FailureInformation(req, Left(resp))

/** Transient failure companion. */
object TransientFailure:

  /** Construct a transient failure from an error string. */
  def apply(message: String): TransientFailure = TransientFailure(DeliveryException(message))

  /** Construct a TransientFailure from an exception */
  def apply(ex: Throwable): TransientFailure = TransientFailure(Left(ex))

  /** Construct a TransientFailure from information describing the failure */
  def apply(info: FailureInformation): TransientFailure = TransientFailure(Right(info))

  /** Attempt a delivery operation, capturing any exception as a transient failure. */
  def attempt(f: => DeliveryResult): DeliveryResult =
    \/.attempt(f)(TransientFailure.apply).merge

  /** Attempt a delivery operation, capturing any exception as a transient failure. */
  def process[T](f: => T): TransientFailure \/ T =
    \/.attempt(f)(TransientFailure.apply)
end TransientFailure

case class Request(url: String, body: String, method: String)
object Request:

  def apply[A <: HttpUriRequest](req: A): Request =
    val body = req match
      case req: HttpEntityEnclosingRequest => EntityUtils.toString(req.getEntity)
      case _                               => ""
    new Request(req.getURI.toString, body, req.getMethod)

  def apply(url: String, body: String, method: String): Request = new Request(url, body, method)

  implicit val codec: CodecJson[Request]   =
    CodecJson.casecodec3(Request.apply, ArgoExtras.unapply)("url", "body", "method")
  implicit val decode: DecodeJson[Request] = codec.Decoder
  implicit val encode: EncodeJson[Request] = codec.Encoder
end Request

case class Response(body: Option[String], contentType: String, status: Int)
object Response:
  def apply(body: Option[String], contentType: String, status: Int): Response = new Response(body, contentType, status)
  implicit val codec: CodecJson[Response]                                     =
    CodecJson.casecodec3(Response.apply, ArgoExtras.unapply)("body", "contentType", "status")
  implicit val decode: DecodeJson[Response]                                   = codec.Decoder
  implicit val encode: EncodeJson[Response]                                   = codec.Encoder
