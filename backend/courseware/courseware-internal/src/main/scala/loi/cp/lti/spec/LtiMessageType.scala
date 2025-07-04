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

package loi.cp.lti.spec

import enumeratum.{Enum, EnumEntry}

/** LTI message types. */
sealed abstract class LtiMessageType(override val entryName: String) extends EnumEntry

object LtiMessageType extends Enum[LtiMessageType]:
  // noinspection TypeAnnotation
  val values = findValues

  case object BasicLtiLaunchRequest       extends LtiMessageType("basic-lti-launch-request")
  case object ContentItemSelectionRequest extends LtiMessageType("ContentItemSelectionRequest")
  case object ContentItemSelection        extends LtiMessageType("ContentItemSelection")
