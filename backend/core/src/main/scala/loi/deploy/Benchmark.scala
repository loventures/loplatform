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

package loi.deploy

import cats.*
import cats.data.WriterT
import cats.effect.*
import cats.instances.list.*
import cats.syntax.all.*

object Benchmark:
  type Bench[F[_], A] = WriterT[F, List[TimedRun], A]

  def printGraph[F[_]: Sync, A](bfa: Bench[F, A]): Bench[F, A] =
    WriterT(for
      (runs, a) <- bfa.run
      graph      = TimedRun.graph(runs.sortBy(_.start), width = 40)
      _         <- Sync[F].delay(println(graph.mkString("\n")))
    yield (runs, a))

  def apply[F[_]: Monad, A](ta: F[A], name: String)(implicit clock: Clock[F]): Bench[F, A] =
    val run = for
      start <- clock.monotonic
      a     <- ta
      end   <- clock.monotonic
    yield (List(TimedRun(start.toNanos, end.toNanos, name)), a)
    WriterT(run)

  def liftF[F[_]: Functor, A](ta: F[A]): Bench[F, A] = WriterT(ta.map(a => (List.empty, a)))
  def delay[F[_]: Sync, A](a: => A): Bench[F, A]     = liftF(Sync[F].delay(a))

  val never: Bench[IO, Nothing] = Benchmark.liftF(IO.never)

  implicit final class BenchOps[F[_], A](val ta: F[A]) extends AnyVal:
    def bench(name: String)(implicit F: Monad[F], C: Clock[F]): Bench[F, A] = Benchmark(ta, name)

  implicit def parallelBench[O[_], PF[_]](implicit
    PA: Parallel.Aux[O, PF]
  ): Parallel.Aux[[X] =>> Bench[O, X], [X] =>> Bench[PF, X]] =
    new Parallel[[X] =>> Bench[O, X]]:
      override type F[x] = Bench[PF, x]
      type M[x]          = Bench[O, x]

      override def applicative: Applicative[F] =
        WriterT.catsDataApplicativeForWriterT[PF, List[TimedRun]](using PA.applicative, implicitly)
      override def monad: Monad[M]             = WriterT.catsDataMonadForWriterT[O, List[TimedRun]](using PA.monad, implicitly)

      override def sequential: F ~> M = new (F ~> M):
        override def apply[A](bpf: F[A]): M[A] = WriterT(PA.sequential(bpf.run))
      override def parallel: M ~> F   = new (M ~> F):
        override def apply[A](bf: M[A]): F[A] = WriterT(PA.parallel(bf.run))

  // A scalaz functor to support the fauxnad asserting that any `withFilter` is idempotent
  implicit def zunctor[F[_]: Functor]: scalaz.Functor[F] = new scalaz.Functor[F]:
    override def map[A, B](fa: F[A])(ab: A => B): F[B] = implicitly[Functor[F]].map(fa)(ab)
end Benchmark

case class TimedRun(start: Long, end: Long, name: String)
object TimedRun:

  def graph(benchMarks: List[TimedRun], width: Int = 16, active: Char = '*', sleep: Char = '-'): List[String] =

    val startTime = benchMarks.map(_.start).min
    val endTime   = benchMarks.map(_.end).max
    val delta     = endTime - startTime

    List(s"Total time: $delta ns (${"%1$,.2f".format(delta.toDouble / 1000000000)} seconds)") ++ (benchMarks map {
      benchmark =>
        val lowerbound = (benchmark.start - startTime).toDouble / delta.toDouble
        val upperbound = (benchmark.end - startTime).toDouble / delta.toDouble
        val row        =
          for i <- 1 to width
          yield
            val percentage = i.toDouble / width.toDouble
            if percentage >= lowerbound && percentage <= upperbound then active else sleep
        val duration   = benchmark.end - benchmark.start
        s"${row.mkString}: ${benchmark.name} (${"%1$,.2f".format(duration.toDouble / 1000000000)} s)"
    })
  end graph
end TimedRun
