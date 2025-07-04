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

package loi.authoring.exchange.exprt.store

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.de.task.TaskReport
import loi.authoring.blob.BlobRef
import loi.authoring.exchange.exprt.ExportReceipt
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus
import org.hibernate.Session

import scala.jdk.CollectionConverters.*

@Service
class ExportReceiptDao(
  session: => Session,
  domainDTO: => DomainDTO
):

  def loadReceipt(id: Long): Option[ExportReceiptEntity] =
    Option(session.find(classOf[ExportReceiptEntity], id))

  def loadReceipts(limit: Int, offset: Int): Seq[ExportReceiptEntity] =
    session
      .createQuery(
        """
        | FROM ExportReceiptEntity
        | WHERE root = :root
        | ORDER BY createTime DESC
      """.stripMargin,
        classOf[ExportReceiptEntity]
      )
      .setParameter("root", domainDTO.id)
      .setFirstResult(offset)
      .setMaxResults(limit)
      .getResultList
      .asScala
      .toSeq

  def save(entity: ExportReceiptEntity): ExportReceiptEntity =
    session.persist(entity)
    session.flush()
    session.clear()
    entity

  // for when `entity` has a real id but is not attached to the session... updates the
  // session with the data on `entity`.
  def update(entity: ExportReceiptEntity): Unit =
    session.merge(entity)

  def loadReference(receipt: ExportReceipt): ExportReceiptEntity =
    session.getReference(classOf[ExportReceiptEntity], receipt.id)

  def delete(entity: ExportReceiptEntity): Unit =
    session.remove(entity)
end ExportReceiptDao

object ExportReceiptDao:

  private val om = JacksonUtils.getFinatraMapper

  def entityToReceipt(entity: ExportReceiptEntity) =
    new ExportReceipt(
      entity.getId,
      entity.getData,
      Option(entity.getSource).map(n => om.readValue(om.treeAsTokens(n), classOf[BlobRef])),
      om.readValue(om.treeAsTokens(entity.getReport), classOf[TaskReport]),
      Option(entity.getAttachmentId).map(Long2long),
      AssetExchangeRequestStatus.withName(entity.getStatus),
      entity.getCreateTime,
      Option(entity.getStartTime),
      Option(entity.getEndTime),
      Option(entity.getCreatedBy).map(Long2long),
      entity.getRoot
    )

  def receiptToEntity(receipt: ExportReceipt) =
    new ExportReceiptEntity(
      receipt.id,
      receipt.data,
      om.valueToTree(receipt.source),
      om.valueToTree(receipt.report),
      receipt.attachmentId.map(long2Long).orNull,
      receipt.status.entryName,
      receipt.createTime,
      receipt.startTime.orNull,
      receipt.endTime.orNull,
      receipt.createdBy.map(long2Long).orNull,
      receipt.domainId
    )
end ExportReceiptDao
