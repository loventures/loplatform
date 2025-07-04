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

package scaloi

import scalaz.syntax.std.boolean.*
import scaloi.syntax.option.*
import scalaz.{Applicative, Traverse, \/}

/** Wrapper around the result of a get-or-create operation which captures whether the object was just created.
  */
sealed abstract class GetOrCreate[T] extends Product with Serializable:

  /** Return the contained object.
    */
  def result: T

  /** Return whether the object was created.
    */
  def isCreated: Boolean

  /** Return whether the object was gotten.
    */
  def isGotten: Boolean = !isCreated

  /** Map the value.
    */
  def map[U](f: T => U): GetOrCreate[U] = GetOrCreate(f(result), isCreated)

  /** Run a side-effecting function on the value.
    */
  def foreach(f: T => Unit): Unit = f(result)

  /** Fold the value according to whether it was created or gotten.
    * @param cf
    *   the function to apply if created
    * @param gf
    *   the function to apply if gotten
    * @tparam U
    *   the result type
    * @return
    *   the result
    */
  def fold[U](cf: T => U)(gf: T => U): U =
    if isCreated then cf(result) else gf(result)

  /** Return the value as a right, if this was created, or else the supplied left.
    * @param left
    *   the left value
    * @tparam U
    *   the left type
    * @return
    *   the disjunction
    */
  def createdOr[U](left: => U): U \/ T = isCreated.either(result).or(left)

  /** Runs a side-effecting function on the value of this get-or-create if it was created, then returns this
    * get-or-create.
    * @param f
    *   the side-effecting function
    * @tparam A
    *   the result type, discarded
    * @return
    *   this get-or-create
    */
  def init[A](f: T => A): GetOrCreate[T] =
    if isCreated then f(result); this

  /** Runs a side-effecting function on the value of this get-or-create if it was gotten, then returns this
    * get-or-create.
    * @param f
    *   the side-effecting function
    * @tparam A
    *   the result type, discarded
    * @return
    *   this get-or-create
    */
  def update[A](f: T => A): GetOrCreate[T] =
    if isGotten then f(result); this

  /** Kestrel combinator on the value of a get-or-create.
    * @param f
    *   the side-effecting function
    * @tparam B
    *   the result type
    * @return
    *   the value
    */
  @inline final def always[B](f: T => B): T =
    f(result); result

  /** An alias for always.
    */
  @inline final def *<|[B](f: T => B): T = always(f)
end GetOrCreate

final case class Gotten[T](value: T) extends GetOrCreate[T]:
  override def result: T = value
  override def isCreated = false
object Gotten:
  def apply[T](value: T): GetOrCreate[T] = new Gotten(value)

final case class Created[T](value: T) extends GetOrCreate[T]:
  override def result: T = value
  override def isCreated = true
object Created:
  def apply[T](value: T): GetOrCreate[T] = new Created(value)

object GetOrCreate:
  def apply[T](t: T, created: Boolean): GetOrCreate[T] =
    if created then Created(t) else Gotten(t)

  /** Gets or creates an object. This is thread safe if all creators of entities in the collection use the same
    * {{lockCollection}} method. The lock need not be against an object of type [[T]].
    *
    * @param queryEntity
    *   a function that can return whether the object already exists (this must not return cached results)
    * @param createEntity
    *   a function to create the object if it does not
    * @param lockCollection
    *   a function to create a lock on the singleton collection of objects that all potential creators of the entity
    *   honor
    * @tparam T
    *   the type of object
    * @return
    *   the gotten or created object
    */
  def apply[T](
    queryEntity: () => Option[T],
    createEntity: () => T,
    lockCollection: () => Unit,
  ): GetOrCreate[T] =
    queryEntity() orElse {
      lockCollection()
      // Query again to see if someone else had created it before we locked (race condition)
      queryEntity()
    } orCreate {
      createEntity()
    }

  /** Factory method for objects that were got and not created.
    */
  def gotten[T](t: T): GetOrCreate[T] = Gotten(t)

  /** Factory method for objects that were created.
    */
  def created[T](t: T): GetOrCreate[T] = Created(t)

  implicit val traversableInstance: Traverse[GetOrCreate] = new Traverse[GetOrCreate]:
    override def map[A, B](fa: GetOrCreate[A])(f: A => B): GetOrCreate[B] = fa `map` f

    override def traverseImpl[G[_], A, B](
      fa: GetOrCreate[A]
    )(f: A => G[B])(implicit ap: Applicative[G]): G[GetOrCreate[B]] =
      fa match
        case Gotten(a)  =>
          ap.map(f(a))(Gotten(_))
        case Created(a) =>
          ap.map(f(a))(Created(_))
end GetOrCreate
