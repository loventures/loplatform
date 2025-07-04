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

import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight

import scala.concurrent.Future
import scala.util.Try

/** SRS test API.
  */
@Controller(value = "srs", root = true)
@RequestMapping(path = "srs")
@Secured(Array(classOf[AdminRight]))
@VisibleForTesting
trait SrsRootApi extends ApiRootComponent:

  /** Get a thing from the system in the future.
    * @param thing
    *   the thing
    * @return
    *   the future thing
    */
  @RequestMapping(path = "future", method = Method.GET)
  def future(@QueryParam("thing") thing: String): Future[SrsThing]

  /** Fail to get a thing from the system in the future. Tests the handling of exceptions from a future.
    * @param thing
    *   the thing
    * @return
    *   exception
    */
  @RequestMapping(path = "futureFail", method = Method.GET)
  def futureFail(@QueryParam("thing") thing: String): Future[SrsThing]

  /** Timeout waiting to get a thing from the system in the future. Tests the handling of timeouts from a future.
    * @param thing
    *   the thing
    * @return
    *   the future thing, after a long time
    */
  @RequestMapping(path = "futureTimeout", method = Method.GET)
  def futureTimeout(@QueryParam("thing") thing: String): Future[SrsThing]

  /** Succeed. */
  @RequestMapping(path = "trySuccess", method = Method.GET)
  def trySuccess(@QueryParam("thing") thing: String): Try[SrsThing]

  /** Fail. */
  @RequestMapping(path = "tryFailure", method = Method.GET)
  def tryFailure(@QueryParam("thing") thing: String): Try[SrsThing]
end SrsRootApi

/** A test entity.
  * @param thing
  *   a value
  */
case class SrsThing(thing: String)
