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

package loi.cp.lti.outcomes

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.MimeUtils
import de.tomcat.juli.LogMeta
import loi.asset.lti.Lti
import loi.cp.content.CourseContent
import loi.cp.course.{CourseSection, CourseSectionService}
import loi.cp.lti.outcomes.LtiOutcomesParser.*
import loi.cp.lti.{GradeTarget, LtiWebUtils}
import loi.cp.ltitool.{LtiLaunchConfiguration, LtiToolLaunchService}
import loi.cp.reference.EdgePath
import loi.cp.user.LightweightUserService
import loi.net.oauth.server.OAuthServlet
import loi.net.oauth.{OAuthAccessor, OAuthConsumer, SimpleOAuthValidator}
import org.imsglobal.lti.*

import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

import java.io.StringWriter
import javax.xml.bind.{JAXB, JAXBElement}
import scala.compat.java8.OptionConverters.*
import scala.util.control.NonFatal

@Component
@ServletBinding(path = LtiToolLaunchService.LTI_OUTCOMES_URL)
class LtiOutcomesServlet(
  val componentInstance: ComponentInstance,
  courseSectionService: CourseSectionService,
  ltiOutcomesParser: LtiOutcomesParser,
  consumerOutcomesService: LtiConsumerOutcomesService,
  componentEnvironment: ComponentEnvironment,
  userService: LightweightUserService,
  ltiWebUtils: LtiWebUtils
) extends ServletComponent
    with ComponentImplementation:

  import LtiOutcomesServlet.*
  import argonaut.*
  import Argonaut.*

  override def service(request: HttpServletRequest, resp: HttpServletResponse): WebResponse =
    val requestBody  = request.body
    LogMeta.let("authorization" := request.getHeader("Authorization"), "request" := requestBody)(
      logger.info("LTI Outcomes request")
    )
    val jaxbResponse = ltiOutcomesParser.parseOutcome(requestBody) match
      case Left(error)            =>
        // responseId intentionally empty, since the request couldn't be parsed
        logger.warn(s"Error parsing BasicOutcomesRequest, cause: $error, request body was: $requestBody")
        response(None).buildFailureEnvelope("Invalid request", a => {})
      case Right(outcomesRequest) =>
        processRequest(request, requestBody, outcomesRequest)

    val writer = new StringWriter()
    JAXB.marshal(jaxbResponse, writer)
    LogMeta.let("response" := writer.toString)(logger.info("LTI Outcomes response"))

    resp.setContentType(MimeUtils.MIME_TYPE_APPLICATION_XML + MimeUtils.CHARSET_SUFFIX_UTF_8)
    resp.getWriter.write(writer.toString)

    NoResponse
  end service

  def processRequest(request: HttpServletRequest, requestBody: String, req: BasicOutcomesRequest): OutcomesResponse =
    req match
      case readResult @ ReadResult(reqId, sourcedId) =>
        validateSourcedId(request, sourcedId)(read(reqId, _))
          .handleError(req, requestBody, body => body.setReadResultResponse(new ReadResultResponse))

      case deleteResult @ DeleteResult(reqId, sourcedId) =>
        validateSourcedId(request, sourcedId)(delete(reqId, _))
          .handleError(req, requestBody, body => body.setDeleteResultResponse(new DeleteResultResponse))

      case replaceResult @ ReplaceResult(reqId, sourcedId, score) =>
        validateSourcedId(request, sourcedId)(replace(reqId, _, score))
          .handleError(req, requestBody, body => body.setReplaceResultResponse(new ReplaceResultResponse))

  def validateSourcedId[A](req: HttpServletRequest, sourcedId: String)(
    f: GradeTarget => ProcessResult[A]
  ): ProcessResult[A] =
    for
      gradeSourcedId <- ltiOutcomesParser.parseSourcedId(sourcedId)
      section        <- findSection(gradeSourcedId.contextId)
      content        <- findContent(section, gradeSourcedId.edgePath)
      student        <- findStudent(gradeSourcedId.studentId)
      _              <- validateRequest(req, content)
      a              <- f(GradeTarget(student, section, content))
    yield a

  def read(requestId: Option[String], gradeTarget: GradeTarget): ProcessResult[OutcomesResponse] =
    val responseBuilder = response(requestId)
    consumerOutcomesService
      .readGrade(gradeTarget)
      .map(responseBuilder.buildReadResultSuccessEnvelope)

  def delete(requestId: Option[String], gradeTarget: GradeTarget): ProcessResult[OutcomesResponse] =
    val responseBuilder = response(requestId)
    consumerOutcomesService
      .deleteGrade(gradeTarget)
      .map(_ => responseBuilder.buildDeleteResultSuccessEnvelope())

  def replace(requestId: Option[String], gradeTarget: GradeTarget, score: String): ProcessResult[OutcomesResponse] =
    val responseBuilder = response(requestId)
    consumerOutcomesService
      .replaceGrade(gradeTarget, score)
      .map(_ => responseBuilder.buildReplaceResultSuccessEnvelope())

  def validateRequest(req: HttpServletRequest, content: CourseContent): ProcessResult[Unit] =
    for
      (key, secret) <- getKeyAndSecret(content)
      _             <- verifyOauth10aSignature(req, key, secret)
    yield ()

  def verifyOauth10aSignature(req: HttpServletRequest, key: String, secret: String): ProcessResult[Unit] =
    def config: LtiOutcomesConfiguration =
      componentEnvironment
        .getJsonConfiguration[LtiOutcomesConfiguration](
          classOf[LtiOutcomesServlet].getName,
          classOf[LtiOutcomesConfiguration]
        )
        .asScala
        .getOrElse(LtiOutcomesConfiguration())
    if config.requireAuthorization then
      try
        val oam  = OAuthServlet.getMessage(req, OAuthServlet.getRequestURL(req))
        val oav  = new SimpleOAuthValidator
        val cons = new OAuthConsumer(null, key, secret, null)
        val acc  = new OAuthAccessor(cons)
        oav.validateMessage(oam, acc)
        Right(())
      catch
        case NonFatal(e) =>
          logger.warn(e)(s"Error validating message")
          Left(s"Could not verify the oauth signature of this request")
