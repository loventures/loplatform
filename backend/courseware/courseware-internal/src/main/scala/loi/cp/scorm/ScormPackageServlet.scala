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

package loi.cp.scorm

import argonaut.Argonaut.*
import com.google.common.io.Resources
import com.google.common.net.MediaType
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.{Component, InheritResources}
import com.learningobjects.cpxp.component.web.Method.{GET, POST}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.util.OAuthUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse.{SC_OK, SC_FORBIDDEN as Forbidden}
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.lti.{
  FriendlyLtiError,
  InvalidLtiParameter,
  LightweightLtiServlet,
  LtiError,
  LtiErrorHandling,
  MissingLtiParameter
}
import org.apache.commons.codec.binary.Base64
import org.apache.http.HttpHeaders
import scalaz.\/
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.boolean.*
import scaloi.syntax.disjunction.*
import scaloi.syntax.option.*
import scaloi.syntax.ʈry.*

import java.nio.charset.StandardCharsets
import java.util.Properties
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.duration.*
import scala.util.Try

@Component
@ServletBinding(path = ScormPackageServlet.Path)
@InheritResources(classOf[LightweightLtiServlet])
class ScormPackageServlet(val componentInstance: ComponentInstance)(implicit
  componentService: ComponentService,
  scormPackageService: ScormPackageService,
  urlService: CurrentUrlService,
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation
    with LtiErrorHandling:

  import ScormPackageServlet.*
  import ServletDispatcher.*

  protected override val logger = org.log4s.getLogger

  override def handler: RequestHandler = {
    case RequestMatcher(GET, LaunchScriptPath(packageId), _, _) =>
      serveLaunchScript(packageId)

    case RequestMatcher(POST, LaunchPath, request, _) =>
      processLaunch(request) `leftFlatMap` errorHandler
  }

  /** Transforms an insecure SCORM launch link into a signed LTI launch. Allows us to avoid reimplementing everything in
    * LTI servlet and plausibly intermediate any LMS issues we discover.
    */
  private def processLaunch(request: HttpServletRequest): LtiError \/ WebResponse =
    for
      packageId   <- request.paramNZ("packageId") \/> MissingLtiParameter("packageId").widen
      pageId       = request.paramNZ("pageId")
      studentId   <- request.paramNZ("studentId") \/> MissingLtiParameter("studentId").widen
      studentName <- request.paramNZ("studentName") \/> MissingLtiParameter("studentName").widen
      sucurity    <- request.paramNZ("checksum") \/> MissingLtiParameter("checksum").widen
      checksum     = hmac(packageId, studentId :: studentName :: Nil)
      _           <- (sucurity == checksum) \/> InvalidLtiParameter("checksum", sucurity).widen
      рackage     <- getPackage(packageId) \/> FriendlyLtiError("scorm_package_unknown", packageId, Forbidden).widen
      system      <- getSystem(рackage.system) \/> FriendlyLtiError("scorm_system_unknown", packageId, Forbidden)
      oﬀering     <- getOffering(рackage.parent) \/> FriendlyLtiError("scorm_offering_unknown", packageId, Forbidden)
      _           <- рackage.disabled \/>! FriendlyLtiError("scorm_package_suspended", packageId, Forbidden)
      _           <- system.getDisabled \/>! FriendlyLtiError("scorm_connector_suspended", system.getSystemId, Forbidden)
      _           <- oﬀering.getDisabled \/>! FriendlyLtiError("scorm_offering_suspended", oﬀering.getGroupId, Forbidden)
    yield
      val (givenName, familyName) = splitName(studentName)
      val url                     = urlService.getUrl(s"/lwlti/offering/${oﬀering.getGroupId}${pageId.foldZ("/" + _)}")

      val properties = new Properties()
      properties.setProperty("user_id", studentId)
      properties.setProperty("lis_person_name_given", givenName)
      properties.setProperty("lis_person_name_family", familyName)
      properties.setProperty("context_id", packageId)     // one section per package for now
      properties.setProperty("roles", "Learner")
      properties.setProperty("custom_presentation_mode", pageId.isDefined.fold("obsequious", "meek"))
      properties.setProperty("custom_packaging", "scorm") // unused but perhaps useful in the logs

      val params = OAuthUtils.getOAuthParameters(url, system.getSystemId, system.getKey, properties).toMap
      autopost(url, params)

  /** Helpers to appease scalafmt. */
  private def getPackage(packageId: String) = scormPackageService.find(packageId)
  private def getSystem(systemId: Id)       = systemId.component_?[ScormSystem]
  private def getOffering(courseId: Id)     = courseId.component_?[LightweightCourse]

  /** Generate a keyed HMAC over a list of values. */
  private def hmac(secret: String, values: List[String]): String =
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"))
    Base64.encodeBase64String(mac.doFinal(values.mkString("|").getBytes(StandardCharsets.UTF_8)))

  /** Odious, and assumes that the John Paul Joneses of the world outnumber the Nick von Aaartsens. */
  private def splitName(name: String): (String, String) =
    name match
      case FullNameRE(givenName, familyName) => givenName -> familyName
      case simple                            => simple    -> simple // why not...

  /** Serves javascript that is responsible for rendering the SCORM player page in the origin LMS frame. This
    * implementation, rather than embedding the logic in the SCORM package, allows us to make changes to behaviour if
    * necessary without redistributing SCORM packages.
    */
  private def serveLaunchScript(packageId: String): EitherResponse =
    for
      url  <- Option(getClass.getResource("launch.js")) \/> ErrorResponse.serverError("error finding script")
      text <-
        Try(Resources.toString(url, StandardCharsets.UTF_8)) \/>| ErrorResponse.serverError("error reading script")
    yield
      val attributes = Map(
        "packageId" -> packageId,
        "launchUrl" -> urlService.getUrl(LaunchPath)
      )
      // Expand `$$variable` tokens as JSON
      val expanded   = VarRE.replaceAllIn(text, m => attributes.getOrElse(m.group(1), m.group(0)).asJson.nospaces)
      TextResponse(expanded, MediaType.JAVASCRIPT_UTF_8, SC_OK) + (HttpHeaders.CACHE_CONTROL -> s"max-age=$MaxAge")
end ScormPackageServlet

object ScormPackageServlet:
  final val Path             = "/scorm"
  final val LaunchPath       = s"$Path/launch"
  final val LaunchScriptPath = """/scorm/player/([^./]+)\.js""".r

  final val VarRE      = """\$\$([a-zA-Z]+)""".r
  final val FullNameRE = """(.+)\s+(\S+)""".r

  final val MaxAge = 12.hours.toSeconds
