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

package loi.cp.tx

import cats.effect.IO
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainWebService}
import com.learningobjects.cpxp.util.ManagedUtils

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

/** Support for running an IO on a single thread bracketed by transaction setup and teardown which allows component
  * environment and hibernate things to operate happily.
  */
object DEIEIO:
  def tx[A](ioa: IO[A])(implicit domain: DomainDTO, dws: DomainWebService): IO[A] =
    // because we rely on thread locals for things to work
    // we can't shift on to the wrong thread
    // if there's only one thread.
    val singleThread = Executors.newSingleThreadExecutor()

    IO.delay({
      ManagedUtils.begin()
      dws.setupContext(domain.id)
    }).bracketCase(_ => ioa)((_, outcome) =>
      IO.delay({
        if !outcome.isSuccess then ManagedUtils.setRollbackOnly()
        ManagedUtils.end()
      })
    ).guarantee(IO.delay(singleThread.shutdown()))
      .evalOn(ExecutionContext.fromExecutor(singleThread))
  end tx
end DEIEIO
