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

package loi.cp.script

import enumeratum.*

import scala.collection.immutable.IndexedSeq

private[script] sealed trait SqlCharState extends EnumEntry:
  import SqlCharState.*

  /*
    States: https://stash.example.org/projects/DE/repos/bfr/attachments/aebf4fe9cf/SqlCharStateFsm.svg

    Conditions in order of priority:
      c0: c == ';'
      c1: c == '-'
      c2: c == '\'' || c == '"'
      c3: c == char.from.c2
      c4: c != char.from.c2
      c5: c == '\n'
      c6: true
   */
  def next(c: Char): SqlCharState =
    val maybeNext: Option[SqlCharState] = this match
      case Statement | Start | PreComment | Separator | StringEnd =>
        if isSeparator(c) then Some(Separator)
        else if isDash(c) && this == PreComment then Some(Comment)
        else if isDash(c) then Some(PreComment)
        else if StrChars.contains(c) then Some(StringBody(c))
        else None
      case StringBody(s) if c == s                                => Some(StringEnd)
      case _                                                      => None

    val nextS = maybeNext.getOrElse(
      this match
        case Start | StringEnd | PreComment => Statement
        case Comment if isEol(c)            => Statement
        case Separator if !isSeparator(c)   => Statement
        case _                              => this
    )
    nextS
  end next

  private def isSeparator(c: Char) = c == ';'
  private def isDash(c: Char)      = c == '-'
  private def isEol(c: Char)       = c == '\n'

  private val StrChars = "'\""
end SqlCharState

private[script] object SqlCharState extends Enum[SqlCharState]:
  override val values: IndexedSeq[SqlCharState] = findValues

  case object Start              extends SqlCharState
  case object Separator          extends SqlCharState
  case object PreComment         extends SqlCharState
  case object Comment            extends SqlCharState
  case class StringBody(s: Char) extends SqlCharState
  case object StringEnd          extends SqlCharState
  case object Statement          extends SqlCharState
end SqlCharState
