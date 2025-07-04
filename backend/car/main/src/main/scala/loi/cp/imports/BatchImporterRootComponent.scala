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

package loi.cp.imports

import argonaut.Json
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, HttpResponse, Method}
import com.learningobjects.cpxp.service.query.QueryBuilder
import com.learningobjects.de.authorization.Secured
import jakarta.servlet.http.HttpServletRequest
import loi.cp.admin.right.AdminRight
import loi.cp.imports.{ImportComponent, ImportItem}

import java.lang.Long as JLong

@Controller(value = "batch", root = true)
@RequestMapping(path = "integration")
@Secured(Array(classOf[AdminRight]))
trait BatchImporterRootComponent extends ApiRootComponent:

  @RequestMapping(path = "batches/{batchId}", method = Method.GET)
  def getBatch(@PathVariable("batchId") batchId: JLong): Option[ImportComponent]

  @RequestMapping(path = "batches", method = Method.GET)
  def getBatches(q: ApiQuery): ApiQueryResults[? <: ImportComponent]

  @RequestMapping(path = "batches", method = Method.POST)
  def submitBatch(
    @RequestBody batch: BatchRest,
    request: HttpServletRequest
  ): ImportComponent

  @RequestMapping(path = "execute", method = Method.POST)
  def submitSingle(@RequestBody batchItem: ArgoBody[ImportItem], request: HttpServletRequest): HttpResponse

  def queryImports: QueryBuilder
end BatchImporterRootComponent

case class BatchRest(
  identifier: String,
  @JsonDeserialize(contentAs = classOf[String]) callbackUrl: Option[String],
  batch: Seq[Json] // i.e. Seq[ImportItem]
)
