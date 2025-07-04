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

package com.learningobjects.cpxp.component

import argonaut.*
import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.web.Method
import de.tomcat.juli.LogMeta

import scala.jdk.CollectionConverters.*

// because its too hard to use LogMeta from Java
object SrsLog:

  private val log = org.log4s.getLogger

  def logUnexpectedQueryParams(params0: java.util.Map[String, Array[String]]): Unit =
    val params = params0.asScala.toMap.view.mapValues(_.toList).toMap.asJson
    LogMeta.let("params" -> params)(log.info("unexpected query parameters"))

  // using the transaction name to represent the route
  def logRoute(transactionName: String, method: Method): Unit =
    LogMeta.let(
      "route"  -> transactionName.asJson,
      "method" -> method.name().asJson,
    )(log.info("route complete"))
end SrsLog
