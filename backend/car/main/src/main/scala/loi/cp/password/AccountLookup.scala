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

package loi.cp.password

import org.apache.pekko.pattern.AskTimeoutException
import org.apache.pekko.util.Timeout
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainWebService}
import com.learningobjects.cpxp.util.ManagedUtils
import loi.cp.throttle.ThrottleActor
import loi.cp.throttle.ThrottleActor.{ThrottleRequest, ThrottleResponse}
import scalaz.syntax.std.boolean.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Future, blocking}

/** Support for throttled account lookup. */
object AccountLookup:
  private final val logger = org.log4s.getLogger

  def runThrottled[A](f: => A)(implicit domain: DomainDTO, dws: DomainWebService): Future[Option[A]] =
    for
      count  <- countRecentAttempts(s"AccountLookup-${domain.id}")
      ok     <- backoff(count)
      answer <- Future(ok option perform(f))
    yield answer

  /** Count the number of recent password recovery attempts. */
  private def countRecentAttempts(throttleKey: String): Future[Int] =
    ThrottleActor.localActor
      .askFor[ThrottleResponse](ThrottleRequest(throttleKey, RecoveryAttemptWindow.toMinutes))
      .map(_.count) recover { case _: AskTimeoutException =>
      0 // if pekko fails then just allow recovery
    }

  /** Either fail the request or backoff depending on the number of recent requests. */
  private def backoff(count: Int): Future[Boolean] =
    if count > MaxRecoveryAttempts then
      // TODO: Should this trigger some manner of alert?
      logger warn s"Password recovery rejected because of $count attempts in $RecoveryAttemptWindow"
      Future.successful(false)
    else delay(Backoff * count, true)

  /** Delay for an amount of time. */
  private def delay[T](amount: FiniteDuration, t: T): Future[T] =
    Future {
      blocking {
        logger warn s"Delaying password recovery by $amount"
        Thread.sleep(amount.toMillis)
        t
      }
    }

  /** The pekko response timeout. */
  private implicit val PekkoTimeout: Timeout = Timeout(5.seconds)

  /** The maximum number of recovery attempts to permit in a given time window. */
  private final val MaxRecoveryAttempts = 100

  /** The recovery attempt window. */
  private final val RecoveryAttemptWindow = 10.minutes

  /** The backoff amount. Every request within a window will cause subsequent requests to be delayed by this amount. */
  private final val Backoff = 100.millis

  private def perform[A](f: => A)(implicit domain: DomainDTO, dws: DomainWebService): A =
    ManagedUtils perform { () =>
      dws.setupContext(domain.id)
      f
    }
end AccountLookup
