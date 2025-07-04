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

package loi.cp.bookmark

import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.annotation.{Component, Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.cpxp.PK.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.{Comparison, QueryService}
import com.learningobjects.cpxp.service.user.{BookmarksFinder, UserDTO}
import loi.cp.content.ContentAccessService
import loi.cp.reference.EdgePath
import scalaz.std.map.*
import scalaz.syntax.std.boolean.*
import scaloi.syntax.all.*

import javax.ws.rs.InternalServerErrorException
import scala.jdk.OptionConverters.*
import scala.util.Try

// Bookmarks are tied to the course branch rather than the course itself, to
// facilitate lossless transfer of students among sections. A dedicated entity
// is used rather than the course storage service, because the latter is rather
// heavily abused by the LTI machinery.
@Controller(value = "lwc-bookmarks", root = true)
@Component
class BookmarksWebController(val componentInstance: ComponentInstance)(
  user: UserDTO,
  domain: => DomainDTO,
)(implicit cas: ContentAccessService, qs: QueryService, is: ItemService, ontology: Ontology)
    extends ApiRootComponent
    with ComponentImplementation:

  import BookmarksWebController.*;

  @RequestMapping(path = "bookmarks/{context}", method = Method.GET)
  def getBookmarks(
    @PathVariable("context") context: Long,
  ): Try[Bookmarks] =
    for
      course <- cas.getCourseAsLearner(context, user)
      branch <- course.getBranchId.toScala.toTry(new InvalidRequestException("Course has no branch"))
    yield loadBookmarks(branch).foldZ(_.bookmarks.jdecode[Bookmarks].getOr(Map.empty))

  @RequestMapping(path = "bookmarks/{context}/{edgePath}", method = Method.PUT)
  def addBookmark(
    @PathVariable("context") context: Long,
    @PathVariable("edgePath") edgePath: EdgePath,
    @RequestBody note: String,
  ): Try[Unit] =
    for
      course   <- cas.getCourseAsLearner(context, user)
      branch   <- course.getBranchId.toScala.toTry(new InvalidRequestException("Course has no branch"))
      existing <- loadBookmarksForUpdate(branch)
    yield existing match
      case Some(finder) =>
        val bookmarks = finder.bookmarks.jdecode[Bookmarks].getOr(Map.empty)
        finder.bookmarks = (bookmarks + (edgePath -> note)).asJson

      case None =>
        user.addChild[BookmarksFinder] { finder =>
          finder.branch = branch
          finder.bookmarks = Map(edgePath -> note).asJson
        }

  @RequestMapping(path = "bookmarks/{context}/{edgePath}", method = Method.DELETE)
  def removeBookmark(
    @PathVariable("context") context: Long,
    @PathVariable("edgePath") edgePath: EdgePath,
  ): Try[Unit] =
    for
      course <- cas.getCourseAsLearner(context, user)
      branch <- course.getBranchId.toScala.toTry(new InvalidRequestException("Course has no branch"))
    yield loadBookmarks(branch) foreach { finder =>
      val bookmarks = finder.bookmarks.jdecode[Bookmarks].getOr(Map.empty)
      finder.bookmarks = (bookmarks - edgePath).asJson
    }

  def loadBookmarksForUpdate(branch: Long): Try[Option[BookmarksFinder]] =
    loadBookmarks(branch) match
      case None =>
        is.pessimisticLock(user.item) either loadBookmarks(
          branch,
          cached = false
        ) `orFailure` new InternalServerErrorException("Lock failure")
      case some =>
        some.success

  def loadBookmarks(branch: Long, cached: Boolean = true): Option[BookmarksFinder] =
    user
      .queryChildren[BookmarksFinder]
      .addCondition(BookmarksFinder.Branch, Comparison.eq, branch)
      .setCacheQuery(cached)
      .getFinder[BookmarksFinder]
end BookmarksWebController

object BookmarksWebController:
  type Bookmarks = Map[EdgePath, String]
