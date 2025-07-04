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

package loi.cp.lti

import argonaut.Argonaut.*
import argonaut.*
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.util.RawHtml
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.scala.util.URIBuilderOps.*
import com.learningobjects.cpxp.service.user.UserDTO
import de.tomcat.juli.LogMeta
import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.asset.lti.Lti
import loi.authoring.edge.{EdgeAttrs, Group}
import loi.authoring.index.web.DcmPathUtils
import loi.authoring.project.AccessRestriction
import loi.authoring.web.AuthoringApiWebUtils
import loi.authoring.workspace.WriteWorkspace
import loi.authoring.workspace.service.WriteWorkspaceService
import loi.authoring.write.*
import loi.cp.content.ContentAccessService
import loi.cp.course.preview.{PreviewRole, PreviewService}
import loi.cp.lti.outcomes.LtiOutcomesParser.GradeSourcedId
import loi.cp.ltitool.LtiToolLaunchService.ResourceLink
import loi.cp.ltitool.{AssetLtiToolConfiguration, LtiLaunchConfiguration, LtiToolComponent, LtiToolLaunchService}
import loi.cp.reference.EdgePath
import org.apache.commons.text.StringEscapeUtils
import pdi.jwt.{Jwt, JwtBase64}
import scalaz.std.list.*
import scalaz.std.map.*
import scalaz.std.option.*
import scalaz.std.string.*
import scalaz.syntax.either.*
import scalaz.syntax.std.`try`.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.json.ArgoExtras
import scaloi.misc.TryInstances.*
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*

import java.util.UUID
import scala.util.Try

/** LTI 1.3 Api. The term "LTI Advantage" technically refers to the trio of Name/Role Services, Deep Linking, and
  * Assignment/Grade Services within LTI 1.3, but we use that as a proxy for the term 1.3 here.
  */
