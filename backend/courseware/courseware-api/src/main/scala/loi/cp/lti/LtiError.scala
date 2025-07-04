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

package loi.cp.lti

import loi.cp.Widen
import scalaz.Semigroup

/** LTI errors. */
sealed trait LtiError extends Widen[LtiError]:
  val msg: String
  def detailMsg: String = s"${msg}_detail"

object LtiError:
  implicit def ltiErrorSemigroup: Semigroup[LtiError] = Semigroup.firstSemigroup

/** Generic translated error with one optional parameter. */
final case class GenericLtiError(msg: String, param: String = "") extends LtiError

final case class FriendlyLtiError(msg: String, param: AnyRef, statusCode: Int) extends LtiError

object FriendlyLtiError:
  def apply(msg: String, statusCode: Int) = new FriendlyLtiError(msg, null, statusCode)

final case class MissingLtiParameter(name: String) extends LtiError:
  override val msg = "lti_missing_parameter"

final case class InvalidLtiParameter(name: String, value: String) extends LtiError:
  override val msg = "lti_invalid_parameter"

final case class MismatchedOffering(existing: String, provided: String) extends LtiError:
  override val msg: String = "lti_mismatched_offering"

final case class InternalLtiError(msg: String, th: Throwable) extends LtiError
