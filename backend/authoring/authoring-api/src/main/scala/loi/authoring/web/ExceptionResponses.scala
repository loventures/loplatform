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

package loi.authoring.web

import com.learningobjects.cpxp.component.web.ErrorResponse
import loi.authoring.asset.service.exception.NoSuchAssetException
import loi.authoring.project.exception.{NoSuchBranchException, NoSuchBranchOnProjectException, NoSuchProjectIdException}
import loi.authoring.workspace.exception.NoSuchNodeInWorkspaceException

object ExceptionResponses:
  private final val logger = org.log4s.getLogger

  def exceptionResponse: Throwable => ErrorResponse = {
    case nf @ (_: NoSuchProjectIdException | _: NoSuchBranchException | _: NoSuchBranchOnProjectException |
        _: NoSuchAssetException | _: NoSuchNodeInWorkspaceException) =>
      ErrorResponse.notFound(nf)
    case th =>
      logger.warn(th)("Internal error")
      ErrorResponse.serverError(th)
  }
end ExceptionResponses
