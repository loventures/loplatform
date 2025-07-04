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

package com.learningobjects.cpxp.test

trait TestSymbols:
  final val _array      = Symbol("array")
  final val _boolean    = Symbol("boolean")
  final val _defined    = Symbol("defined")
  final val _disabled   = Symbol("disabled")
  final val _empty      = Symbol("empty")
  final val _enabled    = Symbol("enabled")
  final val _failure    = Symbol("failure")
  final val _fileName   = Symbol("fileName")
  final val _id         = Symbol("id")
  final val _intValue   = Symbol("intValue")
  final val _left       = Symbol("left")
  final val _length     = Symbol("length")
  final val _longValue  = Symbol("longValue")
  final val _name       = Symbol("name")
  final val _null       = Symbol("null")
  final val _number     = Symbol("number")
  final val _object     = Symbol("object")
  final val _present    = Symbol("present")
  final val _right      = Symbol("right")
  final val _size       = Symbol("size")
  final val _statusCode = Symbol("statusCode")
  final val _success    = Symbol("success")
  final val _textValue  = Symbol("textValue")
  final val _textual    = Symbol("textual")
  final val _type       = Symbol("type")
  final val _value      = Symbol("value")
end TestSymbols

object TestSymbols extends TestSymbols
