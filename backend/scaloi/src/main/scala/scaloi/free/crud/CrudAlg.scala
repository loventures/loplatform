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

package scaloi.free.crud

import scalaz.Id.Id
import scalaz.{:<:, Free, ~>}

import scala.collection.mutable.Map as MMap

/** Created by zpowers on 3/25/17.
  */
sealed trait CrudAlg[Data, PK]:
  sealed trait CrudOp[T]
  case class Create(data: Data)         extends CrudOp[PK]
  case class Read(pk: PK)               extends CrudOp[Option[Data]]
  case class Update(pk: PK, data: Data) extends CrudOp[Data]
  case class Delete(pk: PK)             extends CrudOp[Unit]

  final class Crud[F[_]](implicit I: CrudOp :<: F):
    private def liftCrud[A](op: CrudOp[A]): Free[F, A] = Free.liftF(I(op))

    // Smart Constructors
    def create(data: Data): Free[F, PK]           = liftCrud(Create(data))
    def read(pk: PK): Free[F, Option[Data]]       = liftCrud(Read(pk))
    def update(pk: PK, data: Data): Free[F, Data] = liftCrud(Update(pk, data))
    def delete(pk: PK): Free[F, Unit]             = liftCrud(Delete(pk))

    // extra functions
    def getOrCreate(pk: PK, create: => Data): Free[F, Data] =
      for dataMaybe <- read(pk)
      yield dataMaybe.fold(create)(identity)
  end Crud

  final class MutableMapStore(init: => MMap[PK, Data], pkGenerator: MMap[PK, Data] => PK) extends (CrudOp ~> Id):
    private val store                                     = init
    override def apply[A](fa: CrudOp[A]): scalaz.Id.Id[A] = fa match
      case Create(data)     =>
        val newPK = pkGenerator(store)
        store.update(newPK, data)
        newPK
      case Read(pk)         =>
        store.get(pk)
      case Update(pk, data) =>
        store.update(pk, data)
        data
      case Delete(pk)       =>
        store.remove(pk)
        ()
  end MutableMapStore
end CrudAlg
object CrudAlg:
  implicit def apply[Data, PK]: CrudAlg[Data, PK] = new CrudAlg[Data, PK] {}
