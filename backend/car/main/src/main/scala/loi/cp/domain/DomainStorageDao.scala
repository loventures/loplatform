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

package loi.cp.domain

import argonaut.Json
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import scaloi.{Created, Gotten}

@Service
class DomainStorageDao(implicit fs: FacadeService):

  def loadDomainStorage(domainId: Long): Option[DomainStorageFacade] =
    domainId.facade[DomainStorageParentFacade].getDomainStorage

  def loadDomainStorageForUpdate(domainId: Long): Either[String, DomainStorageFacade] =
    for
      domain  <- domainId.facade_?[DomainStorageParentFacade].toRight(s"no such domain $domainId")
      storage <- getOrCreateForUpdate(domain)
    yield storage

  private def getOrCreateForUpdate(domain: DomainStorageParentFacade): Either[String, DomainStorageFacade] =
    domain.getOrCreateDomainStorage match
      case Gotten(domainStorage)  =>
        Either.cond(
          domainStorage.refresh(true),
          domainStorage,
          s"could not obtain lock on domain storage ${domainStorage.getId}"
        )
      case Created(domainStorage) =>
        domainStorage.setStoragedData(Json.jEmptyObject)
        Right(domainStorage)
end DomainStorageDao
