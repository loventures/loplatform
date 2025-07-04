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

package loi.cp.startup

import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.folder.FolderFacade
import loi.cp.admin.FolderParentFacade
import scaloi.GetOrCreate

/** Startup mixin for ensuring folders exist
  */
trait FolderEnsurers:
  import FolderEnsurers.*

  /** Ensure that a folder with a particular type exists in this domain.
    *
    * @param folderType
    *   the folder type (stored as the "type" attribute)
    * @param domain
    *   the domain
    * @param fs
    *   the facade service
    * @return
    *   the folder
    */
  protected def ensureFolderByType(
    folderType: String
  )(implicit domain: DomainDTO, fs: FacadeService): GetOrCreate[FolderFacade] =
    domain
      .facade[FolderParentFacade]
      .getOrCreateFolderByType(folderType) init { folder =>
      logger info s"Creating domain folder: $folderType"
    }

  /** Ensure that a folder with a particular type and identifier exists in this domain.
    *
    * @param folderType
    *   the folder type (stored as the "type" attribute)
    * @param folderId
    *   the folder identifier (stored as the "id" attribute)
    * @param domain
    *   the domain
    * @param fs
    *   the facade service
    * @return
    *   the folder
    */
  protected def ensureFolderByTypeAndId(folderType: String, folderId: String)(implicit
    domain: DomainDTO,
    fs: FacadeService
  ): GetOrCreate[FolderFacade] =
    ensureFolderByType(folderType) init { _.setIdStr(folderId) }

  /** Ensure that a folder with a particular id exists in this domain.
    *
    * @param folderId
    *   the folder identifier (stored as the "id" attribute)
    * @param domain
    *   the domain
    * @param fs
    *   the facade service
    * @return
    *   the folder
    */
  protected def ensureFolderById(
    folderId: String
  )(implicit domain: DomainDTO, fs: FacadeService): GetOrCreate[FolderFacade] =
    domain
      .facade[FolderParentFacade]
      .getOrCreateFolderByIdString(folderId) init { folder =>
      logger info s"Creating domain folder with id: $folderId"
    }
end FolderEnsurers

object FolderEnsurers:

  /** The logger. */
  final val logger = org.log4s.getLogger
