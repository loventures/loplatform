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
import com.learningobjects.cpxp.scala.json.ArgoParseException
import com.learningobjects.cpxp.service.domain.DomainDTO
import loi.cp.storage.Storeable

@Service
class DomainStorageServiceImpl(domainStorageDao: DomainStorageDao) extends DomainStorageService:

  override def get[T: Storeable](domain: DomainDTO): T =
    val data = domainStorageDao.loadDomainStorage(domain.id).map(_.getStoragedData).getOrElse(Json.jEmptyObject)
    decodeField(data)

  override def modify[T: Storeable](user: DomainDTO)(f: T => T): T =
    val facade = domainStorageDao.loadDomainStorageForUpdate(user.id) match
      case Right(facade) => facade                          // why is there no valueOr for scala.Either how many years will that take
      case Left(err)     => throw new RuntimeException(err) // you shouldn't have messed up the db value

    val document     = facade.getStoragedData
    val original     = decodeField(document)
    val modified     = f(original)
    val nextDocument = encodeField(document, modified)
    facade.setStoragedData(nextDocument)
    modified
  end modify

  private def decodeField[T](document: Json)(implicit storeable: Storeable[T]): T =
    document.field(storeable.key) match
      case Some(json) =>
        storeable.codec
          .decodeJson(json)
          .fold(
            { case (msg, history) =>
              throw ArgoParseException(msg, history)
            },
            identity
          )
      case None       => storeable.empty

  private def encodeField[T](document: Json, field: T)(implicit storeable: Storeable[T]): Json =
    document.withObject(document => document.+(storeable.key, storeable.codec.encode(field)))
end DomainStorageServiceImpl