@Component
@ServletBinding(path = LtiAdvantageServlet.Path)
class LtiAdvantageServlet(val componentInstance: ComponentInstance)(implicit
  cs: ComponentService,
  authoringWebUtils: AuthoringApiWebUtils,
  ltiToolLaunchService: LtiToolLaunchService,
  ltiUtils: LtiWebUtils,
  workspaceService: WriteWorkspaceService,
  writeService: WriteService,
  contentAccessService: ContentAccessService,
  previewService: PreviewService,
  actualUser: UserDTO,
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import LtiAdvantageServlet.*
  import ServletDispatcher.*

  override protected def handler: RequestHandler = {
    case RequestMatcher(Method.GET, JwksPath, _, _)                                  =>
      ArgoResponse(ltiToolLaunchService.getJWKS()).right
    case RequestMatcher(Method.GET, AuthPath, request, _)                            =>
      lti1p3Auth(
        request.getParameter("client_id"),
        request.getParameter("redirect_uri"),
        request.getParameter("state"),
        request.getParameter("nonce"),
        request.getParameter("login_hint"),
        request.paramNZ("lti_message_hint")
      ).toDisjunction.leftMap(e =>
        logger.error(e)("Error authenticating LTI 1.3 request")
        ErrorResponse.serverError
      )
    case RequestMatcher(Method.POST, DeepCallbackPath(branchId, toolId), request, _) =>
      lti1p3DeepCallback(
        branchId,
        toolId,
        request.getParameter("JWT"),
        UUID.fromString(request.getParameter("parent")),
        OptionNZ(request.getParameter("after")).map(UUID.fromString),
        OptionNZ(request.getParameter("before")).map(UUID.fromString)
      ).toDisjunction.leftMap(e =>
        logger.error(e)("Error processing LTI 1.3 deep link callback")
        ErrorResponse.serverError
      )

    case _ => ErrorResponse(404).left
  }

  private def lti1p3Auth(
    clientId: String,
    redirectUri: String,
    state: String,
    nonce: String,
    loginHint: String,
    messageHint: Option[String],
  ): Try[HtmlResponse[RawHtml]] = messageHint match
    case None          =>
      lti1p3Launch(clientId, redirectUri, state, nonce, loginHint)
    case Some(msgHint) =>
      // Deep Link requests store additional data about where to place the content in a preconstructed returlUrl
      lti1p3DeepLink(clientId, redirectUri, state, nonce, loginHint, msgHint)

  // As mentioned in the comment for `LightweightLtiWebController.ltiLaunch`, this is the real LTI 1.3 launch
  private def lti1p3Launch(
    clientId: String,
    redirectUri: String,
    state: String,
    nonce: String,
    loginHint: String
  ): Try[HtmlResponse[RawHtml]] =
    // earlier, we passed a value like '10072529,685ca7d984...' to the Tool and now it returns it to us
    val loginHints      = loginHint.split(",")
    val contextIdStr    = loginHints(0)
    val resourceIdStr   = loginHints(1)
    val previewRole     = (loginHints.length > 1).option(PreviewRole.withName(loginHints(2)))
    val previewLtiRoles = previewRole.toList.flatMap(r => LtiRoles.ContextMappings1p3(r.roleId).list.toList)

    // Wonderful logics: authoring launches pass asset names, which have dashes, while edgepaths have no dashes
    val isAuthoring = resourceIdStr.contains("-")

    for
      // Load the context and LTI tool
      contextId <- contextIdStr.toLongOption <@~* new IllegalStateException("No context ID in login_hint")
      edgePath   = EdgePath.parse(resourceIdStr) // this will be garbage for authoring case, but that is ok

      previewUser <- previewRole.traverse(getPreviewUser(contextId, _))
      user         = previewUser | actualUser

      (context, lti) <- if isAuthoring then ltiUtils.loadContentForAuthoring(contextId, resourceIdStr)
                        else ltiUtils.loadContentForCourse(contextId, edgePath, user)
      (tool, config) <- ltiUtils.loadLtiTool(lti) <@~* new IllegalStateException(s"No tool for path $edgePath")

      // Validate Client ID matches
      _              <- config.clientId.contains(clientId) <@~* new IllegalStateException("Client ID does not match")
      // Validate that URL we are going to is either in redirection URLs, or is the tool's launch URL
      redirectionUrls = config.redirectionUrls.orZ.split("\n")
      validUrls       = redirectionUrls ++ config.url.toList
      _              <- validUrls.contains(redirectUri) <@~* new IllegalStateException(s"Invalid redirect_uri")

      // Assemble launch infos
      sourceDid     = isAuthoring.noption(GradeSourcedId(user.id, context.id, edgePath).encode)
      overrideRoles = if isAuthoring then lti1p3Roles("instructor") else previewLtiRoles
      launchInfo    = if isAuthoring then ltiUtils.defaultLaunchInfo()
                      else ltiUtils.getLaunchInfo(context.id, edgePath)
      resLink       = ResourceLink(resourceIdStr, lti.data.title)
      outcomeInfo   = ltiUtils.getOutcomeInfo(tool, config, sourceDid)

      // Put all the infos into the correct launch service
      params <-
        ltiToolLaunchService
          .get1p3LaunchParameters(
            context,
            user,
            tool,
            launchInfo,
            resLink,
            outcomeInfo,
            state,
            nonce,
            config,
            overrideRoles
          )
    yield ServletDispatcher.autopost(redirectUri, params.toMap)
    end for
  end lti1p3Launch

  private def getPreviewUser(contextId: Long, role: PreviewRole): Try[UserDTO] =
    for
      course    <- contentAccessService.getCourseAsInstructor(contextId, actualUser)
      previewer <- previewService.findPreviewer(course, role) `toTry` new IllegalStateException("No previewer")
    yield previewer.toDTO

  private def lti1p3DeepLink(
    clientId: String,
    redirectUri: String,
    state: String,
    nonce: String,
    loginHint: String,
    returnUrl: String,
  ): Try[HtmlResponse[RawHtml]] =
    // i.e. '10072529,10095302'
    val loginHints  = loginHint.split(",")
    val branchIdStr = loginHints(0)
    val toolIdStr   = loginHints(1) // deep link has no content item bound to a tool, so we just pass the tool ID

    for
      // Load the branch and LTI tool
      branchId <- branchIdStr.toLongOption <@~* new IllegalStateException("No branch ID in login_hint")
      toolId   <- toolIdStr.toLongOption <@~* new IllegalStateException("No tool ID in login_hint")
      ws        = authoringWebUtils.workspaceOrThrow404(branchId)
      context   = ltiUtils.ltiContextForAuthoring(ws)
      tool     <- toolId.component_![LtiToolComponent]
      config    = tool.getLtiConfiguration.defaultConfiguration

      // Validate Client ID matches
      _              <- config.clientId.contains(clientId) <@~* new IllegalStateException("Client ID does not match")
      // Validate that URL we are going to is either in redirection URLs, or is the tool's deep link URL
      redirectionUrls = config.redirectionUrls.orZ.split("\n")
      validUrls       = redirectionUrls ++ config.deepLinkUrl.toList
      _              <- validUrls.contains(redirectUri) <@~* new IllegalStateException(s"Invalid redirect_uri")

      // Assemble launch infos
      launchInfo = ltiUtils.defaultLaunchInfo().copy(returnUrl = Some(returnUrl))
      roles      = lti1p3Roles("instructor")

      // Put all the infos into the correct launch service
      params <- ltiToolLaunchService
                  .get1p3DeepLinkParameters(context, actualUser, tool, launchInfo, state, nonce, config, roles)
    yield ServletDispatcher.autopost(redirectUri, params.toMap)
    end for
  end lti1p3DeepLink

  private def lti1p3Roles(loRole: String) =
    LtiRoles.ContextMappings1p3(loRole).toList ++ LtiRoles.GlobalMappings1p3(loRole).toList

  private def lti1p3DeepCallback(
    branchIdStr: String,
    toolIdStr: String,
    jwtStr: String,
    parent: UUID,
    after: Option[UUID],
    before: Option[UUID]
  ): Try[RedirectResponse] =

    for
      // Load the branch, tool, workspace, etc
      branchId <- branchIdStr.toLongOption <@~* new Exception("Unable to parse Branch ID")
      toolId   <- toolIdStr.toLongOption <@~* new Exception("Unable to parse Tool ID")
      tool     <- toolId.component_![LtiToolComponent]
      config    = tool.getLtiConfiguration.defaultConfiguration
      _         = authoringWebUtils.branchOrFakeBranchOrThrow404(branchId) // validates branchId

      // Decode the JWT (with validation against the tool's public key)
      kid       <- parseHeader(jwtStr).map(_.kid) <@~* new Exception("Unable to parse key ID from JWT header")
      publicKey <- ltiToolLaunchService.getToolPublicKey(config, kid)
      jwtData   <- Jwt.decode(jwtStr, publicKey) // almost useless, but it does validate the signature
      jwtJson    = jwtData.toJson
      _          = jwtJson.parse.foreach(json => LogMeta.let("claim" -> json)(logger.info("JWT deep link claim")))
      jwt       <- jwtJson.decodeOption[DeepLinkClaims] <@~* new Exception("Unable to parse JWT claim")

      // Validation (TODO: more validation things)
      baseUrl = ltiUtils.defaultLaunchInfo().baseUrl
      _      <- jwt.audience.contains(baseUrl) <@~* new Exception(s"Audience ${jwt.audience} does not match")

      // Extract useful datas from JWT
      cItems   = jwt.contentItems
      ws       = workspaceService.requireWriteWorkspace(branchId, AccessRestriction.projectMember)
      ctxtPath = DcmPathUtils.shortestPath(ws, ws.homeName, parent).dropRight(1).optionNZ.map(_.mkString("."))
      success  = jwt.msg.filterNZ || cItems.nonEmpty.option(s"Added ${cItems.length} ${plural("item", cItems.length)}")

      // Build and commit write operations
      ops    <- buildOps(ws, tool, parent, after, before, cItems)
      result <- writeService.commit(ws, ops) // short-circuits no-op
    yield

      val redirect = s"/branch/$branchId/story/$parent" ?
        ctxtPath.strengthL("contextPath") &
        success.strengthL("success") &
        jwt.errMsg.filterNZ.strengthL("failure")
      RedirectResponse.temporary(s"/Authoring$redirect")

  // I have to do this. why do I have to do this? because the library does not parse the 'kid' field. the one I need.
  private def parseHeader(jwtStr: String): Option[JWTHeader] =
    jwtStr.split("\\.").headOption.flatMap(h => JwtBase64.decodeString(h).decodeOption[JWTHeader])

  private def buildOps(
    ws: WriteWorkspace,
    tool: LtiToolComponent,
    parent: UUID,
    after: Option[UUID],
    before: Option[UUID],
    contentItems: List[DeepLinkContentItem],
  ): Try[List[WriteOp]] =
    for
      _           <- ws.requireNodeId(parent)
      edges        = ws.outEdgeAttrs(parent, Group.Elements).toSeq.sortBy(_.position)
      afterIndex  <- after.traverse(findIndexOfTarget(edges, _))
      beforeIndex <- before.traverse(findIndexOfTarget(edges, _))
    yield
      val index    = afterIndex.cata(1 + _, beforeIndex | edges.length)
      val addNodes = contentItems.map(item =>
        AddNode(
          Lti(
            title = item.title.optionNZ | "Untitled",
            lti = AssetLtiToolConfiguration(
              toolId = tool.getToolId,
              name = tool.getName,
              toolConfiguration = LtiLaunchConfiguration(
                url = item.url,
                customParameters = item.custom.orZ
              )
            ),
            instructions = BlockPart(
              Seq(
                HtmlPart(
                  s"<p>${StringEscapeUtils.escapeHtml4(item.text.orZ)}</p>"
                )
              )
            )
          )
        )
      )
      val addEdges = addNodes.map(node =>
        AddEdge(
          sourceName = parent,
          targetName = node.name,
          group = Group.Elements,
        )
      )
      val setOrder = addEdges.nonEmpty.option(
        SetEdgeOrder(
          sourceName = parent,
          group = Group.Elements,
          ordering = edges.take(index).map(_.name) ++ addEdges.map(_.name) ++ edges.drop(index).map(_.name)
        )
      )
      setOrder ::? (addNodes ::: addEdges)

  private def findIndexOfTarget(edges: Seq[EdgeAttrs], name: UUID): Try[Int] =
    val index = edges.indexWhere(_.tgtName == name)
    (index >= 0) either index `orFailure` new NoSuchElementException(s"Target $name not present in edges")
end LtiAdvantageServlet

object LtiAdvantageServlet:
  private final val logger = org.log4s.getLogger

  private final val token = "/([^/\\?]+)"

  final val Path                     = "/lti/adv"
  private final val JwksPath         = s"$Path/jwks"
  private final val AuthPath         = s"$Path/auth"
  private final val DeepCallbackPath = s"$Path/deep$token$token.*".r

final case class JWTHeader(alg: String, typ: String, kid: String)

object JWTHeader:
  implicit val jwtHeaderCodec: CodecJson[JWTHeader] =
    casecodec3(JWTHeader.apply, ArgoExtras.unapply)("alg", "typ", "kid")

final case class DeepLinkPresentation(documentTarget: String)

object DeepLinkPresentation:
  implicit val deepLinkPresentationCodec: CodecJson[DeepLinkPresentation] =
    casecodec1(DeepLinkPresentation.apply, ArgoExtras.unapply1)("documentTarget")

final case class DeepLinkContentItem(
  contentType: String,
  title: String,
  text: Option[String],
  url: Option[String],
  presentation: Option[DeepLinkPresentation],
  custom: Option[Map[String, String]]
)

object DeepLinkContentItem:
  implicit val deepLinkContentItemCodec: CodecJson[DeepLinkContentItem] =
    casecodec6(DeepLinkContentItem.apply, ArgoExtras.unapply)(
      "type",
      "title",
      "text",
      "url",
      "presentation",
      "custom"
    )
end DeepLinkContentItem

// In theory we should be checking more claim details if the JWT library doesn't,
// and evaluating nonceness, but the restricted use of this makes it reasonably ok.
final case class DeepLinkClaims(
  // Standard JWT things
  expires: Long,
  issuedAt: Long,
  issuer: String,
  audience: List[String],
  nonce: String,

  // Deep Linking claims
  msg: Option[String],
  log: Option[String],
  errMsg: Option[String],
  errLog: Option[String],
  contentItems: List[DeepLinkContentItem]
)

object DeepLinkClaims:
  implicit val decodeDeepLinkClaims: DecodeJson[DeepLinkClaims] = DecodeJson(cursor =>
    for
      expires      <- cursor.downField("exp").as[Long]
      issuedAt     <- cursor.downField("iat").as[Long]
      issuer       <- cursor.downField("iss").as[String]
      audience     <- cursor.downField("aud").success match
                        case None        => DecodeResult.ok(List.empty[String])
                        case Some(valid) =>
                          if valid.focus.isArray then DecodeJson.of[List[String]].decode(valid)
                          else DecodeJson.of[String].decode(valid).map(List(_))
      nonce        <- cursor.downField("nonce").as[String]
      msg          <- cursor.downField(LTIConstants.DEEP_LINKING_MESSAGE_CLAIM).as[Option[String]]
      log          <- cursor.downField(LTIConstants.DEEP_LINKING_LOG_CLAIM).as[Option[String]]
      errMsg       <- cursor.downField(LTIConstants.DEEP_LINKING_ERROR_MESSAGE_CLAIM).as[Option[String]]
      errLog       <- cursor.downField(LTIConstants.DEEP_LINKING_ERROR_LOG_CLAIM).as[Option[String]]
      contentItems <- cursor.downField(LTIConstants.DEEP_LINKING_CONTENT_ITEMS_CLAIM).as[List[DeepLinkContentItem]]
    yield DeepLinkClaims(expires, issuedAt, issuer, audience, nonce, msg, log, errMsg, errLog, contentItems)
  )
end DeepLinkClaims
