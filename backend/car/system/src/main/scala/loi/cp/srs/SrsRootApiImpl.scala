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

package loi.cp.srs

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.exception.ValidationException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.{Failure, Success, Try}

/** Implementation of SRS testing API.
  */
@Component(enabled = false)
class SrsRootApiImpl(
  val componentInstance: ComponentInstance
) extends SrsRootApi
    with ComponentImplementation:
  import SrsRootApiImpl.*

  /** Return a thing in the a future.
    * @param thing
    *   the thing
    * @return
    *   the future thing
    */
  override def future(thing: String): Future[SrsThing] = Future {
    logger.info("Future start")
    Thread.sleep(1.seconds.toMillis)
    logger.info("Future done")
    SrsThing(thing)
  }

  /** Fail to return a thing in the future.
    * @param thing
    *   the thing
    * @return
    *   exception
    */
  override def futureFail(thing: String): Future[SrsThing] = Future {
    logger.info("Future start")
    Thread.sleep(1.second.toMillis)
    logger.info("Future fail")
    throw new ValidationException("thing", thing, "not a thing")
  }

  /** Timeout waiting to return a thing in the future.
    * @param thing
    *   the thing
    * @return
    *   exception
    */
  override def futureTimeout(thing: String): Future[SrsThing] = Future {
    logger.info("Future start")
    Thread.sleep(2.minutes.toMillis)
    logger.info("Future timeout")
    SrsThing(thing)
  }

  /** Succeed. */
  override def trySuccess(thing: String): Try[SrsThing] = Success(SrsThing(thing))

  /** Fail. */
  override def tryFailure(thing: String): Try[SrsThing] = Failure(
    new ValidationException("thing", thing, "not a thing")
  )
end SrsRootApiImpl

object SrsRootApiImpl:
  private val logger = org.log4s.getLogger
