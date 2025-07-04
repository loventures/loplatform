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

package com.learningobjects.cpxp.component.web

import com.learningobjects.cpxp.component.util.RawHtml
import com.learningobjects.cpxp.util.EntityContext
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import scalaz.\/
import scalaz.syntax.either.*
import scaloi.syntax.DisjunctionOps.*

/** Default implementation of servlet component that delegates to pattern-matching request handlers. The current
  * transaction is flagged for rollback if the result is a left.
  */
trait ServletDispatcher:
  self: ServletComponent =>
  import ServletDispatcher.*

  /** Service a HTTP request. */
  override def service(request: HttpServletRequest, response: HttpServletResponse): WebResponse =
    checkAccess
      .flatMap(_ => handle(request, response))
      .leftTap(_ => EntityContext.markTransactionFailed())
      .merge

  /** Check access. */
  protected def checkAccess: WebResponse \/ Unit =
    \/.attempt {
      self.getComponentInstance.getComponent.getDelegate.checkAccess(self.getComponentInstance)
    } { ex =>
      logger.warn(ex)("Access denied by enforcers")
      ErrorResponse.unauthorized
    }

  // pedantically, notFound is not a universally-valid response

  /** Handle a HTTP request.
    * @param request
    *   the request object
    * @param response
    *   the response object
    * @return
    *   either a successful or a failure result
    */
  private def handle(request: HttpServletRequest, response: HttpServletResponse): EitherResponse =
    handler.lift(request -> response).getOrElse(ErrorResponse.notFound.left)

  /** Return the partial function defining the supported requests.
    * @return
    *   the request handler
    */
  protected def handler: RequestHandler

  import scala.language.implicitConversions

  /** Implicitly widen narrow responses. */
  protected implicit def widenBoth[A <: WebResponse, B <: WebResponse](aOrB: A \/ B): EitherResponse =
    aOrB.bimap(_.widen, _.widen)
  protected implicit def widenRight[B <: WebResponse](aOrB: Nothing \/ B): EitherResponse            =
    aOrB.bimap(identity, _.widen)
  protected implicit def widenLeft[A <: WebResponse](aOrB: A \/ Nothing): EitherResponse             = aOrB.bimap(_.widen, identity)
end ServletDispatcher

/** Servlet dispatcher companion.
  */
object ServletDispatcher:
  private final val logger = org.log4s.getLogger

  /** Either a failed or successful web response. */
  type EitherResponse = WebResponse \/ WebResponse

  /** A partial function from a request/response. pair to either a failed or successful web response. */
  type RequestHandler = PartialFunction[(HttpServletRequest, HttpServletResponse), EitherResponse]

  /** Helper for matching web requests. */
  object RequestMatcher:

    /** Matches a servlet request against a request method and request URI. */
    def unapply(request: HttpServletRequest): Option[(Method, String)] =
      Some(Method.valueOf(request.getMethod) -> request.getRequestURI)

    /** Matches a servlet request/response pair against a request method and request URI. */
    def unapply(
      http: (HttpServletRequest, HttpServletResponse)
    ): Option[(Method, String, HttpServletRequest, HttpServletResponse)] =
      Some((Method.valueOf(http._1.getMethod), http._1.getRequestURI, http._1, http._2))
  end RequestMatcher

  /** Generate an HTML redirect. Useful when there are cookies in the response that need to stick; Safari sometimes does
    * not stick them on an HTTP redirect response.
    * @param url
    *   the target URL
    * @return
    *   the form response
    */
  def htmlRedirect(url: String): HtmlResponse[RawHtml] =
    HtmlResponse(
      <html>
        <head>
          <meta http-equiv="refresh" content={s"0; url=$url"} />
        </head>
        <body style="background-color: #444">
          <div style="position: absolute; left: 50%; top: 50%; transform: translate(-50%,-50%); font-family: sans-serif; color: #eeb;">redirecting...</div>
        </body>
      </html>
    )

  /** Generate an auto-posting form.
    * @param url
    *   the target URL
    * @param params
    *   any request parameters
    * @return
    *   the form response
    */
  def autopost(url: String, params: Map[String, String] = Map.empty): HtmlResponse[RawHtml] =
    HtmlResponse(
      <html>
        <body onload="document.forms[0].action += document.location.hash; document.forms[0].submit()" style="background-color: #444">
          <div style="position: absolute; left: 50%; top: 50%; transform: translate(-50%,-50%); font-family: sans-serif; color: #eeb;">redirecting...</div>
          <form method="POST" action={url}>
            {params map { case (k, v) => <input type="hidden" name={k} value={v} /> }}
          </form>
        </body>
      </html>
    )
end ServletDispatcher
