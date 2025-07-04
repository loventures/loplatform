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

package loi.cp.course

import com.learningobjects.cpxp.component.ComponentEnvironment
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.util.HttpUtils
import com.learningobjects.cpxp.{BaseServiceMeta, BaseWebContext}
import com.typesafe.config as typesafe
import loi.apm.Apm
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*

/** Sundry things which the course frontend wants interpolatable.
  *
  * Most of these aren't very justifiable.
  */
trait CourseVariables:
  this: CourseComponent =>

  protected def config: typesafe.Config
  protected def environment: ComponentEnvironment

  // I could require all of these but it's a pain
  import BaseServiceMeta.getServiceMeta as serviceMeta
  import BaseWebContext.getContext as webContext
  import HttpUtils.*

  final def current        = Current.getInstance
  final def instance       = this
  final def ipAddress      = getRemoteAddr(webContext.getRequest, serviceMeta)
  final def trackingHeader = apmEnabled ?? Apm.getBrowserTimingHeader
  final def trackingFooter = ""

  final def componentConfiguration =
    environment.getJsonConfiguration(getComponentInstance.getIdentifier)

  private def apmEnabled = config.getBoolean("apm.enabled")
end CourseVariables
