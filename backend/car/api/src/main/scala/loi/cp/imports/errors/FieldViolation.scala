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

package loi.cp.imports.errors

import com.fasterxml.jackson.annotation.JsonTypeName
import com.learningobjects.cpxp.scala.util.I18nMessage

@JsonTypeName("FieldViolation")
case class FieldViolation(
  field: String,
  value: Object,
  message: I18nMessage,
  _type: String = "FieldViolation"
) extends Violation

object FieldViolation:
  def apply(field: String, value: AnyRef, messageKey: String): Violation =
    FieldViolation(field, value, I18nMessage(messageKey))
