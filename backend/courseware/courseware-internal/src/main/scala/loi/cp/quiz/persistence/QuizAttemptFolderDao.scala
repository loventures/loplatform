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

package loi.cp.quiz.persistence

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.quiz.QuizAttemptFolderEntity
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.{PersistenceIdFactory, ThreadTerminator}
import org.hibernate.{LockMode, Session}
import scaloi.GetOrCreate

import scala.jdk.CollectionConverters.*

/** A service to get quiz attempt folders for a given user.
  */
@Service
trait QuizAttemptFolderDao:
  def getOrCreateAttemptFolder(user: UserDTO): QuizAttemptFolderEntity

  def getAttemptFolder(user: UserDTO): Option[QuizAttemptFolderEntity]

@Service
class QuizAttemptFolderDaoImpl(session: => Session, domainDto: => DomainDTO, idFactory: PersistenceIdFactory)
    extends QuizAttemptFolderDao:

  override def getOrCreateAttemptFolder(user: UserDTO): QuizAttemptFolderEntity =
    ThreadTerminator.check()

    GetOrCreate(
      queryEntity = () => getAttemptFolder(user),
      createEntity = () => createAttemptFolder(user),
      lockCollection = () => lockUser(user)
    ).result

  private def lockUser(user: UserDTO): Unit =
    val userEntity: UserFinder = session.getReference(classOf[UserFinder], user.id)
    session.lock(userEntity, LockMode.PESSIMISTIC_WRITE)

  override def getAttemptFolder(user: UserDTO): Option[QuizAttemptFolderEntity] =
    ThreadTerminator.check()
    val folderQueryString: String =
      s"FROM ${classOf[QuizAttemptFolderEntity].getName} WHERE user.id = ${user.id}"

    val folders: Seq[QuizAttemptFolderEntity] =
      session.createQuery(folderQueryString, classOf[QuizAttemptFolderEntity]).getResultList.asScala.toSeq

    if folders.size > 1 then
      throw new IllegalStateException(s"User ${user.id} has multiple attempt folders: ${folders.size}")
    else folders.headOption
  end getAttemptFolder

  private def createAttemptFolder(user: UserDTO): QuizAttemptFolderEntity =
    val domainItem: Item       = session.getReference(classOf[Item], domainDto.id)
    val userEntity: UserFinder = session.getReference(classOf[UserFinder], user.id)

    val newId: Long = idFactory.generateId()

    val entity = new QuizAttemptFolderEntity
    entity.setId(newId)
    entity.setRoot(domainItem)
    entity.user = userEntity
    session.persist(entity)

    entity
  end createAttemptFolder
end QuizAttemptFolderDaoImpl
