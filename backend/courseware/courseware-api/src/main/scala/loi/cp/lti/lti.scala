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

package loi.cp

import java.util.Properties

import _root_.loi.cp.integration.BasicLtiSystemComponent
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import jakarta.servlet.http.HttpServletRequest
import scalaz.\/
import scalaz.std.option.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.misc.Stringomorphism
import scaloi.syntax.AnyOps.*
import scaloi.syntax.CollectionOps.*
import scaloi.syntax.TryOps.*

package object lti:

  /** Searches the LTI connector or web request for an optional parameter and parses the result into a target type.
    * @param names
    *   the possible parameter names
    * @param request
    *   the web request
    * @param system
    *   the LTI connector
    * @tparam T
    *   the target type
    * @return
    *   an error or the parsed parameter, if present
    */
  def ltiParamT[T: Stringomorphism](
    names: String*
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ Option[T] =
    systemParam[T](names).orElse(requestParam[T](names map paramName)).sequence

  /** Searches the LTI connector or web request for a required parameter and parses the result into a target type.
    * @param name
    *   the parameter name
    * @param request
    *   the web request
    * @param system
    *   the LTI connector
    * @tparam T
    *   the target type
    * @return
    *   an error or the parsed parameter
    */
  def ltiParamT_![T: Stringomorphism](
    name: String
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ T =
    ltiParamT[T](name) flatMap { paramOpt =>
      paramOpt \/> MissingLtiParameter(paramName(name))
    }

  /** Searches the LTI connector or web request for an optional parameter.
    * @param names
    *   the possible parameter names
    * @param request
    *   the web request
    * @param system
    *   the LTI connector
    * @return
    *   an error or the parameter, if present
    */
  def ltiParam(
    name: String
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ Option[String] =
    ltiParamT[String](name)

  /** Searches the LTI connector or web request for a mandatory parameter.
    * @param name
    *   the parameter name
    * @param request
    *   the web request
    * @param system
    *   the LTI connector
    * @return
    *   an error or the parameter
    */
  def ltiParam_!(
    name: String
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ String =
    ltiParam(name) flatMap { paramOpt =>
      paramOpt \/> MissingLtiParameter(paramName(name))
    }

  /** Return either the overridden parameter name from the LTI connector or the provided name. */
  private def paramName(name: String)(implicit system: BasicLtiSystemComponent): String =
    system.getBasicLtiConfiguration.parameterNames.flatMap(_.get(name)).getOrElse(name)

  /** Searches the LTI connector for a preconfigured parameter. */
  private def systemParam[T: Stringomorphism](
    names: Seq[String]
  )(implicit system: BasicLtiSystemComponent): Option[LtiError \/ T] =
    system.getBasicLtiConfiguration.parameters flatMap { params =>
      parseParam(names)(params.get)
    }

  /** Searches the http request for a matching parameter. */
  private def requestParam[T: Stringomorphism](names: Seq[String])(implicit
    request: HttpServletRequest
  ): Option[LtiError \/ T] =
    parseParam(names)(request.paramNZ)

  /** Parses the first defined parameter from a given list of names into a target type or error.
    *
    * @param names
    *   the parameter names for which to search
    * @param param
    *   a function that returns a given parameter value
    * @tparam T
    *   the target type
    * @return
    *   the first defined parameter parsed into the target type or an error
    */
  private def parseParam[T: Stringomorphism](
    names: Seq[String]
  )(param: String => Option[String]): Option[LtiError \/ T] =
    names findMap { name =>
      param(name) map { value =>
        Stringomorphism[T].apply(value) \/>| InvalidLtiParameter(name, value)
      }
    }

  /** Matcher for either a GET or a POST. */
  object GetOrPost:
    def unapply(m: Method): Boolean = (m == Method.GET) || (m == Method.POST)

  /** Utility to build a [[Properties]]. */
  implicit class StrStrListOps(val self: List[(String, String)]) extends AnyVal:
    def toProperties: Properties =
      new Properties <| { properties =>
        self foreach (properties.setProperty).tupled
      }
end lti
