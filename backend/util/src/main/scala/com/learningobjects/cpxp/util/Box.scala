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

package com.learningobjects.cpxp.util

/** A mutable box of something. Think mutable option with fewer bells.
  *
  * See `scala.concurrent.SyncVar` which may be a better fit for your needs.
  *
  * @param value
  *   the initial value
  * @tparam T
  *   the box type
  */
class Box[T <: AnyRef] private (var value: T):

  /** Test whether this box is empty.
    *
    * @return
    *   if empty
    */
  final def isEmpty: Boolean = value eq null

  /** Test whether this box is non-empty.
    *
    * @return
    *   if non-empty
    */
  final def nonEmpty: Boolean = !isEmpty

  /** Convert this box to an option.
    *
    * @return
    *   the box as an option
    */
  final def toOption: Option[T] = Option(value)

  /** Run a function over the value, if present.
    */
  final def foreach[U](f: T => U): Unit = toOption.foreach(f)

  /** Returns whether a value is set that satisfies the predicate. */
  final def exists(p: T => Boolean): Boolean = toOption.exists(p)

  /** Returns whether a predicate holds for all contents of the box. */
  final def forall(p: T => Boolean): Boolean = toOption.forall(p)
end Box

/** Box companion.
  */
object Box:

  /** Create a box of something.
    *
    * @param t
    *   the thing to put in the box
    * @tparam T
    *   the type of the thing
    * @return
    *   a box with the thing in it
    */
  def of[T <: AnyRef](t: T): Box[T] = new Box(t)

  /** Create an empty box.
    *
    * @tparam T
    *   the type of the thing that could be put in the box
    * @return
    *   an empty box
    */
  def empty[T <: AnyRef]: Box[T] = new Box(null.asInstanceOf[T])
end Box

/** Extractor for empty boxen.
  */
object Empty:

  /** Matches an empty box. */
  def unapply(box: Box[?]): Boolean = box.isEmpty

/** Extractor for full boxen.
  */
object Full:

  /** Matches a non-empty box. */
  def unapply[T <: AnyRef](box: Box[T]): Option[T] = box.toOption
