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

package loi.cp.user

import com.github.tototoshi.csv.CSVReader
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.{Comparison, Projection, QueryService, Function as QBFunction}
import com.learningobjects.cpxp.service.user.RestrictedLearnerFinder
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.UserAdminRight
import scalaz.\/
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource

import java.util.Date
import scala.util.Using

/** A restricted learner is a student with limited collaboration access in the system: No access to discussion boards,
  * etc.
  *
  * Restricted learner email addresses are bulk uploaded into the system prior to student access and stored in
  * [RestrictedLearnerFinder].
  */
@Controller(value = "restrictedLearners", root = true, category = Controller.Category.USERS)
@RequestMapping(path = "restrictedLearners")
@Secured(Array(classOf[UserAdminRight]))
@Component
class RestrictedLearnerWebController(
  val componentInstance: ComponentInstance,
  domain: DomainDTO,
  now: TimeSource
)(implicit is: ItemService, qs: QueryService, ontology: Ontology)
    extends ApiRootComponent
    with ComponentImplementation:
  @DeIgnore
  protected def this() = this(null, null, null)(using null, null, null)

  @RequestMapping(method = Method.GET)
  def getRestrictedLearners(query: ApiQuery): ApiQueryResults[RestrictedLearner] =
    ApiQueries
      .queryFinder[RestrictedLearnerFinder](domain.queryAll[RestrictedLearnerFinder], query)
      .map(RestrictedLearner.apply)

  @RequestMapping(path = "{id}", method = Method.GET)
  def getRestrictedLearner(@PathVariable("id") id: Long): Option[RestrictedLearner] =
    getRestrictedLearners(ApiQuery.byId(id, classOf[RestrictedLearnerFinder])).asOption

  @RequestMapping(path = "{id}", method = Method.DELETE)
  def deleteRestrictedLearner(@PathVariable("id") id: Long): ErrorResponse \/ Unit =
    for learner <- getRestrictedLearner(id) \/> ErrorResponse.notFound
    yield is.destroy(is.get(learner.id, RestrictedLearnerFinder.ItemType))

  @RequestMapping(path = "upload", method = Method.POST)
  def uploadRestrictedLearners(@RequestBody upload: UploadInfo): Int =
    Using.resource(CSVReader.open(upload.getFile)) { csv =>
      val results = for
        row   <- csv.all()
        email <- row.headOption
        if email.contains('@') && !RestrictedLearner.isRestricted(domain, email)
      yield domain.addChild[RestrictedLearnerFinder] { f =>
        f.email = email
        f.created = now.date
      }
      results.length
    }
end RestrictedLearnerWebController

final case class RestrictedLearner(
  id: Long,
  email: String,
  created: Date,
)

object RestrictedLearner:
  def apply(finder: RestrictedLearnerFinder): RestrictedLearner =
    new RestrictedLearner(finder.id, finder.email, finder.created)

  /** Return whether the email is restricted in the domain. */
  def isRestricted(domain: DomainDTO, email: String)(implicit qs: QueryService): Boolean =
    domain
      .queryAll[RestrictedLearnerFinder]
      .addCondition(RestrictedLearnerFinder.Email, Comparison.eq, email, QBFunction.LOWER)
      .setProjection(Projection.ROOT_ID)
      .setLimit(1)
      .optionResult[Long]
      .isDefined
end RestrictedLearner
