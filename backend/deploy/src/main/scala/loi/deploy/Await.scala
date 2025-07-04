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

import cats.effect.IO

import java.util.concurrent.CountDownLatch
import scala.annotation.tailrec

sealed abstract class Await:
  def block: IO[Unit]
  def trigger: IO[Unit]

object Await:

  def apply(): Await = new Await:

    val latch = new CountDownLatch(1)

    override def block = IO async_ { cb =>
      new Thread(
        () =>
          @tailrec def loop(): Unit =
            try latch.await()
            catch case _: InterruptedException => loop()
          loop()
          cb(Right(()))
        ,
        "Patiently-Awaiting"
      ).start()
    }

    override def trigger = IO { latch.countDown() }
end Await
