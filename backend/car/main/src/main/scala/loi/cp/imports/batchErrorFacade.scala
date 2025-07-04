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

import com.learningobjects.cpxp.dto.{Facade, FacadeComponent, FacadeData, FacadeItem}
import com.learningobjects.cpxp.service.component.misc.BatchConstants
import com.learningobjects.cpxp.service.query.QueryBuilder
import loi.cp.imports.errors.{GenericError, ImportErrorComponent}

import java.lang.Long as JLong

@FacadeItem(BatchConstants.ITEM_TYPE_BATCH_ERROR)
trait ImportErrorFacade extends Facade:

  @FacadeData
  def getReason: String
  def setReason(reason: String): Unit

  @FacadeData
  def getError: GenericError
  def setError(error: GenericError): Unit

  @FacadeData
  def getIndex: JLong
  def setIndex(index: JLong): Unit
end ImportErrorFacade

@FacadeItem(BatchConstants.ITEM_TYPE_BATCH)
trait ImportErrorParentFacade extends Facade:
  @FacadeComponent
  def addBatchError[T <: ImportErrorComponent](batchError: T): T
  def addBatchError(): ImportErrorComponent
  def getBatchError(id: Long): Option[ImportErrorComponent]
  def getBatchErrors: List[ImportErrorComponent]
  def queryBatchErrors: QueryBuilder
