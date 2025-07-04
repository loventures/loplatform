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

package scaloi.free.tx

import scalaz.*

/** A Purely functional API for demarcating transaction boundaries. The `TxOP` ADT expresses a high level operations for
  * those boundaries.
  */
trait Transactor[T]:

  /** An ADT for representing transactional operations using JTA semantics.
    */
  sealed trait TxOp[A]

  /** Begin a Transaction, creating a value which represents the transaction to evaluate over.
    */
  case object Begin extends TxOp[Transaction]

  /** Commit the given transaction.
    */
  case class Commit(t: Transaction) extends TxOp[Unit]

  /** Rollback the given transaction.
    */
  case class Rollback(t: Transaction) extends TxOp[Unit]

  /** An identity for a suspended transaction.
    */
  case class Transaction(id: Long, underlying: T)

  /** A DSL for expressing transaction boundaries over a Transactor.
    */
  final class Tx[F[_]](implicit I: TxOp :<: F):

    private def lift[A](tx: TxOp[A]) = Free.liftF(I(tx))

    type TxIO[A] = Free[F, A]

    /** Perform some side effect to begin a transaction.
      */
    def begin: TxIO[Transaction] = lift(Begin)

    /** Commit the active transaction. Active is nebulously defined via side effects.
      */
    def commit(transaction: Transaction): TxIO[Unit] = lift(Commit(transaction))

    /** Rollback the active transaction.
      */
    def rollback(transaction: Transaction): TxIO[Unit] = lift(Rollback(transaction))

    /** Evaluate the given expression between the boundaries of a transaction. If the exception occurs when evaluating
      * the expression, the transaction is rolled back, else it is commited.
      */
    def perform[A](a: T => A): TxIO[Throwable \/ A] =
      for
        transaction <- begin
        aMaybe       = \/.attempt(a(transaction.underlying))(identity)
        _           <- aMaybe.fold(_ => rollback(transaction), _ => commit(transaction))
      yield aMaybe

    /** Evaluate the given transactional effect, and perform the recovery effect given a -\/ value.
      */
    def recover[L, R](attempt: TxIO[L \/ R], recover: L => TxIO[L \/ R])(implicit TxIO: Monad[TxIO]): TxIO[L \/ R] =
      for
        maybe   <- attempt
        recover <- maybe.fold(recover, _ => TxIO.point(maybe))
      yield recover

    /** Given a transactional effect, repeat the effect in the event of failure, up to the given number attempts and
      * given that the exception meets the criteria of pred.
      *
      * TODO: Maybe implement a MonadError[Tx]?
      *
      * @return
      */
    def retry[A](
      attempts: Int
    )(pred: Throwable => Boolean)(tx: TxIO[Throwable \/ A])(implicit TxIO: Monad[TxIO]): TxIO[Throwable \/ A] =
      tx.flatMap(attempt =>
        if attempts > 0 && attempt.isLeft && attempt.swap.forall(pred) then retry(attempts - 1)(pred)(tx)
        else TxIO.point(attempt)
      ) // Applicative Syntax?

    /** Evaluate the given list of disjunctions until a left(error) value is encountered.
      */
    def attemptOrdered[L, R](txs: List[TxIO[L \/ R]])(implicit TxIO: Monad[TxIO]): TxIO[L \/ R] =
      txs.reduce((a, b) => a.flatMap(aa => aa.fold(_ => TxIO.point(aa), _ => b)))
  end Tx
end Transactor

/** A pure transactor that performs no side effects. Useful for testing the effects of different Tx abstractions.
  */
object UnitTransactor extends Transactor[Unit]:

  /** Evaluate the transaction operations to their identity values.
    */
  val evalId = new (TxOp ~> Id.Id):
    override def apply[A](fa: TxOp[A]): Id.Id[A] = fa match
      // case Begin       => unitTx
      case Commit(_)   => ()
      case Rollback(_) => ()

  /** The unit transaction value.
    */
  val unitTx = Transaction(1L, ())
end UnitTransactor
