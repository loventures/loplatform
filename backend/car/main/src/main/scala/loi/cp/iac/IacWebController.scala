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

package loi.cp.iac
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.user.{UserDTO, UserFacade}
import loi.cp.accesscode.AccessCodeComponent
import loi.cp.content.ContentAccessService
import loi.cp.course.{CourseConfigurationService, CoursePreferences}
import scaloi.syntax.boolean.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import scala.jdk.CollectionConverters.*
import scala.util.Try

/** For acquiring an Individual Access Code associated with the course IAC Course Code. */
@Component
@Controller(root = true)
class IacWebController(
  val componentInstance: ComponentInstance,
  contentAccessService: ContentAccessService,
  courseConfigurationService: CourseConfigurationService,
  user: UserDTO,
  domain: DomainDTO,
)(implicit fs: FacadeService, qs: QueryService)
    extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(path = "lwc/{context}/iac/acquire", method = Method.POST)
  def acquireIac(
    @PathVariable("context") id: Long,
  ): Try[Option[String]] =
    for
      course <- contentAccessService.getCourseAsLearner(id, user)
      prefs   = course.getPreferences
      isbn   <- prefs.ltiISBN <@~* new InvalidRequestException("No LTI ISBN")
      _      <- user.facade[UserFacade].lock(0L) <@~* new Exception("Lock Failed")
    yield getAlreadyRedeemedAccessCode(isbn) orElse acquireNewAccessCode(isbn)

  @RequestMapping(path = "lwc/{context}/iac/courseKey", method = Method.PUT)
  def configureCourseKey(
    @PathVariable("context") id: Long,
    @RequestBody courseKey: String,
  ): Try[Unit] =
    for course <- contentAccessService.getCourseAsInstructor(id, user)
    yield
      val patch = JacksonUtils.getMapper.createObjectNode().put("ltiCourseKey", courseKey)
      courseConfigurationService.patchGroupConfig(CoursePreferences, course, patch)

  private def getAlreadyRedeemedAccessCode(isbn: String): Option[String] =
    qs
      .createNativeQuery(
        """SELECT
          |    c.accessCode
          |  FROM
          |    RedemptionFinder r,
          |    AccessCodeFinder c,
          |    AccessCodeBatchFinder b
          |  WHERE
          |    r.parent_id = :user AND
          |    c.id = r.accessCode_id AND
          |    b.id = c.batch_id AND
          |    b.componentId = :componentId AND
          |    b.json ->> 'isbn' = :isbn AND
          |    r.del IS NULL AND c.del IS NULL AND b.del IS NULL
          |""".stripMargin
      )
      .setParameter("user", user.id)
      .setParameter("componentId", classOf[IacAccessCodeBatch].getName)
      .setParameter("isbn", isbn)
      .getResultList
      .asScala
      .collectType[String]
      .headOption

  private def acquireNewAccessCode(isbn: String): Option[String] =
    for
      id        <- getUnredeemedAccessCodeId(isbn)
      accessCode = fs.getComponent(id, classOf[AccessCodeComponent])
    yield
      accessCode.redeem()
      accessCode.getAccessCode

  private def getUnredeemedAccessCodeId(isbn: String): Option[Long] =
    qs
      .createNativeQuery(
        """SELECT
          |    c.id
          |  FROM
          |    AccessCodeFinder c,
          |    AccessCodeBatchFinder b
          |  WHERE
          |    c.root_id = :domain AND
          |    c.redemptionCount = 0 AND
          |    b.id = c.batch_id AND
          |    b.componentId = :componentId AND
          |    NOT b.disabled AND
          |    b.json ->> 'isbn' = :isbn AND
          |    c.del IS NULL AND b.del IS NULL
          |  ORDER BY c.id ASC
          |  LIMIT 1
          |  FOR UPDATE
          |""".stripMargin
      )
      .setParameter("domain", domain.id)
      .setParameter("componentId", classOf[IacAccessCodeBatch].getName)
      .setParameter("isbn", isbn)
      .getResultList
      .asScala
      .collectType[Number]
      .headOption
      .map(_.longValue)
end IacWebController
