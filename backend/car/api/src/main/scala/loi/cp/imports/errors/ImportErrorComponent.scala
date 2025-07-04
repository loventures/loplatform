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

package loi.cp.imports.errors

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.{ItemMapping, Schema}
import com.learningobjects.cpxp.service.component.misc.BatchConstants
import com.learningobjects.de.web.Queryable

import java.lang.Long as JLong

@ItemMapping(value = BatchConstants.ITEM_TYPE_BATCH_ERROR, singleton = true)
@Schema("batchError")
trait ImportErrorComponent extends ComponentInterface with Id:

  @JsonProperty
  def getReason: String
  def setReason(reason: String): Unit

  @JsonProperty
  def getError: GenericError
  def setError(error: GenericError): Unit

  @JsonProperty
  @Queryable(dataType = BatchConstants.DATA_TYPE_BATCH_ERROR_INDEX)
  def getIndex: JLong
  def setIndex(index: JLong): Unit

  @JsonProperty
  def getMessages: Seq[String]
end ImportErrorComponent
