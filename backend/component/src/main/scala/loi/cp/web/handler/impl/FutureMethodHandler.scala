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

package loi.cp.web.handler.impl

import java.util
import jakarta.servlet.AsyncContext
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.pekko.actor.ActorSystem
import com.learningobjects.cpxp.BaseWebContext
import com.learningobjects.cpxp.component.web.converter.{ConvertOptions, HttpMessageConverter}
import com.learningobjects.cpxp.component.web.{
  DePathSegment,
  ErrorResponse,
  HttpResponse,
  NoContentResponse,
  WebRequest,
  WebResponse,
  WebResponseOps
}
import com.learningobjects.cpxp.component.{
  ComponentEnvironment,
  ComponentInstance,
  ComponentSupport,
  RequestMappingInstance
}
import de.tomcat.juli.LogMeta
import scaloi.syntax.AnyOps.*
import loi.cp.web.handler.SequencedMethodResult
import loi.cp.web.{ExceptionResolver, HttpResponseEntity}

import scala.jdk.CollectionConverters.*
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.Try
import scalaz.{-\/, \/-}
import scalaz.syntax.std.`try`.*

/** Method handler for an SRS method that returns a Future[A] or a WebResponse \/ Future[A].
  *
  * @param component
  *   the owning component
  * @param function
  *   the request mapping function
  * @param effectiveSegments
  *   the effective segments
  * @param pathSegments
  *   the path segments
  * @param sequenceContext
  *   the sequence context
  * @param actorSystem
  *   the actor system
  */
class FutureMethodHandler(
  component: ComponentInstance,
  function: RequestMappingInstance,
  effectiveSegments: util.List[DePathSegment],
  pathSegments: util.List[DePathSegment],
  sequenceContext: SequenceContext,
  actorSystem: ActorSystem,
  env: ComponentEnvironment
) extends AbstractRequestMappingMethodHandler(
      component,
      function,
      effectiveSegments,
      pathSegments,
      sequenceContext,
      actorSystem
    ):

  import FutureMethodHandler.*

  import scala.concurrent.ExecutionContext.Implicits.global

  override def handleForValue(request: WebRequest, response: HttpServletResponse): AsyncContext =
    val logMeta = LogMeta.capture
    function.preAsync(request, response, effectiveSegments)
    // I used to response.reset() to clear previously-set headers but it seems not necessary
    request.getRawRequest.startAsync <| { context =>
      val listener = new FutureListener(env, request, logMeta)
      context.addListener(listener)
      context.setTimeout(AsyncTimeout.toMillis)
      val future   = function.asyncInvoke(context) match // sequence a disjunction of a future
        case f: Future[?]        => f
        case \/-(f: Future[?])   => f
        case -\/(l: WebResponse) => Future.successful(l)
        case e: ErrorResponse    => Future.successful(e) // SRS unwraps lefty ErrorResponses for our own good
      // TODO: which execution context?
      future onComplete { attempt =>
        logMeta {
          function.asyncCleanup()
          if listener.isTimedOut then
            logger.warn(s"Request timed out, dropping future response: ${request.getRawRequest.getRequestURI}")
          else writeResponse(attempt)(env, context, request)
        }
      }
    } // return the context for the future method result
  end handleForValue

  override def handleForResult(
    request: WebRequest,
    response: HttpServletResponse,
    handledValue: AnyRef,
    nextSequenceContext: SequenceContext
  ): SequencedMethodResult =
    new FutureMethodResult(nextSequenceContext)
end FutureMethodHandler

object FutureMethodHandler:
  private val logger = org.log4s.getLogger

  /** If the future doesn't return in 45 seconds then fail out. Consider ELB 60 second timeout. */
  private val AsyncTimeout = 45.seconds

  /** Convert and write an asynchronous response in a given context. This largely follows the pattern of
    * ApiDispatcherServlet.
    */
  def writeResponse[A](
    responseTry: Try[A]
  )(env: ComponentEnvironment, context: AsyncContext, webRequest: WebRequest): Unit =
    val httpRequest     = context.getRequest.asInstanceOf[HttpServletRequest]
    val httpResponse    = context.getResponse.asInstanceOf[HttpServletResponse]
    // At this point we just need a component environment for service lookup. Unfortunately
    // inner DI relies on the BaseWebContext component environment so we have to set it up.
    BaseWebContext.getContext.setComponentEnvironment(env)
    // First wrap the result as a http response
    val responseWrapper = responseTry.cata(toHttpResponse, resolveException)
    // Then unwrap the underlying entity
    val valueOpt        = Option(unwrapResponse(responseWrapper))
    // Set a status and write the response
    if responseWrapper.statusCode > 0 then
      valueOpt.getOrElse(ErrorResponse.notFound) match
        case \/-(value) =>
          write(value, webRequest, httpRequest, httpResponse, responseWrapper.statusCode)
        case -\/(value) =>
          write(value, webRequest, httpRequest, httpResponse, HttpServletResponse.SC_ACCEPTED)
        case value      =>
          write(value, webRequest, httpRequest, httpResponse, responseWrapper.statusCode)
      // Complete the async response
      context.complete()
    end if
  end writeResponse

  private def write(
    a: Any,
    webRequest: WebRequest,
    httpRequest: HttpServletRequest,
    httpResponse: HttpServletResponse,
    sc: Int
  ): Unit = a match
    case webResponse: WebResponse =>
      WebResponseOps.send(webResponse, httpRequest, httpResponse)

    case () =>
      WebResponseOps.send(NoContentResponse, httpRequest, httpResponse)

    case value =>
      // Lookup a suitable message converter
      val converters = for
        mediaType <- webRequest.getAcceptedMediaTypes.asScala
        converter <- ComponentSupport
                       .getComponents(classOf[HttpMessageConverter[Any]])
                       .asScala
        if converter.canWrite(value, mediaType)
      yield (mediaType, converter)
      httpResponse.setStatus(sc)
      converters.headOption foreach { case (mediaType, converter) =>
        converter.write(
          value,
          new ConvertOptions(mediaType, util.Optional.empty()),
          httpRequest,
          httpResponse
        )
      }

  /** Return a HttpResponse unchanged, wrap anything else as a success. */
  private def toHttpResponse(a: Any): HttpResponse = a match
    case r: HttpResponse => r
    case o               => HttpResponseEntity.okay(o)

  /** Transform any exception to a response using the available exception resolver. */
  private def resolveException(t: Throwable): HttpResponse =
    logger.warn(t)("Future response failed")
    ComponentSupport
      .lookupService(classOf[ExceptionResolver])
      .resolveException(t)

  /** Unwrap any wrapped response entity. */
  private def unwrapResponse(h: HttpResponse) = h match
    case e: HttpResponseEntity[?] => e.get
    case o                        => o
end FutureMethodHandler
