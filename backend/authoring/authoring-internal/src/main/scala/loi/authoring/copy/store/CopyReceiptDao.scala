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

package loi.authoring.copy.store

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.de.task.TaskReport
import loi.authoring.copy.{CopyReceipt, CopyReceiptStatus}
import org.hibernate.Session

@Service
class CopyReceiptDao(
  session: => Session
):

  def loadReceipt(id: Long): Option[CopyReceiptEntity] =
    Option(session.find(classOf[CopyReceiptEntity], id))

  def loadReference(receipt: CopyReceipt): CopyReceiptEntity =
    session.getReference(classOf[CopyReceiptEntity], receipt.id)

  def deleteReceipt(entity: CopyReceiptEntity): Unit =
    session.remove(entity)

  def save(entity: CopyReceiptEntity): CopyReceiptEntity =
    session.persist(entity)
    entity

  def update(receipt: CopyReceipt): Unit =
    session.merge(receiptToEntity(receipt))

  def receiptToEntity(receipt: CopyReceipt) =
    new CopyReceiptEntity(
      receipt.id,
      receipt.originalId,
      receipt.copyId.map(long2Long).orNull,
      JacksonUtils.getFinatraMapper.valueToTree(receipt.report),
      receipt.status.name(),
      receipt.createTime,
      receipt.startTime.orNull,
      receipt.endTime.orNull,
      receipt.createdBy.map(long2Long).orNull,
      receipt.domainId
    )
end CopyReceiptDao

object CopyReceiptDao:

  def entityToReceipt(entity: CopyReceiptEntity) =
    CopyReceipt(
      entity.getId,
      entity.getOriginal,
      Option(entity.getCopy),
      JacksonUtils.getFinatraMapper.treeToValue(entity.getReport, classOf[TaskReport]),
      CopyReceiptStatus.valueOf(entity.getStatus),
      entity.getCreateTime,
      Option(entity.getStartTime),
      Option(entity.getEndTime),
      Option(entity.getCreatedBy).map(_.toLong),
      entity.getRoot
    )
end CopyReceiptDao
