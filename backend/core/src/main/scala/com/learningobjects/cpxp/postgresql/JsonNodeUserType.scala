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

package com.learningobjects.cpxp.postgresql

import io.hypersistence.utils.hibernate.`type`.json.JsonType
import io.hypersistence.utils.hibernate.`type`.util.ObjectMapperWrapper

/** Adds our legacy behaviour of stripping nulls (that used to come in on various authoring imports) and bizarre null
  * handling.
  */
class JsonNodeUserType extends JsonType(new SanitizingObjectMapperWrapper)

class SanitizingObjectMapperWrapper extends ObjectMapperWrapper:
  // We used to turn NullNode/MissingNode into database nulls but I don't buy that
  // this is a real thing we should do anymore. It has some moral validity to it,
  // in that we can then have a database null value that IS NOT NULL, but...
  override def toString(value: AnyRef): String = clean(super.toString(value))

  // for jsonb column, explosion on insert if unicode NULL chars are present
  // for json column, insert succeeds, but most postgres json functions fail later on
  private def clean(value: String): String = value.replaceAll("(?<=[^\\\\])\\\\u0000", "")