// https://github.com/1EdTech/basiclti-util-java by a certain Mr pfgray
//      val verifier: LtiVerifier = new LtiOauthVerifier()
//      val result                = verifier.verify(req, secret)
//      if (!result.getSuccess) {
//
//        Left(s"Could not verify the oauth signature of this request, ${result.getError}")
//      } else {
//        Right(())
//      }
    else Right(())
    end if
  end verifyOauth10aSignature

  def findSection(contextId: Long): ProcessResult[CourseSection] =
    courseSectionService.getCourseSection(contextId).toRight(s"Could not find course for context id: $contextId")

  def findContent(section: CourseSection, edgePath: EdgePath): ProcessResult[CourseContent] =
    section.contents.get(edgePath).toRight(s"Could not find activity for edgePath: $edgePath in course: ${section.id}")

  def findStudent(userId: Long): ProcessResult[UserDTO] =
    userService.getUserById(userId).toRight(s"no such user $userId")

  def getKeyAndSecret(content: CourseContent): ProcessResult[(String, String)] =
    for
      ltiAsset     <-
        content.asset.filter[Lti].toRight(s"Activity with edgePath: ${content.edgePath} is not an LTI activity")
      ltiTool      <- ltiWebUtils
                        .getTool(ltiAsset.data.lti.toolId)
                        .toRight(s"Could not find Lti Tool configuration for content: ${content.edgePath}")
      keyAndSecret <- getKeyAndSecret(
                        ltiAsset.data.lti.toolConfiguration.applyDefaultLtiConfig(ltiTool.getLtiConfiguration)
                      )
    yield keyAndSecret

  def getKeyAndSecret(config: LtiLaunchConfiguration): ProcessResult[(String, String)] =
    for
      key    <- config.key.toRight(misconfiguredMessage)
      secret <- config.secret.toRight(misconfiguredMessage)
    yield (key, secret)

  def misconfiguredMessage = s"Lti activity for this outcome is misconfigured"

end LtiOutcomesServlet

object LtiOutcomesServlet:

  type OutcomesResponse = JAXBElement[ImsxPOXEnvelopeType]

  final val logger = org.log4s.getLogger

  private def response(requestId: Option[String]) = new LTIOutcomesResponseBuilder(requestId.getOrElse(""))

  implicit class EnvelopeProcessResultOps(result: ProcessResult[OutcomesResponse]):

    /** Constructs an error request in the case of an error during processing, returns the existing response if there is
      * no error
      * @param request
      *   The request which was being handled
      * @param sideEffect
      *   a side affect to apply to the body. This is necessary, because of the API of ImsxPOXBodyType, and IMS
      *   conformance testing will fail us unless we respond appropriately for the different request types
      * @return
      */
    def handleError(
      request: BasicOutcomesRequest,
      requestBody: String,
      sideEffect: ImsxPOXBodyType => Unit
    ): JAXBElement[ImsxPOXEnvelopeType] =
      result match
        case Left(error) =>
          logger.warn(
            s"Error processing parsed BasicOutcomesRequest: $request, cause: $error, request body was: $requestBody"
          )
          response(request.id).buildFailureEnvelope(error, ImsxCodeMajorType.FAILURE, body => sideEffect(body))
        case Right(body) =>
          logger.info(s"Successfully Processed BasicOutcomesRequest: $request, request body was: $requestBody")
          body
  end EnvelopeProcessResultOps
end LtiOutcomesServlet
